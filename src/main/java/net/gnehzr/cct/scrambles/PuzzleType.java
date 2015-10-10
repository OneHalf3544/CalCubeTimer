package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PuzzleType {

	private static final Logger LOG = LogManager.getLogger(PuzzleType.class);

	private final Configuration configuration;
	@NotNull
	private ScrambleVariation variation;
	public final ScramblePluginManager scramblePluginManager;
	private String customization;

	public PuzzleType(Configuration configuration, @NotNull ScrambleVariation variation,
					  String customization, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.customization = customization;

		// todo hack to avoid NPE in PuzzleType.toString() in loadFromConfig:
		this.variation = variation;
		this.variation = variation.withGeneratorGroup(scramblePluginManager.loadGeneratorFromConfig(this, false, variation));
	}

	public ScrambleString generateScramble(ScrambleVariation variation) {
		ScrambleString newScramblePlugin = variation.getPlugin().createScramble(
				variation,
				variation.getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
		LOG.info("generated scramble: " + newScramblePlugin + ", for plugin '" + this.variation.getPlugin().getPuzzleName() + "', variation: " + this.variation);
		return newScramblePlugin;
	}

	public ScrambleString importScramble(String scramble) throws InvalidScrambleException {
		return variation.getPlugin().importScramble(variation.withoutLength(), scramble,
				variation.getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public void setRA(int index, int newra, boolean trimmed) {
		configuration.setLong(VariableKey.RA_SIZE(index, this), newra);
		configuration.setBoolean(VariableKey.RA_TRIMMED(index, this), trimmed);
	}

	public int getRASize(int index) {
		Integer size = configuration.getInt(VariableKey.RA_SIZE(index, this));
		if(size == null || size <= 0) {
			size = configuration.getInt(VariableKey.RA_SIZE(index, null));
		}
		return size;
	}
	public boolean isTrimmed(int index) {
		VariableKey<Boolean> key = VariableKey.RA_TRIMMED(index, this);
		if(!configuration.keyExists(key))
			key = VariableKey.RA_TRIMMED(index, null);
		return configuration.getBoolean(key, false);
	}

	public void setScrambleVariation(@NotNull ScrambleVariation newVariation) {
		variation = newVariation;
	}

	public void setCustomization(String custom) {
		customization = custom;
	}

	public ScramblePlugin getScramblePlugin() {
		return variation.getPlugin();
	}

	@NotNull
	public ScrambleVariation getScrambleVariation() {
		return variation;
	}

	public String getCustomization() {
		return customization;
	}

	public String toString() {
		String temp = variation.getName();
		if(temp.isEmpty()) {
			temp += variation.getPlugin().getPuzzleName();
		}
		if(customization != null) {
			temp += ":" + customization;
		}
		return temp;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
 		return this.toString().equals(o.toString());
	}

	public boolean isNullType() {
		return this == scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
	}
}
