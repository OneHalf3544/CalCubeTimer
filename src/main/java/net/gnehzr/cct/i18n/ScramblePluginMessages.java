package net.gnehzr.cct.i18n;

import net.gnehzr.cct.scrambles.ScramblePlugin;
import org.apache.log4j.Logger;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ScramblePluginMessages implements MessageAccessor {

	private static final Logger LOG = Logger.getLogger(ScramblePluginMessages.class);

	private static ResourceBundle RESOURCE_BUNDLE = null;

	public static final MessageAccessor SCRAMBLE_ACCESSOR = new ScramblePluginMessages();
	private ScramblePluginMessages() {}
	
	private static String bundleFileName;
	public static void loadResources(String pluginName) {
		bundleFileName = ScramblePlugin.SCRAMBLE_PLUGIN_PACKAGE + pluginName;
		try {
			RESOURCE_BUNDLE = ResourceBundle.getBundle(bundleFileName);
		} catch(MissingResourceException e) {
			RESOURCE_BUNDLE = null;
		}
	}

	public String getString(String key) {
		if(RESOURCE_BUNDLE == null) {
			String error = "Could not find " + bundleFileName + ".properties!";
			LOG.error(error);
			return error;
		}
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			LOG.error("Could not find " + key + "!");
			return '!' + key + '!';
		}
	}
}
