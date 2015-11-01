package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;
import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import javax.swing.border.Border;

public class DynamicSelectableLabel extends JEditorPane implements DynamicStringSettable {

	private DynamicString s = null;

	public DynamicSelectableLabel(){
		super("text/html", null);
		putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		setEditable(false);
		setBorder(null);
		setOpaque(false);
	}
	
	@Override
	public void updateUI() {
		Border b = getBorder();
		super.updateUI();
		setBorder(b);
	}

	@Override
	public void setDynamicString(DynamicString s){
		this.s = s;
	}

	@Override
	public void updateTextFromDynamicString(Configuration configuration, SessionsList sessionsList) {
		if(s != null) {
			setText(s.toString(sessionsList));
		}
	}
}
