package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.CCTFileChooser;
import net.gnehzr.cct.misc.JTextAreaWithHistory;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.lafwidget.LafWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ScrambleImportDialog extends JDialog {

	private static final Logger LOG = LogManager.getLogger(ScrambleImportDialog.class);
	private URLHistoryBox urlField;

	private final Configuration configuration;
	private JButton importButton;
	private JTextAreaWithHistory scramblesTextArea;
	private JEditorPane qualityControl;
	private ScrambleChooserComboBox<ScrambleCustomization> scrambleChooser;

	private List<ScrambleString> scrambles = new ArrayList<>();

	public ScrambleImportDialog(CALCubeTimerFrame calCubeTimerFrame, ScrambleImporter scrambleImporter,
								ScrambleCustomization sc,
								ScramblePluginManager scramblePluginManager, Configuration configuration) {
		super(calCubeTimerFrame, StringAccessor.getString("ScrambleImportDialog.importscrambles"), true);
		this.configuration = configuration;

		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		JPanel topBot = new JPanel();
		topBot.setLayout(new BoxLayout(topBot, BoxLayout.Y_AXIS));

		JPanel sideBySide = new JPanel();
		sideBySide.setLayout(new BoxLayout(sideBySide, BoxLayout.X_AXIS));
		urlField = new URLHistoryBox(VariableKey.IMPORT_URLS, this.configuration);
		urlField.setSelectedItem(configuration.getString(VariableKey.DEFAULT_SCRAMBLE_URL, false));
		urlField.setToolTipText(StringAccessor.getString("ScrambleImportDialog.browsescrambles")); 
		sideBySide.add(urlField);
		JButton browseButton = new JButton(StringAccessor.getString("ScrambleImportDialog.browse"));
		browseButton.addActionListener(this::browseButtonListener);
		sideBySide.add(browseButton);
		JButton addToAreaButton = new JButton(StringAccessor.getString("ScrambleImportDialog.add"));
		addToAreaButton.addActionListener(this::addToAreaButtonListener);
		sideBySide.add(addToAreaButton);
		topBot.add(sideBySide);

		scrambleChooser = new ScrambleCustomizationChooserComboBox(false, scramblePluginManager, configuration);
		scrambleChooser.addItem(scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION);
		scrambleChooser.setSelectedItem(sc);
		scrambleChooser.addActionListener(e -> this.validateScrambles());
		topBot.add(scrambleChooser);

		contentPane.add(topBot, BorderLayout.PAGE_START);
		
		scramblesTextArea = new JTextAreaWithHistory();
		scramblesTextArea.getDocument().addDocumentListener(createDocumentListener());
		scramblesTextArea.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		JScrollPane scramblePane = new JScrollPane(scramblesTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		qualityControl = new JEditorPane();
		qualityControl.setContentType("text/html"); 
		qualityControl.setEditable(false);
		qualityControl.setFocusable(false);
		scramblePane.setRowHeaderView(new JScrollPane(qualityControl));
		scramblesTextArea.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
		qualityControl.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
		qualityControl.setMinimumSize(new Dimension(25, 0));
		contentPane.add(scramblePane, BorderLayout.CENTER);
		
		importButton = new JButton(StringAccessor.getString("ScrambleImportDialog.import")); 
		importButton.setEnabled(false);
		importButton.addActionListener(e -> {
			scrambleImporter.importScrambles(getSelectedCustomization(), scrambles, calCubeTimerFrame);
			setVisible(false);
		});
		JButton cancelButton = new JButton(StringAccessor.getString("ScrambleImportDialog.cancel"));
		cancelButton.addActionListener(e -> setVisible(false));
		sideBySide = new JPanel();
		sideBySide.add(importButton);
		sideBySide.add(cancelButton);
		contentPane.add(sideBySide, BorderLayout.PAGE_END);
		
		validateScrambles();
		setMinimumSize(new Dimension(450, 250));
		pack();
		setLocationRelativeTo(calCubeTimerFrame);
		setVisible(true);
	}

	private DocumentListener createDocumentListener() {
		return new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {}

			@Override
			public void insertUpdate(DocumentEvent e) {
				validateScrambles();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				validateScrambles();
			}
		};
	}

	private void browseButtonListener(ActionEvent e) {
		CCTFileChooser fc = new CCTFileChooser(configuration);
		if(fc.showDialog(this, StringAccessor.getString("ScrambleImportDialog.open")) == CCTFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            urlField.setSelectedItem(selectedFile.toURI().toString());
            if (!selectedFile.exists()) {
                Utils.showErrorDialog(this, StringAccessor.getString("ScrambleImportDialog.filenotfound") + " " + selectedFile.getName());
                urlField.setSelectedItem("");
            }
        }
	}

	private void addToAreaButtonListener(ActionEvent e) {
		URL url;
		try {
            url = new URI(urlField.getSelectedItem().toString()).toURL();
        } catch (MalformedURLException | URISyntaxException ee) {
            Utils.showErrorDialog(this, ee, StringAccessor.getString("ScrambleImportDialog.badname"));
            return;
        }
		try (InputStream input = url.openStream()) {

            scramblesTextArea.append(IOUtils.toString(input));
            urlField.commitCurrentItem();
        }
        catch(ConnectException ee) {
            Utils.showErrorDialog(this, ee, StringAccessor.getString("ScrambleImportDialog.connectionrefused"));
        }
        catch(FileNotFoundException ee) {
            Utils.showErrorDialog(this, ee, url + "\n" + StringAccessor.getString("ScrambleImportDialog.notfound"));
        }
        catch(Exception ee) {
            LOG.info("unexpected exception", ee);
            Utils.showErrorDialog(this, ee);
        }
	}

	private ScrambleCustomization getSelectedCustomization() {
		return (ScrambleCustomization) scrambleChooser.getSelectedItem();
	}

	private void validateScrambles() {
		ScrambleCustomization sc = getSelectedCustomization();
		
		Font font = scramblesTextArea.getFont();
		String fontStyle = ""; 
		if(font.isItalic())
			fontStyle += "font-style: italic; ";
		else if(font.isPlain())
			fontStyle += "font-style: normal; ";
		if(font.isBold())
			fontStyle += "font-weight: bold; ";
		else
			fontStyle += "font-weight: normal; ";
		StringBuilder validationString = new StringBuilder("<html><head><style type=\"text/css\">") 
			.append("span {text-align: center; font-family: ").append(font.getFamily()).append("; font-size: ").append(font.getSize()).append("; ")   
			.append(fontStyle).append(";}") 
			.append("span.green {color: green;}") 
			.append("span.red {color: red;}") 
			.append("</style></head><body>"); 
		String[] importedScrams = scramblesTextArea.getText().split("\n", -1); //-1 allows for trailing \n
		boolean perfect = true;
		boolean empty = true;
		int scramNumber = 1;
		scrambles.clear();
		for (String importedScram : importedScrams) {
			if (!importedScram.trim().isEmpty()) {
				empty = false;
				try {
					scrambles.add(sc.importScramble(importedScram));
					validationString.append("<span class=\"green\">O");
				} catch (InvalidScrambleException e) {
					perfect = false;
					validationString.append("<span class=\"red\">X");
				}
				validationString.append(" ").append(scramNumber).append(". ");
				scramNumber++;
			} else {
				validationString.append("<span>");
			}
			validationString.append("<br></span>");
		}
		validationString.append("</body></html>"); 
		qualityControl.setText(validationString.toString());
		importButton.setEnabled(perfect && !empty);
		validate();
	}
}
