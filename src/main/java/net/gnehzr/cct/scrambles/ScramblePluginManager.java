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

	private Map<PuzzleType, ScrambleSettings> scrambleVariations;

	private final Configuration configuration;
	private final Map<Class<? extends ScramblePlugin>, ScramblePlugin> scramblePlugins;

	private List<String> attributes;

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

		plugins = ImmutableList.copyOf(ServiceLoader.load(ScramblePlugin.class));
		this.scramblePlugins = createScramblePlugins();
		this.scrambleVariations = calculateScrambleVariations();

		LOG.info("loaded plugins: {}", plugins);
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
			configuration.setLong(VariableKey.scrambleLength(puzzleType.getVariationName()), getScrambleVariation(puzzleType).getLength());
		}
	}

	public void reloadLengthsFromConfiguration(boolean defaults) {
		for(Map.Entry<PuzzleType, ScrambleSettings> v : getScrambleVariations().entrySet()) {
			ScrambleSettings scrambleSettings = v.getValue();
			PuzzleType puzzleType = v.getKey();

			scrambleSettings.setLength(ScrambleSettings.getScrambleLength(
					puzzleType.getScramblePlugin(), puzzleType.getVariationName(), configuration, defaults));
			scrambleSettings.setGeneratorGroup(loadGeneratorFromConfig(puzzleType, defaults));
		}
	}

	public Map<PuzzleType, ScrambleSettings> getScrambleVariations() {
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
						.withGeneratorGroup(getDefaultGeneratorGroup(plugin, variationName));

				variations.put(puzzleType, scrambleSettings);
            }
        }
		if(variations.isEmpty()) {
            throw new IllegalStateException("no plugins available");
        }
		return variations;
	}

	public PuzzleType getCurrentScrambleCustomization() {
		String scName = configuration.getString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, false);

		//now we'll try to match the variation, if we couldn't match the customization
		if(scName.contains(":")) {
			scName = scName.substring(0, scName.indexOf(":"));
			return getPuzzleTypeByString(scName);
		}
		return getPuzzleTypeByString(scName);
	}

	public PuzzleType getPuzzleTypeByVariation(PuzzleType scrambleSettings) {
		return getPuzzleTypeByString(scrambleSettings.getVariationName());
	}

	@NotNull
	public PuzzleType getPuzzleTypeByString(String variationName) {
		Optional<PuzzleType> result = getPuzzleTypes(null).stream()
				.filter(puzzleType -> puzzleType.getVariationName().equals(variationName))
				.findAny();
		return result.get();
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

	public List<String> getAvailablePuzzleAttributes(Class<? extends ScramblePlugin> aClass) {
		return getScramblePlugin(aClass).getAttributes();
	}

	public ScramblePlugin getScramblePlugin(Class<? extends ScramblePlugin> aClass) {
		return scramblePlugins.get(aClass);
	}

	public BufferedImage getScrambleImage(@NotNull final ScrambleString scramble, final int gap, final int unitSize,
										  @NotNull final Map<String, Color> colorScheme) {
		int finalUnitSize = Math.max(unitSize, scramble.getScramblePlugin().getDefaultUnitSize());
		return scramble.getScramblePlugin().getScrambleImage(scramble, gap, finalUnitSize, colorScheme);
	}

	public BufferedImage getDefaultStateImage(PuzzleType puzzleType, final int gap, final int unitSize,
											  @NotNull final Map<String, Color> colorScheme) {
		int finalUnitSize = Math.max(unitSize, puzzleType.getScramblePlugin().getDefaultUnitSize());
		return puzzleType.getScramblePlugin().getDefaultStateImage(puzzleType, gap, finalUnitSize, colorScheme);
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

	public Map<String, Color> getColorScheme(ScramblePlugin scramblePluginPlugin, boolean defaults, Configuration configuration) {
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
		if (isGeneratorEnabled(puzzleType)) {
			String generatorGroup = getScrambleVariation(puzzleType).getGeneratorGroup();
			configuration.setString(
					VariableKey.scrambleGeneratorKey(puzzleType),
					generatorGroup == null ? "" : generatorGroup);
		}
	}

	@NotNull
	public ScrambleSettings getScrambleVariation(@NotNull PuzzleType puzzleType) {
		return getScrambleVariations().get(puzzleType);
	}

	public void setScrambleSettings(PuzzleType puzzleType, ScrambleSettings scrambleSettings) {
		getScrambleVariations().put(puzzleType, scrambleSettings);
	}

	public PuzzleType getDefaultPuzzleType() {
		return getPuzzleTypeByString("3x3x3");
	}
}
