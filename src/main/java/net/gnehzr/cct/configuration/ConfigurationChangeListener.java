package net.gnehzr.cct.configuration;

import net.gnehzr.cct.statistics.Profile;

public interface ConfigurationChangeListener {

	public void configurationChanged(Profile currentProfile);
}
