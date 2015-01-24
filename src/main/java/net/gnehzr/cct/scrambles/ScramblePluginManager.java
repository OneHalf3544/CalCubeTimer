package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;
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

	public final ScrambleCustomization NULL_SCRAMBLE_CUSTOMIZATION;

	private final Configuration configuration;
	private final Map<Class<? extends Scramble>, Scramble> scramblePlugins;

	private List<String> attributes;

	public void setEnabledPuzzleAttributes(List<String> attributes) {
		this.setAttributes(attributes);
	}

	private List<Class<? extends Scramble>> pluginClasses = ImmutableList.of(
			// todo load class names from /META-INF/somefile
			CubeScramble.class,
			ClockScramble.class,
			MegaminxScramble.class,
			PyraminxScramble.class,
			SquareOneScramble.class
	);


	@Inject
	public ScramblePluginManager(Configuration configuration) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {

		this.configuration = configuration;
		this.scramblePlugins = createScramblePlugins();

		NULL_SCRAMBLE_CUSTOMIZATION = new ScrambleCustomization(configuration,
				new ScrambleVariation(Scramble.NULL_SCRAMBLE, "", this.configuration, this), null, this);
	}

	public Map<Class<? extends Scramble>, Scramble> createScramblePlugins() throws IllegalAccessException, InstantiationException {
		Map<Class<? extends Scramble>, Scramble> scramblePlugin = new HashMap<>();
		for (Class<? extends Scramble> pluginClass : pluginClasses) {
			Scramble plugin = pluginClass.newInstance();
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
			v.setLength(v.getScrambleLength(defaults));
		}
	}
	private ScrambleVariation[] scrambleVariations;

	public ScrambleVariation[] getScrambleVariations() {
		if(scrambleVariations == null) {
			List<ScrambleVariation> vars = new ArrayList<>();
			for(Scramble p : scramblePlugins.values()) {
				for(String var : p.getVariations()) {
					vars.add(new ScrambleVariation(p, var, configuration, this));
				}
			}
			if(vars.isEmpty()) {
				vars.add(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation());
			}
			scrambleVariations = vars.toArray(new ScrambleVariation[vars.size()]);
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
		if(sv == null)
			return null;
		return getCustomizationFromString(profile, sv.toString());
	}

	public ScrambleCustomization getCustomizationFromString(Profile profile, String customName) {
		List<ScrambleCustomization> scrambleCustomizations = getScrambleCustomizations(profile, false);
		for(ScrambleCustomization custom : scrambleCustomizations)
			if(custom.toString().equals(customName))
				return custom;

		return null;
	}

	public List<ScrambleCustomization> getScrambleCustomizations(Profile selectedProfile, boolean defaults) {
		ArrayList<ScrambleCustomization> scrambleCustomizations = new ArrayList<>();
		for(ScrambleVariation t : getScrambleVariations())
			scrambleCustomizations.add(new ScrambleCustomization(configuration, t, null, this));
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
				if(ch < 0)
					break;
				name = customNames.get(ch--);
			}
			int delimeter = name.indexOf(':');
			String customizationName;
			if(delimeter == -1) {
				delimeter = name.length();
				customizationName = null;
			} else
				customizationName = name.substring(delimeter + 1, name.length());
			String variationName = name.substring(0, delimeter);
			ScrambleCustomization scramCustomization = null;
			for(ScrambleCustomization custom : scrambleCustomizations) {
				if(variationName.equals(custom.toString())) {
					scramCustomization = custom;
					break;
				}
			}
			ScrambleCustomization sc;
			if(scramCustomization != null)
				sc = new ScrambleCustomization(configuration, scramCustomization.getScrambleVariation(), customizationName, this);
			else if(variationName.equals(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation().toString()))
				sc = new ScrambleCustomization(configuration, NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation(), customizationName, this);
			else
				sc = new ScrambleCustomization(configuration, new ScrambleVariation(Scramble.NULL_SCRAMBLE, variationName, configuration, this), customizationName, this);
			if(!variationName.isEmpty()) {
				if(scrambleCustomizations.contains(sc)) {
					if(ch == customNames.size() - 1) //we don't want to move this customization to the front of the list if it's from the database
						continue;
					scrambleCustomizations.remove(sc);
				}
				scrambleCustomizations.add(0, sc);
			}
		}
		return scrambleCustomizations;
	}

	public List<String> getAvailablePuzzleAttributes(Class<? extends Scramble> aClass) {
		return getScramblePlugin(aClass).getAttributes();
	}

	public Scramble getScramblePlugin(Class<? extends Scramble> aClass) {
		return scramblePlugins.get(aClass);
	}

	public BufferedImage getScrambleImage(final Scramble instance, final int gap, final int unitSize, final Color[] colorScheme) {
		return instance.getScrambleImage(gap, Math.max(unitSize, instance.getDefaultUnitSize()), colorScheme);
	}

	public String getDefaultGeneratorGroup(ScrambleVariation var) {
		int c = getIndexOfVariation(var);
		if(c == -1 || var.getPlugin().getDefaultGenerators() == null) {
			return null;
		}
		return var.getPlugin().getDefaultGenerators()[c];
	}
	
	public boolean isGeneratorEnabled(Scramble scramble) {
		return scramble.getDefaultGenerators() != null;
	}
	
	private int getIndexOfVariation(ScrambleVariation var) {
		for(int c = 0; c < var.getPlugin().getVariations().length; c++) {
			if (var.getPlugin().getVariations()[c].equals(var.getVariation())) {
				return c;
			}
		}
		return -1;
	}

	public Color[] getColorScheme(Scramble scramblePlugin, boolean defaults) {
		if(scramblePlugin.getFaceNamesColors() == null) {
			//this is for null scrambles
			return null;
		}
		Color[] scheme = new Color[scramblePlugin.getFaceNamesColors()[0].length];
		for(int face = 0; face < scheme.length; face++) {
			scheme[face] = configuration.getColorNullIfInvalid(VariableKey.PUZZLE_COLOR(scramblePlugin, scramblePlugin.getFaceNamesColors()[0][face]), defaults);
			if(scheme[face] == null)
				scheme[face] = Utils.stringToColor(scramblePlugin.getFaceNamesColors()[1][face], false);
		}
		return scheme;
	}

	public Collection<Scramble> getScramblePlugins() {
		return scramblePlugins.values();
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
}
