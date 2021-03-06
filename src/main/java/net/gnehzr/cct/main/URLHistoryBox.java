package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.pushingpixels.lafwidget.LafWidget;
import org.pushingpixels.substance.internal.utils.combo.SubstanceComboBoxEditor;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.List;

public class URLHistoryBox extends JComboBox<String> implements KeyListener {

	private VariableKey<List<String>> valuesKey;
	private IncrementalComboBoxModel model;
	private JTextField editor;
	private final Configuration configuration;

	public URLHistoryBox(VariableKey<List<String>> valuesKey, Configuration configuration) {
		this.valuesKey = valuesKey;
		this.configuration = configuration;

		setEditor(new SubstanceComboBoxEditor() {
			@Override
			public void setItem(Object anObject) {} //we set the text from IncrementalComboBoxModel instead
		});
		editor = (JTextField) getEditor().getEditorComponent();
		editor.addKeyListener(this);

		// todo values = configuration.getStringArray(valuesKey, false); (it doesn't work at initialization stage, for unknown user)
		List<String> values = Collections.emptyList();
		model = new IncrementalComboBoxModel(values, editor);

		setModel(model);
		setEditable(true);
		putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, true);
	}
	
	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
		SwingUtilities.invokeLater(this::editorUpdated);
	}
	
	private void editorUpdated() {
		hidePopup();
		model.setPrefix(editor.getText());
		if(model.getSize() != 0)
			showPopup();
	}
	
	public void commitCurrentItem() {
		model.addElement(editor.getText());
		configuration.setStringArray(valuesKey, model.getItems());
	}

}
