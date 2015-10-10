package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicLabel extends JLabel implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable {

	private final ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
		@Override
		public void configurationChanged(Profile currentProfile) {
			DynamicLabel.this.update(currentProfile.getSessionsListTableModel().getSessionsList());
		}

		@Override
		public String toString() {
			return "DynamicLabel: " + getText();
		}
	};

	private final Configuration configuration;
	private DynamicString dynamicString = null;

	public DynamicLabel(Configuration configuration){
		this.configuration = configuration;
		configuration.addConfigurationChangeListener(changeListener);
	}

	@Override
	public void setDynamicString(DynamicString s){
		if(this.dynamicString != null) {
			this.dynamicString.getStatisticsModel().removeStatisticsUpdateListener(this);
		}
		this.dynamicString = s;
		if(this.dynamicString != null) {
			this.dynamicString.getStatisticsModel().addStatisticsUpdateListener(this);
			//update((SessionsList)null);
		}
	}

	@Override
	public void update(SessionsList sessions){
		if(dynamicString != null) {
			setText(dynamicString.toString(sessions));
		}
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(changeListener);
	}
}
