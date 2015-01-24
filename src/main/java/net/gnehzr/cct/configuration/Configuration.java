package net.gnehzr.cct.configuration;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.inject.Singleton;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.statistics.Profile;
import org.apache.log4j.Logger;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.fonts.FontPolicy;
import org.jvnet.substance.fonts.FontSet;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class Configuration {

	private static final Logger LOG = Logger.getLogger(Configuration.class);

	@Deprecated
	private final File documentationFile;
	@Deprecated
	private final File dynamicStringsFile;
	@Deprecated
	private final File profilesFolder;
	@Deprecated
	private final File voicesFolder;
	@Deprecated
	private final File databaseDTD;
	@Deprecated
	private final File guiLayoutsFolder;
	@Deprecated
	private final File languagesFolder;
	@Deprecated
	private final File startupProfileFile;
	private final File flagsFolder;

	private final LocaleAndIcon jvmDefaultLocale;

	private final Pattern languageFile = Pattern.compile("^language_(.*).properties$");

	private final File defaultsFile;

	public Configuration() throws IOException {
		this(getRootDirectory());
	}

	public Configuration(File rootDirectory) throws IOException {
		documentationFile = new File(rootDirectory, "documentation/readme.html");
		dynamicStringsFile = new File(rootDirectory, "documentation/dynamicstrings.html");
		profilesFolder = new File(rootDirectory, "profiles/");

		voicesFolder = new File(rootDirectory, "voices/");
		databaseDTD = new File(getProfilesFolder(), "database.dtd");
		guiLayoutsFolder = new File(rootDirectory, "guiLayouts/");
		languagesFolder = new File(rootDirectory, "languages/");
		flagsFolder = new File(languagesFolder, "flags/");
		startupProfileFile = new File(getProfilesFolder(), "startup");
		defaultsFile = new File(getProfilesFolder(), "defaults.properties");
		jvmDefaultLocale = new LocaleAndIcon(flagsFolder, Locale.getDefault(), null);
		loadConfiguration(null);
	}

	// using for unit-tests
	public Configuration(SortedProperties props) {
		this.props = props;
		documentationFile = null;
		dynamicStringsFile = null;
		profilesFolder = null;

		voicesFolder = null;
		databaseDTD = null;
		guiLayoutsFolder = null;
		languagesFolder = null;
		flagsFolder = null;
		startupProfileFile = null;
		defaultsFile = null;
		jvmDefaultLocale = new LocaleAndIcon(flagsFolder, Locale.getDefault(), null);
	}

	public DateTimeFormatter getDateFormat() {
		return DateTimeFormatter.ofPattern(props.getString(VariableKey.DATE_FORMAT, false));
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

	private static File root;

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

	public SortedProperties props;

	public void loadConfiguration(File file) throws IOException {
		props = SortedProperties.load(file, defaultsFile);
	}

	//********* Start of specialized methods ***************//

	public Profile commandLineProfile;
	//this is used for adding profiles that aren't under the "profiles" directory
	public void setCommandLineProfile(Profile profile) {
		commandLineProfile = profile;
	}

	public void setProfileOrdering(List<Profile> profiles) {
        profileOrdering = Joiner.on("|").join(profiles);
	}

	public String profileOrdering;

	//returns file stored in props file, if available
	//otherwise, returns any available layout
	//otherwise, returns null
	public File getXMLGUILayout() {
		for(File file : getXMLLayoutsAvailable()) {
			if(file.getName().equalsIgnoreCase(props.getString(VariableKey.XML_LAYOUT, false))) {
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
				new Locale(props.getString(VariableKey.LANGUAGE, false), props.getString(VariableKey.REGION, false)),
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

	public File getProfilesFolder() {
		return profilesFolder;
	}

	public File getDatabaseDTD() {
		return databaseDTD;
	}

	public File getVoicesFolder() {
		return voicesFolder;
	}

	public File getStartupProfileFile() {
		return startupProfileFile;
	}

	public void saveConfigurationToFile(File f) throws IOException {
		props.saveConfigurationToFile(f);
	}

	public boolean getBoolean(VariableKey<Boolean> key, boolean defaultValue) {
		return props.getBoolean(key, defaultValue);
	}

	public String getString(String substring) {
		return props.getString(new VariableKey<>(substring), false);
	}

	public List<String> getStringArray(VariableKey<List<String>> solveTags, boolean defaultValue) {
		return props.getStringArray(solveTags, defaultValue);
	}

	public void setLong(VariableKey<Integer> key, long newValue) {
		props.setLong(key, newValue);
	}

	public void setBoolean(VariableKey<Boolean> booleanVariableKey, boolean newValue) {
		props.setBoolean(booleanVariableKey, newValue);
	}

	public Integer getInt(VariableKey<Integer> integerVariableKey, boolean defaultValue) {
		Long aLong = props.getLong(integerVariableKey, defaultValue);
		return aLong == null ? null : aLong.intValue();
	}

	public String getString(VariableKey<String> key, boolean defaults) {
		return props.getString(key, defaults);
	}

	public void setString(VariableKey<String> key, String s) {
		props.setString(key, s);
	}

	public Color getColorNullIfInvalid(VariableKey<Color> colorVariableKey, boolean defaults) {
		return props.getColorNullIfInvalid(colorVariableKey, defaults);
	}

	public void setIntegerArray(VariableKey<Integer[]> variableKey, Integer[] ordering) {
		props.setIntegerArray(variableKey, ordering);
	}

	public Integer[] getIntegerArray(VariableKey<Integer[]> variableKey, boolean b) {
		return props.getIntegerArray(variableKey, b);
	}

	public void setDimension(VariableKey<Dimension> ircFrameDimension, Dimension size) {
		props.setDimension(ircFrameDimension, size);
	}

	public void setPoint(VariableKey<Point> ircFrameLocation, Point location) {
		props.setPoint(ircFrameLocation, location);
	}

	public void setStringArray(VariableKey<List<String>> valuesKey, List<?> items) {
		props.setStringArray(valuesKey, items);
	}

	public Font getFont(VariableKey<Font> scrambleFont, boolean b) {
		return props.getFont(scrambleFont, b);
	}

	public Color getColor(VariableKey<Color> timerFg, boolean b) {
		return props.getColor(timerFg, b);
	}

	public Dimension getDimension(VariableKey<Dimension> ircFrameDimension, boolean b) {
		return props.getDimension(ircFrameDimension);
	}

	public float getFloat(VariableKey<Float> opacity, boolean b) {
		return props.getFloat(opacity, b);
	}

	public Point getPoint(VariableKey<Point> ircFrameLocation, boolean b) {
		return props.getPoint(ircFrameLocation, b);
	}

	public void setColor(VariableKey<Color> currentAverage, Color background) {
		props.setColor(currentAverage, background);
	}

	public void setFont(VariableKey<Font> timerFont, Font font) {
		props.setFont(timerFont, font);
	}

	public void setDouble(VariableKey<Double> minSplitDifference, Double value) {
		props.setDouble(minSplitDifference, value);
	}

	public void setFloat(VariableKey<Float> opacity, float v) {
		props.setFloat(opacity, v);
	}

	public Double getDouble(VariableKey<Double> key, boolean defaultValue) {
		return props.getDouble(key, defaultValue);
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
		props.setString(VariableKey.LANGUAGE, l.getLanguage());
		props.setString(VariableKey.REGION, l.getCountry());
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
