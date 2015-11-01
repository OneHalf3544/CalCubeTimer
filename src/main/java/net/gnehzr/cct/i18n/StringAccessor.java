package net.gnehzr.cct.i18n;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class StringAccessor {

	private static final Logger LOG = LogManager.getLogger(StringAccessor.class);

	private static final String CCT_STRINGS = "languages/cctStrings";

	private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
		@NotNull
		@Override
		public Enumeration<String> getKeys() {
			return Collections.enumeration(Collections.emptyList());
		}
		@Override
		protected Object handleGetObject(@NotNull String key) {
			return "Couldn't find " + CCT_STRINGS + ".properties!";
		}

		@Override
		public String toString() {
			return "empty bundle";
		}
	};

	private static ResourceBundle cctStrings;

	public static String getString(String key) {
		if(cctStrings == null) {
			try {
				cctStrings = ResourceBundle.getBundle(CCT_STRINGS);
			} catch(MissingResourceException e) {
				cctStrings = EMPTY_BUNDLE;
				LOG.info("unexpected exception", e);
			}
		}
		return cctStrings.getString(key);
	}
	public static boolean keyExists(String key) {
		if (cctStrings == null) {
			try {
				cctStrings = ResourceBundle.getBundle(CCT_STRINGS);
			} catch (MissingResourceException e) {
				cctStrings = EMPTY_BUNDLE;
				LOG.info("unexpected exception", e);
			}
		}
		return cctStrings != null && cctStrings.containsKey(key);
	}

	public static String format(String formatKey, Object... values) {
		return MessageFormat.format(StringAccessor.getString(formatKey), values);
	}

	public static void clearResources() {
		cctStrings = null;
	}
}
