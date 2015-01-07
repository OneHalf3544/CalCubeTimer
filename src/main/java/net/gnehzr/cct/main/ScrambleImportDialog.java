package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.CCTFileChooser;
import net.gnehzr.cct.misc.JTextAreaWithHistory;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.Scramble;
import net.gnehzr.cct.scrambles.Scramble.InvalidScrambleException;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.statistics.ProfileDao;
import org.apache.log4j.Logger;
import org.jvnet.lafwidget.LafWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class ScrambleImportDialog extends JDialog implements ActionListener, DocumentListener {

	private static final Logger LOG = Logger.getLogger(ScrambleImportDialog.class);
	private final ScramblePlugin scramblePlugin;
	private URLHistoryBox urlField;

	private final Configuration configuration;
	private JButton browse, addToArea;
	private JTextAreaWithHistory scrambles;
	private JEditorPane qualityControl;
	private ScrambleChooserComboBox scrambleChooser;
	private JButton importButton, cancelButton;
	private CALCubeTimer cct;

	public ScrambleImportDialog(ProfileDao profileDao, CALCubeTimer cct, ScrambleCustomization sc, ScramblePlugin scramblePlugin, Configuration configuration) {
		super(cct, StringAccessor.getString("ScrambleImportDialog.importscrambles"), true); 
		this.cct = cct;
		this.scramblePlugin = scramblePlugin;
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
		browse = new JButton(StringAccessor.getString("ScrambleImportDialog.browse")); 
		browse.addActionListener(this);
		sideBySide.add(browse);
		addToArea = new JButton(StringAccessor.getString("ScrambleImportDialog.add")); 
		addToArea.addActionListener(this);
		sideBySide.add(addToArea);
		topBot.add(sideBySide);

		scrambleChooser = new ScrambleChooserComboBox(false, true, this.scramblePlugin, this.configuration, profileDao);
		scrambleChooser.addItem(this.scramblePlugin.NULL_SCRAMBLE_CUSTOMIZATION);
		scrambleChooser.setSelectedItem(sc);
		scrambleChooser.addActionListener(this);
		topBot.add(scrambleChooser);

		contentPane.add(topBot, BorderLayout.PAGE_START);
		
		scrambles = new JTextAreaWithHistory();
		scrambles.getDocument().addDocumentListener(this);
		scrambles.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		JScrollPane scramblePane = new JScrollPane(scrambles, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		qualityControl = new JEditorPane();
		qualityControl.setContentType("text/html"); 
		qualityControl.setEditable(false);
		qualityControl.setFocusable(false);
		scramblePane.setRowHeaderView(new JScrollPane(qualityControl));
		scrambles.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
		qualityControl.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
		qualityControl.setMinimumSize(new Dimension(25, 0));
		contentPane.add(scramblePane, BorderLayout.CENTER);
		
		importButton = new JButton(StringAccessor.getString("ScrambleImportDialog.import")); 
		importButton.setEnabled(false);
		importButton.addActionListener(this);
		cancelButton = new JButton(StringAccessor.getString("ScrambleImportDialog.cancel")); 
		cancelButton.addActionListener(this);
		sideBySide = new JPanel();
		sideBySide.add(importButton);
		sideBySide.add(cancelButton);
		contentPane.add(sideBySide, BorderLayout.PAGE_END);
		
		validateScrambles();
		setMinimumSize(new Dimension(450, 250));
		pack();
		setLocationRelativeTo(cct);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == browse) {
			CCTFileChooser fc = new CCTFileChooser(configuration);
			if(fc.showDialog(this, StringAccessor.getString("ScrambleImportDialog.open")) == CCTFileChooser.APPROVE_OPTION) { 
				File selectedFile = fc.getSelectedFile();
				urlField.setSelectedItem(selectedFile.toURI().toString());
				if(!selectedFile.exists()) {
					Utils.showErrorDialog(this, StringAccessor.getString("ScrambleImportDialog.filenotfound") + " " + selectedFile.getName());
					urlField.setSelectedItem(""); 
				}
			}
		} else if(source == addToArea) {
			URL url;
			try {
				url = new URI(urlField.getSelectedItem().toString()).toURL();
			} catch(Exception ee) {
				Utils.showErrorDialog(this, ee, StringAccessor.getString("ScrambleImportDialog.badname"));  
				return;
			}
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String line, all = ""; 
				while((line = in.readLine()) != null) {
					all += line + "\n"; 
				}
				scrambles.append(all);
				in.close();
				urlField.commitCurrentItem();
			} catch(ConnectException ee) {
				Utils.showErrorDialog(this, ee, StringAccessor.getString("ScrambleImportDialog.connectionrefused"));
			} catch(FileNotFoundException ee) {
				Utils.showErrorDialog(this, ee, url + "\n" + StringAccessor.getString("ScrambleImportDialog.notfound"));
			} catch(Exception ee) {
				LOG.info("unexpected exception", ee);
				Utils.showErrorDialog(this, ee);
			}
		} else if(source == importButton) {
			cct.importScrambles(getSelectedCustomization(), scrams);
			setVisible(false);
		} else if(source == cancelButton) {
			setVisible(false);
		} else if(source == scrambleChooser) {
			validateScrambles();
		}
	}
	
	private ScrambleCustomization getSelectedCustomization() {
		return (ScrambleCustomization) scrambleChooser.getSelectedItem();
	}

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
	private ArrayList<Scramble> scrams = new ArrayList<>();
	private void validateScrambles() {
		ScrambleCustomization sc = getSelectedCustomization();
		
		Font font = scrambles.getFont();
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
		String[] importedScrams = scrambles.getText().split("\n", -1); //-1 allows for trailing \n 
		boolean perfect = true;
		boolean empty = true;
		int scramNumber = 1;
		scrams.clear();
		for (String importedScram : importedScrams) {
			if (!importedScram.trim().isEmpty()) {
				empty = false;
				try {
					scrams.add(sc.generateScramble(importedScram));
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
