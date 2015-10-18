package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;

public class DynamicCheckBoxMenuItem extends JCheckBoxMenuItem implements DynamicStringSettable {

	private DynamicString currentDynamicString = null;

	public DynamicCheckBoxMenuItem(Configuration configuration){
	}

	@Override
	public void setDynamicString(DynamicString newDynamicString){
		this.currentDynamicString = newDynamicString;
	}

	@Override
	public void updateTextFromDynamicString(Configuration configuration, SessionsList sessionsList) {
		if(currentDynamicString != null) {
			setText(currentDynamicString.toString(sessionsList));
		}
	}

}
