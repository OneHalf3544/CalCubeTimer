package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;

public class DynamicButton extends JButton implements DynamicStringSettable {

	private DynamicString s = null;

	public DynamicButton(){
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
