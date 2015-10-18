package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;

public class DynamicLabel extends JLabel implements DynamicStringSettable {

	private DynamicString dynamicString = null;

	public DynamicLabel(){
	}

	@Override
	public void setDynamicString(DynamicString s){
		this.dynamicString = s;
	}

	@Override
	public void updateTextFromDynamicString(Configuration configuration, SessionsList sessionsList) {
		if(dynamicString != null) {
			setText(dynamicString.toString(sessionsList));
		}
	}
}
