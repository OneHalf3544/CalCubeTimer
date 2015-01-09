package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicLabel extends JLabel implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable {

	private final ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
		@Override
		public void configurationChanged(Profile currentProfile) {
			DynamicLabel.this.update();
		}

		@Override
		public String toString() {
			return "DynamicLabel: " + getText();
		}
	};

	private final Configuration configuration;
	private DynamicString s = null;

	public DynamicLabel(Configuration configuration){
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
			update();
		}
	}

	@Override
	public void update(){
		if(s != null) {
			setText(s.toString());
		}
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(changeListener);
	}
}
