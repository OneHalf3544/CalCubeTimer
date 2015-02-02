package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.Profile;
import org.apache.log4j.Logger;
import scramblePlugins.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

@Singleton
public class ScramblePluginManager {

	private static final Logger LOG = Logger.getLogger(ScramblePluginManager.class);

	public static final ScramblePlugin NULL_SCRAMBLE_PLUGIN = new NullScramblePlugin();

	public final ScrambleCustomization NULL_SCRAMBLE_CUSTOMIZATION;

	private ScrambleVariation[] scrambleVariations;

	public static final ScrambleString NULL_IMPORTED_SCRUMBLE = new ScrambleString("", true, null, NULL_SCRAMBLE_PLUGIN, null);
	public static final ScrambleString NULL_CREATED_SCRAMBLE = new ScrambleString("", false, null, NULL_SCRAMBLE_PLUGIN, null);


	private final Configuration configuration;
	private final Map<Class<? extends ScramblePlugin>, ScramblePlugin> scramblePlugins;

	private List<String> attributes;

	public void setEnabledPuzzleAttributes(List<String> attributes) {
		this.setAttributes(attributes);
	}

	private List<Class<? extends ScramblePlugin>> pluginClasses = ImmutableList.of(
			// todo load class names from /META-INF/somefile
			Cube2x2ScramblePlugin.class,
			CubeScramblePlugin.class,
			ClockScramblePlugin.class,
			MegaminxScramblePlugin.class,
			PyraminxScramblePlugin.class,
			SquareOneScramblePlugin.class
	);

	@Inject
	public ScramblePluginManager(Configuration configuration) throws IllegalArgumentException,
														       		 IllegalAccessException, InstantiationException {
		this.configuration = configuration;
		this.scramblePlugins = createScramblePlugins();

		ScrambleVariation NULL_SCRAMBLE_VARIATION = new ScrambleVariation(NULL_SCRAMBLE_PLUGIN, "", this.configuration, this);
		NULL_SCRAMBLE_CUSTOMIZATION = new ScrambleCustomization(configuration, NULL_SCRAMBLE_VARIATION, null, this);
	}

	public Map<Class<? extends ScramblePlugin>, ScramblePlugin> createScramblePlugins() throws IllegalAccessException, InstantiationException {
		Map<Class<? extends ScramblePlugin>, ScramblePlugin> scramblePlugin = new HashMap<>();
		for (Class<? extends ScramblePlugin> pluginClass : pluginClasses) {
			ScramblePlugin plugin = pluginClass.newInstance();
			plugin.checkPluginState();
			scramblePlugin.put(pluginClass, plugin);
		}
		return scramblePlugin;
	}

	//this has the potential to break a lot of things in cct,
	//it's only used by cctbot right now
	public void clearScramblePlugins() {
		scrambleVariations = null;
	}

	public void saveLengthsToConfiguration() {
		for(ScrambleVariation variation : getScrambleVariations()) {
			configuration.setLong(VariableKey.SCRAMBLE_LENGTH(variation), variation.getLength());
		}
	}

	public void reloadLengthsFromConfiguration(boolean defaults) {
		for(ScrambleVariation v : getScrambleVariations()) {
			v.setLength(v.getScrambleLength(v.getName(), defaults));
		}
	}

	public ScrambleVariation[] getScrambleVariations() {
		if(scrambleVariations == null) {
			List<ScrambleVariation> variations = new ArrayList<>();
			for(ScramblePlugin plugin : scramblePlugins.values()) {
				for(String variationName : plugin.getVariations()) {
					variations.add(new ScrambleVariation(plugin, variationName, configuration, this));
				}
			}
			if(variations.isEmpty()) {
				variations.add(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation());
			}
			scrambleVariations = variations.toArray(new ScrambleVariation[variations.size()]);
		}
		return scrambleVariations;
	}

	public ScrambleVariation getBestMatchVariation(String variation) {
		if(variation == null) {
			return null;
		}
		for(ScrambleVariation var : getScrambleVariations()) {
			if (var.toString().toLowerCase().startsWith(variation)) {
				return var;
			}
		}
		return null;
	}

	public ScrambleCustomization getCurrentScrambleCustomization(Profile currentProfile) {
		String scName = configuration.getString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, false);
		ScrambleCustomization sc = getCustomizationFromString(currentProfile, scName);

		//now we'll try to match the variation, if we couldn't match the customization
		if(sc == null && scName.indexOf(':') != -1) {
			scName = scName.substring(0, scName.indexOf(":"));
			sc = getCustomizationFromString(currentProfile, scName);
		}
		if(sc == null) {
			List<ScrambleCustomization> scs = getScrambleCustomizations(currentProfile, false);
			if(scs.size() > 0)
				sc = scs.get(0);
		}
		return sc;
	}

	public ScrambleCustomization getCustomizationFromVariation(ScrambleVariation sv, Profile profile) {
		if(sv == null) {
			return null;
		}
		return getCustomizationFromString(profile, sv.toString());
	}

	public ScrambleCustomization getCustomizationFromString(Profile profile, String customName) {
		return getScrambleCustomizations(profile, false).stream()
				.filter(c -> c.toString().equals(customName))
				.findAny()
				.orElse(null);
	}

	public List<ScrambleCustomization> getScrambleCustomizations(Profile selectedProfile, boolean defaults) {
		ArrayList<ScrambleCustomization> scrambleCustomizations = new ArrayList<>();
		for(ScrambleVariation variation : getScrambleVariations()) {
			scrambleCustomizations.add(new ScrambleCustomization(configuration, variation, null, this));
		}
		List<String> customNames = configuration.getStringArray(VariableKey.SCRAMBLE_CUSTOMIZATIONS, defaults);
		if(customNames == null) {
			customNames = Lists.newArrayList();
		}
		Iterator<String> databaseCustoms = selectedProfile.getPuzzleDatabase().getCustomizations().iterator();
		int ch = customNames.size() - 1;
		while(true) {
			String name;
			if(databaseCustoms.hasNext()) {
				name = databaseCustoms.next();
			} else {
				if(ch < 0) {
					break;
				}
				name = customNames.get(ch--);
			}
			int delimeter = name.indexOf(':');
			String customizationName;
			if(delimeter == -1) {
				delimeter = name.length();
				customizationName = null;
			} else {
				customizationName = name.substring(delimeter + 1, name.length());
			}
			String variationName = name.substring(0, delimeter);

			ScrambleCustomization scramCustomization = searchCustomizationByName(scrambleCustomizations, variationName);
			ScrambleCustomization sc = new ScrambleCustomization(configuration, scramCustomization.getVariation(), customizationName, this);
			if (!variationName.isEmpty()) {
				if(scrambleCustomizations.contains(sc)) {
					if(ch == customNames.size() - 1) {
						//we don't want to move this customization to the front of the list if it's from the database
						continue;
					}
					scrambleCustomizations.remove(sc);
				}
				scrambleCustomizations.add(0, sc);
			}
		}
		return scrambleCustomizations;
	}

	private ScrambleCustomization searchCustomizationByName(List<ScrambleCustomization> customizations, String variationName) {
		return customizations.stream()
				.filter(variationName::equals)
				.findAny()
				.orElse(NULL_SCRAMBLE_CUSTOMIZATION);
	}

	public List<String> getAvailablePuzzleAttributes(Class<? extends ScramblePlugin> aClass) {
		return getScramblePlugin(aClass).getAttributes();
	}

	public ScramblePlugin getScramblePlugin(Class<? extends ScramblePlugin> aClass) {
		return scramblePlugins.get(aClass);
	}

	public BufferedImage getScrambleImage(final ScrambleString scramble, final int gap, final int unitSize, final Map<String, Color> colorScheme) {
		int finalUnitSize = Math.max(unitSize, scramble.getScramblePlugin().getDefaultUnitSize());
		return scramble.getScramblePlugin().getScrambleImage(scramble, gap, finalUnitSize, colorScheme);
	}

	public String getDefaultGeneratorGroup(ScrambleVariation var) {
		return var.getPlugin().getDefaultGenerators().get(var.getName());
	}
	
	public boolean isGeneratorEnabled(ScrambleVariation scrambleVariation) {
		return scrambleVariation.getPlugin().getDefaultGenerators().containsKey(scrambleVariation.getName());
	}

	public Map<String, Color> getColorScheme(ScramblePlugin scramblePluginPlugin, boolean defaults) {
		if(scramblePluginPlugin.getFaceNamesColors().isEmpty()) {
			return null;
		}
		Map<String, Color> scheme = new HashMap<>(scramblePluginPlugin.getFaceNamesColors().size());
		for(Map.Entry<String, Color> face : scramblePluginPlugin.getFaceNamesColors().entrySet()) {
			Color colorFromConfig = configuration.getColorNullIfInvalid(
					VariableKey.PUZZLE_COLOR(scramblePluginPlugin, face.getKey()), defaults);
			scheme.put(face.getKey(), colorFromConfig == null ? face.getValue() : colorFromConfig);
		}
		return scheme;
	}

	public Collection<ScramblePlugin> getScramblePlugins() {
		return scramblePlugins.values();
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		Integer variationsCount = pluginClasses.stream()
				.map(this::getScramblePlugin)
				.map(scramble -> getScrambleVariations().length)
				.reduce((a, b) -> a + b)
				.orElse(0);
		return String.format("ScramblePluginManager{has %d plugins (%d variations)}", pluginClasses.size(), variationsCount);
	}

	public String loadGeneratorFromConfig(ScrambleCustomization scrambleCustomization, boolean defaults) {
		if (!isGeneratorEnabled(scrambleCustomization.getScrambleVariation())) {
			return null;
		}
		String generatorConfig = configuration.getNullableString(VariableKey.scrambleGeneratorKey(scrambleCustomization), defaults);
		return generatorConfig == null ? getDefaultGeneratorGroup(scrambleCustomization.getVariation()) : generatorConfig;
	}

	// todo scramble generator not save without configuration dialog
	public void saveGeneratorToConfiguration(ScrambleCustomization scrambleCustomization) {
		if(isGeneratorEnabled(scrambleCustomization.getScrambleVariation())) {
			configuration.setString(
					VariableKey.scrambleGeneratorKey(scrambleCustomization),
					scrambleCustomization.getGenerator() == null ? "" : scrambleCustomization.getGenerator());
		}
	}
}
