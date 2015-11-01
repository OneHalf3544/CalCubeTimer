package net.gnehzr.cct.configuration;

import net.gnehzr.cct.statistics.Profile;

@FunctionalInterface
public interface ConfigurationChangeListener {

	void configurationChanged(Profile currentProfile);
}
