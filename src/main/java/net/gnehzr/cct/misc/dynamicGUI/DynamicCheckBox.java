package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DynamicCheckBox extends JCheckBox implements DynamicStringSettable {

	private DynamicString s = null;

	public DynamicCheckBox(){
	}

	public DynamicCheckBox(@NotNull DynamicString s){
		this();
		this.s = s;
		setText(s.toString(null));
	}

	public DynamicString getDynamicString() {
		return s;
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
