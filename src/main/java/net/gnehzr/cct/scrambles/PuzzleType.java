package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.RollingAverageOf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;

public class PuzzleType {

	private static final Logger LOG = LogManager.getLogger(PuzzleType.class);

	private final Configuration configuration;
	public final ScramblePluginManager scramblePluginManager;
	private final ScramblePlugin plugin;
	private String customization;
	private String variationName;

	public PuzzleType(Configuration configuration,
					  String customization, ScramblePluginManager scramblePluginManager, ScramblePlugin plugin) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.customization = customization;
		this.plugin = plugin;

	}

	public ScrambleString generateScramble() {
		return generateScramble(scramblePluginManager.getScrambleVariation(this));
	}

	public ScrambleString generateScramble(ScrambleSettings scrambleSettings) {
		checkState(!isNullType());

		ScramblePlugin scramblePlugin = this.getScramblePlugin();
		ScrambleString newScramble = scramblePlugin.createScramble(
				this,
				scrambleSettings.withGeneratorGroup(scramblePluginManager.getDefaultGeneratorGroup(scramblePlugin, getVariationName())),
				scramblePlugin.getEnabledPuzzleAttributes(scramblePluginManager, configuration));
		LOG.info("generated scramble: {}, for puzzle '{}'", newScramble, this);
		return newScramble;
	}

	public ScrambleString importScramble(String scramble) throws InvalidScrambleException {
		ScrambleSettings scrambleSettings = scramblePluginManager.getScrambleVariation(this);
		return getScramblePlugin().importScramble(
				this,
				scrambleSettings.withoutLength(),
				scramble,
				getScramblePlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public int getPuzzleUnitSize(ScramblePlugin scramblePlugin, boolean defaults) {
		Integer unitSize = configuration.getInt(VariableKey.UNIT_SIZE(this), defaults);
		return unitSize  != null ? unitSize : scramblePlugin.getDefaultUnitSize();
	}

	public void setPuzzleUnitSize(int size) {
		if(!this.isNullType()) {
			configuration.setLong(VariableKey.UNIT_SIZE(this), size);
		}
	}

	public void setRA(RollingAverageOf index, int newra, boolean trimmed) {
		configuration.setLong(VariableKey.RA_SIZE(index, this), newra);
		configuration.setBoolean(VariableKey.RA_TRIMMED(index, this), trimmed);
	}

	public int getRASize(RollingAverageOf index) {
		Integer size = configuration.getInt(VariableKey.RA_SIZE(index, this));
		if(size == null) {
			size = Objects.requireNonNull(configuration.getInt(VariableKey.RA_SIZE(index, null)));
		}
		return size;
	}
	public boolean isTrimmed(RollingAverageOf index) {
		VariableKey<Boolean> key = VariableKey.RA_TRIMMED(index, this);
		if(!configuration.keyExists(key))
			key = VariableKey.RA_TRIMMED(index, null);
		return configuration.getBoolean(key, false);
	}

	public void setCustomization(String custom) {
		customization = custom;
	}

	public ScramblePlugin getScramblePlugin() {
		return plugin;
	}

	@NotNull
	@Deprecated // use scramblePluginManager directly
	public ScrambleSettings getScrambleVariation() {
		return scramblePluginManager.getScrambleVariation(this);
	}

	public String getCustomization() {
		return customization;
	}

	public String toString() {
		if(Utils.notEmpty(customization)) {
			return plugin.getPuzzleName() + ":" + customization;
		} else {
			return plugin.getPuzzleName();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(plugin, customization, variationName);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		PuzzleType other = (PuzzleType) o;
		return Objects.equals(plugin, other.plugin)
				&& Objects.equals(customization, other.customization)
				&& Objects.equals(variationName, other.variationName);
	}

	public boolean isNullType() {
		return this == scramblePluginManager.NULL_PUZZLE_TYPE;
	}

	public String getVariationName() {
		return variationName;
	}

	public void setVariationName(String variationName) {
		this.variationName = variationName;
	}

}
