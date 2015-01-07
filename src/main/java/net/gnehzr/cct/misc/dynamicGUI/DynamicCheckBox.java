package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;

public class DynamicCheckBox extends JCheckBox implements StatisticsUpdateListener, DynamicStringSettable, ConfigurationChangeListener, DynamicDestroyable{
	private DynamicString s = null;
	private final Configuration configuration;

	public DynamicCheckBox(Configuration configuration){
		configuration.addConfigurationChangeListener(this);
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
			update();
		}
	}

	@Override
	public void update(){
		if(s != null) setText(s.toString());
	}

	@Override
	public void configurationChanged(Profile profile){
		update();
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(this);
	}
}
