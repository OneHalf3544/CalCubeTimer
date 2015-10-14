package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.tuple.Tuple2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ScramblePluginManager {

	private static final Logger LOG = LogManager.getLogger(ScramblePluginManager.class);

	public static final ScramblePlugin NULL_SCRAMBLE_PLUGIN = new NullScramblePlugin();
	public static final String NULL_SCRAMBLE_VARIATION_NAME = "NullScrambleVariation";

	public final PuzzleType NULL_PUZZLE_TYPE;

	private Map<PuzzleType, ScrambleSettings> scrambleVariations;

	public final ScrambleString NULL_IMPORTED_SCRUMBLE;

	private final Configuration configuration;
	private final Map<Class<? extends ScramblePlugin>, ScramblePlugin> scramblePlugins;

	private List<String> attributes;
	public final ScrambleSettings nullScrambleSettings;

	public void setEnabledPuzzleAttributes(List<String> attributes) {
		this.setAttributes(attributes);
	}

	private final List<ScramblePlugin> plugins;

	@Inject
	private SolutionDao solutionDao;

	@Inject
	public ScramblePluginManager(Configuration configuration) throws IllegalArgumentException,
														       		 IllegalAccessException, InstantiationException {
		this.configuration = configuration;

		nullScrambleSettings = new ScrambleSettings(this.configuration, this, "", 0, null);
		NULL_PUZZLE_TYPE = new PuzzleType(configuration, null, this, NULL_SCRAMBLE_PLUGIN);

		plugins = ImmutableList.copyOf(ServiceLoader.load(ScramblePlugin.class));
		this.scramblePlugins = createScramblePlugins();
		LOG.info("loaded plugins: {}", plugins);
		NULL_IMPORTED_SCRUMBLE = new ScrambleString(NULL_PUZZLE_TYPE, "", true, null, NULL_SCRAMBLE_PLUGIN, null);
	}

	public Map<Class<? extends ScramblePlugin>, ScramblePlugin> createScramblePlugins() throws IllegalAccessException, InstantiationException {
		Map<Class<? extends ScramblePlugin>, ScramblePlugin> scramblePlugin = new HashMap<>();
		for (ScramblePlugin plugin : plugins) {
			plugin.checkPluginState();
			scramblePlugin.put(plugin.getClass(), plugin);
		}
		return scramblePlugin;
	}

	public void saveLengthsToConfiguration() {
		for(PuzzleType puzzleType : getScrambleVariations().keySet()) {
			configuration.setLong(VariableKey.scrambleLength(puzzleType.getVariationName()), puzzleType.getScrambleVariation().getLength());
		}
	}

	public void reloadLengthsFromConfiguration(boolean defaults) {
		for(Map.Entry<PuzzleType, ScrambleSettings> v : getScrambleVariations().entrySet()) {
			v.getValue().setLength(ScrambleSettings.getScrambleLength(
					v.getKey().getScramblePlugin(), v.getKey().getVariationName(), configuration, defaults));
		}
	}

	public Map<PuzzleType, ScrambleSettings> getScrambleVariations() {
		if (scrambleVariations == null) {
			this.scrambleVariations = calculateScrambleVariations();
		}
		return scrambleVariations;
	}

	private Map<PuzzleType, ScrambleSettings> calculateScrambleVariations() {
		Map<PuzzleType, ScrambleSettings> variations = new HashMap<>();

		for(ScramblePlugin plugin : scramblePlugins.values()) {
            for(String variationName : plugin.getVariations()) {
				PuzzleType puzzleType = new PuzzleType(configuration, "", this, plugin);
				puzzleType.setVariationName(variationName);

				ScrambleSettings scrambleSettings = ScrambleSettings.createScrambleVariation(
						plugin, variationName, configuration, this, getDefaultGeneratorGroup(variationName, plugin))
						.withGeneratorGroup(loadGeneratorFromConfig(puzzleType, false));

				variations.put(puzzleType, scrambleSettings);
            }
        }
		if(variations.isEmpty()) {
            throw new IllegalStateException("no plugins available");
        }
		return variations;
	}

	public Tuple2<PuzzleType, ScrambleSettings> getBestMatchPuzzleType(String variation) {
		if(variation == null) {
			return null;
		}
		for(Map.Entry<PuzzleType, ScrambleSettings> var : getScrambleVariations().entrySet()) {
			if (var.getKey().getVariationName().toLowerCase().startsWith(variation.toLowerCase())) {
				return new Tuple2<>(var.getKey(), var.getValue());
			}
		}
		return null;
	}

	public PuzzleType getCurrentScrambleCustomization(Profile currentProfile) {
		String scName = configuration.getString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, false);
		PuzzleType sc = getPuzzleTypeByString(scName);

		//now we'll try to match the variation, if we couldn't match the customization
		if(sc == null && scName.indexOf(':') != -1) {
			scName = scName.substring(0, scName.indexOf(":"));
			sc = getPuzzleTypeByString(scName);
		}
		if(sc == null) {
			List<PuzzleType> puzzleTypes = getPuzzleTypes(currentProfile);
			if (puzzleTypes.size() > 0) {
				sc = puzzleTypes.get(0);
			}
		}
		return sc;
	}

	public PuzzleType getPuzzleTypeByVariation(PuzzleType scrambleSettings) {
		return getPuzzleTypeByString(scrambleSettings.getVariationName());
	}

	public PuzzleType getPuzzleTypeByString(String customName) {
		return getPuzzleTypes(null).stream()
				.filter(c -> c.toString().equals(customName))
				.findAny()
				.orElse(null);
	}

	public List<PuzzleType> getPuzzleTypes(@Nullable Profile selectedProfile) {
		List<PuzzleType> puzzleTypes = getPuzzleTypes();

		if (selectedProfile != null) {
			List<String> usedPuzzleTypes = solutionDao.getUsedPuzzleTypes(selectedProfile);
			usedPuzzleTypes.removeAll(puzzleTypes.stream()
                    .map(PuzzleType::getVariationName)
                    .collect(Collectors.toList()));

			LOG.warn("unsupported puzzleTypes: {}", usedPuzzleTypes);
		}

		return puzzleTypes;
	}

	public List<PuzzleType> getPuzzleTypes() {
		SortedSet<PuzzleType> elements = new TreeSet<>(Comparator.comparing(PuzzleType::getVariationName));
		elements.addAll(getScrambleVariations().keySet());
		return ImmutableList.copyOf(elements);
	}

	private PuzzleType searchPuzzleByName(List<PuzzleType> customizations, String variationName) {
		return customizations.stream()
				.filter(variationName::equals)
				.findAny()
				.orElse(NULL_PUZZLE_TYPE);
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

	public String getDefaultGeneratorGroup(ScramblePlugin scramblePlugin, String variationName) {
		return getDefaultGeneratorGroup(variationName, scramblePlugin);
	}
	
	public String getDefaultGeneratorGroup(String variationName, ScramblePlugin plugin) {
		return plugin.getDefaultGenerators().get(variationName);
	}

	public boolean isGeneratorEnabled(@NotNull PuzzleType puzzleType) {
		return puzzleType.getScramblePlugin().getDefaultGenerators().containsKey(puzzleType.getVariationName());
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
		Integer variationsCount = getScrambleVariations().size();
		return String.format("ScramblePluginManager{has %d plugins (%d variations)}", plugins.size(), variationsCount);
	}

	@Nullable
	public String loadGeneratorFromConfig(@NotNull PuzzleType puzzleType, boolean defaults) {
		if (!isGeneratorEnabled(puzzleType)) {
			return null;
		}
		String generatorConfig = configuration.getNullableString(VariableKey.scrambleGeneratorKey(puzzleType), defaults);
		return generatorConfig == null ? getDefaultGeneratorGroup(puzzleType.getScramblePlugin(), puzzleType.getVariationName()) : generatorConfig;
	}

	// todo scramble generator not save without configuration dialog
	public void saveGeneratorToConfiguration(PuzzleType puzzleType) {
		if(isGeneratorEnabled(puzzleType)) {
			configuration.setString(
					VariableKey.scrambleGeneratorKey(puzzleType),
					puzzleType.getScrambleVariation().getGeneratorGroup() == null ? "" : puzzleType.getScrambleVariation().getGeneratorGroup());
		}
	}

	@NotNull
	public ScrambleSettings getScrambleVariation(PuzzleType puzzleType) {
		return getScrambleVariations().get(puzzleType);
	}

	public void setScrambleSettings(PuzzleType puzzleType, ScrambleSettings scrambleSettings) {
		getScrambleVariations().put(puzzleType, scrambleSettings);
	}
}
