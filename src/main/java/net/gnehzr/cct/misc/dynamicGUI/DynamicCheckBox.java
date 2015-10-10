package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicCheckBox extends JCheckBox implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable{

	private DynamicString s = null;
	private final Configuration configuration;

	private final ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
		@Override
		public void configurationChanged(Profile currentProfile) {
			update(currentProfile.getSessionsListTableModel().getSessionsList());
		}

		@Override
		public String toString() {
			return getClass().getEnclosingClass().getSimpleName() + ": " + getText();
		}
	};

	public DynamicCheckBox(Configuration configuration){
		configuration.addConfigurationChangeListener(changeListener);
		this.configuration = configuration;
	}

	public DynamicCheckBox(DynamicString s, Configuration configuration){
		this.configuration = configuration;
		setDynamicString(s);
	}

	public DynamicString getDynamicString() {
		return s;
	}
	
	@Override
	public void setDynamicString(DynamicString s){
		if(this.s != null) {
			this.s.getStatisticsModel().removeStatisticsUpdateListener(this);
		}
		this.s = s;
		if(this.s != null) {
			this.s.getStatisticsModel().addStatisticsUpdateListener(this);
			update(((SessionsList) null));
		}
	}

	@Override
	public void update(SessionsList sessions){
		if(s != null) {
			setText(s.toString(sessions));
		}
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(changeListener);
	}
}
