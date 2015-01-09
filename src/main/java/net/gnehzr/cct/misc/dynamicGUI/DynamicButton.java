package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicButton extends JButton implements StatisticsUpdateListener, DynamicStringSettable, DynamicDestroyable{

	private final ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
        @Override
        public void configurationChanged(Profile currentProfile) {
            update();
        }

		@Override
		public String toString() {
			return getClass().getEnclosingClass().getSimpleName() + ": " + getText();
		}
	};

	private DynamicString s = null;
	private final Configuration configuration;

	public DynamicButton(Configuration configuration){
		this.configuration = configuration;
		this.configuration.addConfigurationChangeListener(changeListener);
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
