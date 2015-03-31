package net.gnehzr.cct.configuration;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.dao.ConfigurationDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.fonts.FontPolicy;
import org.jvnet.substance.fonts.FontSet;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Configuration {

	private static final Logger LOG = LogManager.getLogger(Configuration.class);

	private static File root;

	private final File documentationFile;
	private final File dynamicStringsFile;
	private final File voicesFolder;
	private final File guiLayoutsFolder;
	private final File languagesFolder;

	private final File flagsFolder;

	private final LocaleAndIcon jvmDefaultLocale;

	private final Pattern languageFile = Pattern.compile("^language_(.*).properties$");

	private final File defaultsFile;
	private final ConfigurationDao configurationDao;

	@Inject
	public Configuration(ConfigurationDao configurationDao) throws IOException {
		this(getRootDirectory(), configurationDao);
	}

	public Configuration(File rootDirectory, ConfigurationDao configurationDao) throws IOException {
		this.configurationDao = configurationDao;
		documentationFile = new File(rootDirectory, "documentation/readme.html");
		dynamicStringsFile = new File(rootDirectory, "documentation/dynamicstrings.html");

		voicesFolder = new File(rootDirectory, "voices/");
		guiLayoutsFolder = new File(rootDirectory, "guiLayouts/");
		languagesFolder = new File(rootDirectory, "languages/");
		flagsFolder = new File(languagesFolder, "flags/");
		defaultsFile = new File(rootDirectory, "profiles/defaults.properties");
		jvmDefaultLocale = new LocaleAndIcon(flagsFolder, Locale.getDefault(), null);
	}

	// using for unit-tests
	public Configuration(SortedProperties userProperties) {
		this.configurationDao = null;
		this.userProperties = userProperties;
		documentationFile = null;
		dynamicStringsFile = null;

		voicesFolder = null;
		guiLayoutsFolder = null;
		languagesFolder = null;
		flagsFolder = null;
		defaultsFile = null;
		jvmDefaultLocale = new LocaleAndIcon(flagsFolder, Locale.getDefault(), null);
	}

	public DateTimeFormatter getDateFormat() {
		return DateTimeFormatter.ofPattern(userProperties.getString(VariableKey.DATE_FORMAT, false));
	}
	
	private CopyOnWriteArrayList<ConfigurationChangeListener> listeners = new CopyOnWriteArrayList<>();

	public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
		LOG.trace("register listener: " + listener);
		listeners.add(listener);
	}
	public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
		LOG.trace("remove listener: " + listener);
		listeners.remove(listener);
	}

	public void apply(Profile currentProfile) {
		listeners.forEach((l) -> l.configurationChanged(currentProfile));
	}

	public static File getRootDirectory() {
        if (root != null) {
            return root;
        }
        try {
			String rootDir = System.getProperty("rootDir");
			if (rootDir != null) {
				root = new File(URI.create(rootDir));
			} else {
				root = new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			}
            if(root.isFile()) {
                root = root.getParentFile();
            }
        	return root;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
	}

	//returns empty string if everything is fine, error message otherwise
	public String getStartupErrors() {
		String seriousError = "";
		if (!defaultsFile.exists()) {
			seriousError += "Couldn't find file!\n" + defaultsFile.getAbsolutePath() + "\n";
		}
		File[] layouts = getXMLLayoutsAvailable();
		if (layouts == null || layouts.length == 0) {
			seriousError += "Couldn't find file!\n" + guiLayoutsFolder.getAbsolutePath() + "\n";
		}
		return seriousError;
	}

	@NotNull
	private SortedProperties userProperties = SortedProperties.NOT_LOADED_PROPERTIES;

	public void loadConfiguration(Profile profile) {
		userProperties = SortedProperties.load(profile, configurationDao, defaultsFile);
	}

	public boolean isPropertiesLoaded() {
		return userProperties != SortedProperties.NOT_LOADED_PROPERTIES;
	}

	//********* Start of specialized methods ***************//

	public void setProfileOrdering(List<Profile> profiles) {
        profileOrdering = Joiner.on("|").join(profiles);
	}

	public String profileOrdering;

	//returns file stored in props file, if available
	//otherwise, returns any available layout
	//otherwise, returns null
	public File getXMLGUILayout() {
		for(File file : getXMLLayoutsAvailable()) {
			if(file.getName().equalsIgnoreCase(userProperties.getString(VariableKey.XML_LAYOUT, false))) {
				return file;
			}
		}
		if(getXMLLayoutsAvailable() == null)
			return null;
		return getXMLLayoutsAvailable()[0];
	}
	public File getXMLFile(String xmlGUIName) {
		for(File f : getXMLLayoutsAvailable()) {
			if(f.getName().equalsIgnoreCase(xmlGUIName))
				return f;
		}
		return null;
	}
	private File[] availableLayouts;

	public File[] getXMLLayoutsAvailable() {
		if(availableLayouts == null) {
			availableLayouts = guiLayoutsFolder.listFiles(
					(dir, name) -> name.endsWith(".xml") && new File(dir, name).isFile());
		}
		return availableLayouts;
	}
	
	public Font getFontForLocale(LocaleAndIcon l) {
		Font newFont = defaultSwingFont;
		if(defaultSwingFont.canDisplayUpTo(l.toString()) != -1)
			for(Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
				if(f.canDisplayUpTo(l.toString()) == -1) {
					newFont = f;
					break;
				}
		return newFont.deriveFont(12f);
	}

	public LocaleAndIcon getDefaultLocale() {
		LocaleAndIcon l = new LocaleAndIcon(
				flagsFolder,
				new Locale(userProperties.getString(VariableKey.LANGUAGE, false), userProperties.getString(VariableKey.REGION, false)),
				null);
		if(getAvailableLocales().contains(l))
			return l;
		return jvmDefaultLocale;
	}

	public URI getDynamicStringsFile() {
		return dynamicStringsFile.toURI();
	}

	public URI getDocumentationFile() {
		return documentationFile.toURI();
	}

	public File getVoicesFolder() {
		return voicesFolder;
	}

	public void saveConfiguration(@NotNull Profile profile) {
		userProperties.saveConfiguration(profile, configurationDao);
	}

	public boolean getBoolean(VariableKey<Boolean> key, boolean defaultValue) {
		return userProperties.getBoolean(key, defaultValue);
	}

	public String getString(String substring) {
		return userProperties.getString(new VariableKey<>(substring), false);
	}

	public List<String> getStringArray(VariableKey<List<String>> solveTags, boolean defaultValue) {
		return userProperties.getStringArray(solveTags, defaultValue);
	}

	public void setLong(VariableKey<Integer> key, long newValue) {
		userProperties.setLong(key, newValue);
	}

	public void setBoolean(VariableKey<Boolean> booleanVariableKey, boolean newValue) {
		userProperties.setBoolean(booleanVariableKey, newValue);
	}

	public Integer getInt(VariableKey<Integer> integerVariableKey, boolean defaultValue) {
		Long aLong = userProperties.getLong(integerVariableKey, defaultValue);
		return aLong == null ? null : aLong.intValue();
	}

	@NotNull
	public String getString(VariableKey<String> key, boolean defaults) {
		return userProperties.getString(key, defaults);
	}

	@Nullable
	public String getNullableString(VariableKey<String> key, boolean defaults) {
		return userProperties.getNullableString(key, defaults);
	}

	public void setString(VariableKey<String> key, String s) {
		userProperties.setString(key, s);
	}

	public Color getColorNullIfInvalid(VariableKey<Color> colorVariableKey, boolean defaults) {
		return userProperties.getColorNullIfInvalid(colorVariableKey, defaults);
	}

	public void setIntegerArray(VariableKey<Integer[]> variableKey, Integer[] ordering) {
		userProperties.setIntegerArray(variableKey, ordering);
	}

	public Integer[] getIntegerArray(VariableKey<Integer[]> variableKey, boolean b) {
		return userProperties.getIntegerArray(variableKey, b);
	}

	public void setDimension(VariableKey<Dimension> ircFrameDimension, Dimension size) {
		userProperties.setDimension(ircFrameDimension, size);
	}

	public void setPoint(VariableKey<Point> ircFrameLocation, Point location) {
		userProperties.setPoint(ircFrameLocation, location);
	}

	public void setStringArray(VariableKey<List<String>> valuesKey, List<?> items) {
		userProperties.setStringArray(valuesKey, items);
	}

	public Font getFont(VariableKey<Font> scrambleFont, boolean b) {
		return userProperties.getFont(scrambleFont, b);
	}

	public Color getColor(VariableKey<Color> timerFg, boolean b) {
		return userProperties.getColor(timerFg, b);
	}

	public Dimension getDimension(VariableKey<Dimension> ircFrameDimension, boolean b) {
		return userProperties.getDimension(ircFrameDimension);
	}

	public float getFloat(VariableKey<Float> opacity, boolean b) {
		return userProperties.getFloat(opacity, b);
	}

	public Point getPoint(VariableKey<Point> ircFrameLocation, boolean b) {
		return userProperties.getPoint(ircFrameLocation, b);
	}

	public void setColor(VariableKey<Color> currentAverage, Color background) {
		userProperties.setColor(currentAverage, background);
	}

	public void setFont(VariableKey<Font> timerFont, Font font) {
		userProperties.setFont(timerFont, font);
	}

	public void setDouble(VariableKey<Double> minSplitDifference, Double value) {
		userProperties.setDouble(minSplitDifference, value);
	}

	public void setFloat(VariableKey<Float> opacity, float v) {
		userProperties.setFloat(opacity, v);
	}

	public Double getDouble(VariableKey<Double> key, boolean defaultValue) {
		return userProperties.getDouble(key, defaultValue);
	}

	public Duration getDuration(VariableKey<Duration> delayUntilInspection, boolean b) {
		Long aLong = userProperties.getLong(delayUntilInspection.toKey(), b);
		return aLong == null ? null : Duration.ofMillis(aLong);
	}

	public boolean keyExists(VariableKey<Boolean> key) {
		return userProperties.keyExists(key);
	}

	static class SubstanceFontPolicy implements FontPolicy {
		FontUIResource f;
		public void setFont(Font f) {
			this.f = new FontUIResource(f);
		}
		@Override
		public FontSet getFontSet(String arg0, UIDefaults arg1) {
			return new FontSet() {
				@Override
				public FontUIResource getControlFont() {
					return f;
				}
				@Override
				public FontUIResource getMenuFont() {
					return f;
				}
				@Override
				public FontUIResource getMessageFont() {
					return f;
				}
				@Override
				public FontUIResource getSmallFont() {
					return f;
				}
				@Override
				public FontUIResource getTitleFont() {
					return f;
				}
				@Override
				public FontUIResource getWindowTitleFont() {
					return f;
				}
			};
		}
	}

	private SubstanceFontPolicy currentFontPolicy = new SubstanceFontPolicy();
	private Font defaultSwingFont = SubstanceLookAndFeel.getFontPolicy().getFontSet(null, null).getTitleFont();
	public void setDefaultLocale(LocaleAndIcon li) {
		Locale l = li.getLocale();
		userProperties.setString(VariableKey.LANGUAGE, l.getLanguage());
		userProperties.setString(VariableKey.REGION, l.getCountry());
		Locale.setDefault(l);
		currentFontPolicy.setFont(getFontForLocale(li));
		SubstanceLookAndFeel.setFontPolicy(currentFontPolicy);
	}
	
	private List<LocaleAndIcon> locales;

	public List<LocaleAndIcon> getAvailableLocales() {
		if (locales == null) {
			locales = new ArrayList<>();

			for(File langFile : checkNotNull(languagesFolder.listFiles())) {
				Matcher m = languageFile.matcher(langFile.getName());
				if (!m.matches()) {
					continue;
				}

				Optional<Locale> locale = parseLocale(m.group(1));
				if (!locale.isPresent()) {
                    continue;
                }

				LocaleAndIcon li = new LocaleAndIcon(flagsFolder, locale.get(), getLanguageName(langFile));
				if(!locales.contains(li)) {
                    locales.add(li);
                }
			}
		}
		return locales;
	}

	private String getLanguageName(File lang) {
		try (InputStream in = new FileInputStream(lang)) {
			Properties prop = new Properties();
			prop.load(in);
			return prop.getProperty("language");
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private Optional<Locale> parseLocale(String locale) {
		String[] language_region = locale.split("_");
		Locale l;
		if (language_region.length == 1) {
			return Optional.of(new Locale(language_region[0]));
		}
		else if(language_region.length == 2) {
			return Optional.of(new Locale(language_region[0], language_region[1]));
		}
		return Optional.empty();
	}

	//********* End of specialized methods ***************//
}
