package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.CCTFileChooser;
import net.gnehzr.cct.misc.JSpinnerWithText;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ScrambleExportDialog extends JDialog {

	private static final Logger LOG = LogManager.getLogger(ScrambleExportDialog.class);

	private final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	private JTextField urlField;
	private ScrambleChooserComboBox<?> scrambleChooser;
	private JSpinnerWithText scrambleLength, numberOfScrambles;

	public ScrambleExportDialog(JFrame owner, ScrambleVariation selected, ScramblePluginManager scramblePluginManager,
								Configuration configuration, Profile profile, ProfileDao profileDao) {
		super(owner, StringAccessor.getString("ScrambleExportDialog.exportscrambles"), true);
		this.scramblePluginManager = scramblePluginManager;
		this.configuration = configuration;
		urlField = new JTextField(40);
		urlField.setToolTipText(StringAccessor.getString("ScrambleExportDialog.choosefile"));
		JButton browseButton = new JButton(StringAccessor.getString("ScrambleExportDialog.browse"));
		browseButton.addActionListener(e -> {
			CCTFileChooser fc = new CCTFileChooser(configuration);
			if (fc.showDialog(ScrambleExportDialog.this, StringAccessor.getString("ScrambleExportDialog.save")) == CCTFileChooser.APPROVE_OPTION) {
				File selectedFile = fc.getSelectedFile();
				urlField.setText(selectedFile.toURI().toString());
			}
		});

		scrambleChooser = new ScrambleVariationChooserComboBox(false, this.scramblePluginManager, this.configuration);
		scrambleChooser.setSelectedItem(selected);
		scrambleChooser.addActionListener(e -> {
            if(scrambleLength != null) {
                ScrambleVariation curr = (ScrambleVariation) scrambleChooser.getSelectedItem();
                scrambleLength.setValue(curr.getLength());
                numberOfScrambles.setValue(scramblePluginManager.getCustomizationFromVariation(curr, profile).getRASize(0));
            }
        });

		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));

		JPanel sideBySide = new JPanel();
		sideBySide.setLayout(new BoxLayout(sideBySide, BoxLayout.X_AXIS));
		sideBySide.add(urlField);
		sideBySide.add(browseButton);
		
		subPanel.add(sideBySide);
		subPanel.add(scrambleChooser);

		scrambleLength = new JSpinnerWithText(selected.getLength(), 1, StringAccessor.getString("ScrambleExportDialog.lengthscrambles")); 
		numberOfScrambles = new JSpinnerWithText(scramblePluginManager.getCustomizationFromVariation(selected, profile).getRASize(0), 1, StringAccessor.getString("ScrambleExportDialog.numberscrambles"));
		subPanel.add(scrambleLength);
		subPanel.add(numberOfScrambles);

		JButton exportButton = new JButton(StringAccessor.getString("ScrambleExportDialog.export"));
		exportButton.addActionListener(e -> {
			URL file;
			try {
				file = new URI(urlField.getText()).toURL();
			} catch (Exception e1) {
				Utils.showErrorDialog(ScrambleExportDialog.this, e1, StringAccessor.getString("ScrambleExportDialog.badfilename"));
				return;
			}
			if (generateAndExportScrambles(file, getNumberOfScrambles(), getVariation()))
				setVisible(false);
		});
		JButton htmlExportButton = new JButton(StringAccessor.getString("ScrambleExportDialog.htmlexport"));
		htmlExportButton.addActionListener(e -> {
			URL file;
			try {
				file = new URI(urlField.getText()).toURL();
			} catch (Exception e1) {
				Utils.showErrorDialog(ScrambleExportDialog.this, e1, StringAccessor.getString("ScrambleExportDialog.badfilename"));
				return;
			}
			if (exportScramblesToHTML(file, getNumberOfScrambles(), getVariation()))
				setVisible(false);
		});
		JButton cancelButton = new JButton(StringAccessor.getString("ScrambleExportDialog.cancel"));
		cancelButton.addActionListener(e -> setVisible(false));
		sideBySide = new JPanel();
		sideBySide.add(exportButton);
		sideBySide.add(htmlExportButton);
		sideBySide.add(cancelButton);
		subPanel.add(sideBySide);
		
		add(subPanel);
		
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	private int getNumberOfScrambles() {
		return numberOfScrambles.getSpinnerValue();
	}

	private ScrambleVariation getVariation() {
		ScrambleVariation var = (ScrambleVariation) scrambleChooser.getSelectedItem();
		if(scrambleLength != null) {
			var.setLength(scrambleLength.getSpinnerValue());
		}
		return var;
	}

	private boolean generateAndExportScrambles(URL outputFile, int numberOfScrambles, ScrambleVariation scrambleVariation) {
		try (PrintWriter fileWriter = new PrintWriter(new FileWriter(new File(outputFile.toURI())))) {

			ScrambleCustomization scrambleCustomization = new ScrambleCustomization(configuration, scrambleVariation, null, scramblePluginManager);
			for(int ch = 0; ch < numberOfScrambles; ch++) {
				fileWriter.println(scrambleCustomization.generateScramble().getScramble());
			}
			Utils.showConfirmDialog(this, StringAccessor.getString("ScrambleExportDialog.successmessage") + "\n" + outputFile.getPath());
			return true;

		} catch(Exception e) {
			Utils.showErrorDialog(this, e);
			return false;
		}
	}
	
	private boolean exportScramblesToHTML(URL outputFile, int numberOfScrambles, ScrambleVariation scrambleVariation) {
		File htmlFile;
		try {
			htmlFile = new File(outputFile.toURI());
		} catch (URISyntaxException e1) {
			LOG.info("unexpected exception", e1);
			Utils.showErrorDialog(this, e1);
			return false;
		}
		File imageDir = new File(htmlFile.getParentFile(), htmlFile.getName() + ".files");
		if(imageDir.isFile()){
			Utils.showErrorDialog(this, StringAccessor.getString("ScrambleExportDialog.directoryexists") + "\n" + imageDir);
			return false;
		}
		//need to check isDirectory() because mkdir() returns false if the directory exists
		if(!imageDir.isDirectory() && !imageDir.mkdir()) {
			Utils.showErrorDialog(this, StringAccessor.getString("ScrambleExportDialog.mkdirfail") + "\n" + imageDir);
			return false;
		}

		try (PrintWriter fileWriter = new PrintWriter(new FileWriter(new File(outputFile.toURI())))) {

			ScrambleCustomization scrambleCustomization = new ScrambleCustomization(configuration, scrambleVariation, null, scramblePluginManager);
			Integer popupGap = configuration.getInt(VariableKey.POPUP_GAP);
			fileWriter.println("<html><head><title>Exported Scrambles</title></head><body><table>");
			for(int ch = 0; ch < numberOfScrambles; ch++) {
				ScrambleString scramble = scrambleCustomization.generateScramble();
				BufferedImage image = scramblePluginManager.getScrambleImage(scramble, popupGap,
						scramble.getScramblePlugin().getDefaultUnitSize(), scramblePluginManager.getColorScheme(scramble.getScramblePlugin(), false));

				File file = new File(imageDir, "scramble" + ch + ".png");
				ImageIO.write(image, "png", file);
				fileWriter.println("<tr><td>" + (ch + 1) + "</td><td width='100%'>" + scramble.toString()
						+ "</td><td><img src='" + imageDir.getName() + File.separator + file.getName() + "'></td></tr>");
			}
			fileWriter.println("</table></body></html>");

			Utils.showConfirmDialog(this, StringAccessor.getString("ScrambleExportDialog.successmessage") + "\n" + outputFile.getPath());
			return true;

		} catch(Exception e) {
			LOG.info("unexpected exception", e);
			Utils.showErrorDialog(this, e);
			return false;
		}
	}
}
