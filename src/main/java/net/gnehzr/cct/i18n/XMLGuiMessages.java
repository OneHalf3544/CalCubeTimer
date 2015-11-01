package net.gnehzr.cct.i18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@Singleton
public class XMLGuiMessages implements MessageAccessor {

	private static final Logger LOG = LogManager.getLogger(XMLGuiMessages.class);
	private static final String BUNDLE_NAME = "guiLayouts/";

	private static ResourceBundle RESOURCE_BUNDLE = null;

	public final MessageAccessor XMLGUI_ACCESSOR;
	private final Configuration configuration;

	@Inject
	public XMLGuiMessages(Configuration configuration) {
		this.configuration = configuration;
		XMLGUI_ACCESSOR = this;
	}
	
	private static String bundleFileName;

	public void reloadResources() {
		//we need to load this xml gui's language properties file
		String fileName = configuration.getXMLGUILayout().getName();
		fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		
		bundleFileName = BUNDLE_NAME + fileName;
		try {
			RESOURCE_BUNDLE = ResourceBundle.getBundle(bundleFileName);
		} catch(MissingResourceException e) {
			RESOURCE_BUNDLE = null;
			LOG.info("unexpected exception", e); //No need to warn the user here, they'll see it in the gui
		}
	}

	@Override
	public String getString(String key) {
		Objects.requireNonNull(RESOURCE_BUNDLE, "Could not find " + bundleFileName + ".properties!");

		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			throw new IllegalArgumentException("Could not find key "+ key + '!');
		}
	}
}
