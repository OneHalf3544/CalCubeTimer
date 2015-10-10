package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicMenuItem extends JMenuItem
		implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable{

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
	private DynamicString s = null;
	private final Configuration configuration;

	public DynamicMenuItem(Configuration configuration){
		this.configuration = configuration;
		configuration.addConfigurationChangeListener(changeListener);
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
