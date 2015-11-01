package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.CCTFileChooser;
import net.gnehzr.cct.misc.JSpinnerWithText;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ScrambleExportDialog extends JDialog {

	private static final Logger LOG = LogManager.getLogger(ScrambleExportDialog.class);

	private final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	private JTextField urlField;
	private PuzzleTypeComboBox scrambleChooser;
	private JSpinnerWithText scrambleLengthJSpinner;
	private JSpinnerWithText numberOfScramblesJSpinner;

	public ScrambleExportDialog(JFrame owner, PuzzleType selectedPuzzleType,
								ScramblePluginManager scramblePluginManager,
								Configuration configuration) {
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

		scrambleChooser = new PuzzleTypeComboBox(this.scramblePluginManager, this.configuration);
		scrambleChooser.setSelectedItem(selectedPuzzleType);
		scrambleChooser.addActionListener(e -> {
            //if(scrambleLengthJSpinner != null) {
                PuzzleType puzzleType = (PuzzleType) scrambleChooser.getSelectedItem();
                scrambleLengthJSpinner.setValue(scramblePluginManager.getScrambleVariation(puzzleType).getLength());
                numberOfScramblesJSpinner.setValue(scramblePluginManager.getPuzzleTypeByVariation(puzzleType).getRASize(RollingAverageOf.OF_5));
            //}
        });

		JPanel subPanel = new JPanel();
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));

		JPanel sideBySide = new JPanel();
		sideBySide.setLayout(new BoxLayout(sideBySide, BoxLayout.X_AXIS));
		sideBySide.add(urlField);
		sideBySide.add(browseButton);
		
		subPanel.add(sideBySide);
		subPanel.add(scrambleChooser);

		scrambleLengthJSpinner = new JSpinnerWithText(
				scramblePluginManager.getScrambleVariation(selectedPuzzleType).getLength(),
				1,
				StringAccessor.getString("ScrambleExportDialog.lengthscrambles"));

		numberOfScramblesJSpinner = new JSpinnerWithText(
				selectedPuzzleType.getRASize(RollingAverageOf.OF_5),
				1,
				StringAccessor.getString("ScrambleExportDialog.numberscrambles"));

		subPanel.add(scrambleLengthJSpinner);
		subPanel.add(numberOfScramblesJSpinner);

		JButton exportButton = new JButton(StringAccessor.getString("ScrambleExportDialog.export"));
		exportButton.addActionListener(e -> {
			URL file;
			try {
				file = new URI(urlField.getText()).toURL();
			} catch (Exception e1) {
				Utils.showErrorDialog(ScrambleExportDialog.this, e1, StringAccessor.getString("ScrambleExportDialog.badfilename"));
				return;
			}
			if (generateAndExportScrambles(file, getNumberOfScramblesJSpinner(), selectedPuzzleType))
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
			if (exportScramblesToHTML(file, getNumberOfScramblesJSpinner(), selectedPuzzleType))
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

	private int getNumberOfScramblesJSpinner() {
		return numberOfScramblesJSpinner.getSpinnerValue();
	}

	private boolean generateAndExportScrambles(URL outputFile, int numberOfScrambles, PuzzleType puzzleType) {
		try (PrintWriter fileWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFile.toURI())), "UTF-8"))) {

			for(int ch = 0; ch < numberOfScrambles; ch++) {
				fileWriter.println(puzzleType.generateScramble(scramblePluginManager.getScrambleVariation(puzzleType)).getScramble());
			}
			Utils.showConfirmDialog(this, StringAccessor.getString("ScrambleExportDialog.successmessage") + "\n" + outputFile.getPath());
			return true;

		} catch(Exception e) {
			Utils.showErrorDialog(this, e);
			return false;
		}
	}
	
	private boolean exportScramblesToHTML(URL outputFile, int numberOfScrambles, PuzzleType puzzleType) {
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

			Integer popupGap = configuration.getInt(VariableKey.POPUP_GAP);
			fileWriter.println("<html><head><title>Exported Scrambles</title></head><body><table>");
			for(int ch = 0; ch < numberOfScrambles; ch++) {
				ScrambleString scramble = puzzleType.generateScramble(scramblePluginManager.getScrambleVariation(puzzleType));
				BufferedImage image = scramblePluginManager.getScrambleImage(scramble, popupGap,
						scramble.getScramblePlugin().getDefaultUnitSize(),
						scramblePluginManager.getColorScheme(scramble.getScramblePlugin(), false, configuration));

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
