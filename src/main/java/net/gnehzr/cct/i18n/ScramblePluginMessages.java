package net.gnehzr.cct.i18n;

import net.gnehzr.cct.scrambles.ScramblePlugin;
import org.apache.log4j.Logger;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ScramblePluginMessages implements MessageAccessor {

	private static final Logger LOG = Logger.getLogger(ScramblePluginMessages.class);

	private final ResourceBundle resourceBundle;

	private final Class<? extends ScramblePlugin> pluginClass;

	public ScramblePluginMessages(Class<? extends ScramblePlugin> pluginClass) {
		this.pluginClass = pluginClass;
		this.resourceBundle = loadResources(this.pluginClass);
	}

	private ResourceBundle loadResources(Class<? extends ScramblePlugin> pluginClass) {
		try {
			return ResourceBundle.getBundle(pluginClass.getName());
		} catch(MissingResourceException e) {
			return null;
		}
	}

	@Override
	public String getString(String key) {
		if (resourceBundle == null) {
			String error = "Could not find " + pluginClass.getSimpleName() + ".properties!";
			LOG.error(error);
			return error;
		}
		try {
			return resourceBundle.getString(key);
		} catch (MissingResourceException e) {
			LOG.error("Could not find " + key + " in " + pluginClass.getSimpleName() + "!");
			return '!' + key + '!';
		}
	}
}
