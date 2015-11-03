package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class PuzzleType {

	private static final Logger LOG = LogManager.getLogger(PuzzleType.class);

	private final Configuration configuration;
	public final ScramblePluginManager scramblePluginManager;
	private final ScramblePlugin plugin;
	private final String variationName;

	public PuzzleType(Configuration configuration, ScramblePluginManager scramblePluginManager,
					  ScramblePlugin plugin, String variationName) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.plugin = plugin;
		this.variationName = variationName;
	}

	public ScrambleString generateScramble() {
		return generateScramble(scramblePluginManager.getScrambleVariation(this));
	}

	public ScrambleString generateScramble(ScrambleSettings scrambleSettings) {
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
		configuration.setLong(VariableKey.UNIT_SIZE(this), size);
	}

	public void setRA(RollingAverageOf index, int newra, boolean trimmed) {
		configuration.setLong(VariableKey.RA_SIZE(index, this), newra);
		configuration.setBoolean(VariableKey.RA_TRIMMED(index, this), trimmed);
	}

	public int getRASize(RollingAverageOf index) {
		Integer size = configuration.getInt(VariableKey.RA_SIZE(index, this));
		if(size == null) {
			size = Objects.requireNonNull(configuration.getInt(VariableKey.defaultRaSize(index)));
		}
		return size;
	}

	public boolean isTrimmed(RollingAverageOf index) {
		VariableKey<Boolean> key = VariableKey.RA_TRIMMED(index, this);
		if(!configuration.keyExists(key))
			key = VariableKey.RA_TRIMMED(index, null);
		return configuration.getBoolean(key, false);
	}

	public ScramblePlugin getScramblePlugin() {
		return plugin;
	}

	public String toString() {
		return getVariationName();
	}

	@Override
	public int hashCode() {
		return Objects.hash(plugin, variationName);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		PuzzleType other = (PuzzleType) o;
		return Objects.equals(plugin, other.plugin)
				&& Objects.equals(variationName, other.variationName);
	}

	public String getVariationName() {
		return variationName;
	}
}
