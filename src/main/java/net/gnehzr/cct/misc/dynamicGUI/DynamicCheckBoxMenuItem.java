package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicCheckBoxMenuItem extends JCheckBoxMenuItem
		implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable{

	private final Configuration configuration;
	private final ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
		@Override
		public void configurationChanged(Profile currentProfile) {
			DynamicCheckBoxMenuItem.this.update();
		}

		@Override
		public String toString() {
			return getClass().getEnclosingClass().getSimpleName() + ": " + getText();
		}
	};
	private DynamicString currentDynamicString = null;

	public DynamicCheckBoxMenuItem(Configuration configuration){
		this.configuration = configuration;
		this.configuration.addConfigurationChangeListener(changeListener);
	}

	@Override
	public void setDynamicString(DynamicString newDynamicString){
		if(this.currentDynamicString != null) {
			this.currentDynamicString.getStatisticsModel().removeStatisticsUpdateListener(this);
		}
		this.currentDynamicString = newDynamicString;
		if(newDynamicString != null) {
			newDynamicString.getStatisticsModel().addStatisticsUpdateListener(this);
			update();
		}
	}

	@Override
	public void update(){
		if(currentDynamicString != null) {
			setText(currentDynamicString.toString());
		}
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(changeListener);
	}
}
