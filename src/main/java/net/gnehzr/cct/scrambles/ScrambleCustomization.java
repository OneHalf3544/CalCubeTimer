package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScrambleCustomization {

	private static final Logger LOG = LogManager.getLogger(ScrambleCustomization.class);

	private final Configuration configuration;
	private ScrambleVariation variation;
	public final ScramblePluginManager scramblePluginManager;
	private String customization;
	private String generator;

	public ScrambleCustomization(Configuration configuration, ScrambleVariation variation, String customization, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.variation = variation;
		this.scramblePluginManager = scramblePluginManager;
		this.customization = customization;
		this.setGenerator(scramblePluginManager.loadGeneratorFromConfig(this, false));
	}

	public ScrambleString generateScramble() {
		ScrambleString newScramblePlugin = variation.getPlugin().createScramble(variation, generator, variation.getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
		LOG.info("generated scramble: " + newScramblePlugin + ", for plugin '" + variation.getPlugin().getPuzzleName() + "', variation: " + variation);
		return newScramblePlugin;
	}

	public ScrambleString importScramble(String scramble) throws InvalidScrambleException {
		return variation.getPlugin().importScramble(variation.withoutLength(), scramble, generator,
				variation.getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public void setRA(int index, int newra, boolean trimmed) {
		configuration.setLong(VariableKey.RA_SIZE(index, this), newra);
		configuration.setBoolean(VariableKey.RA_TRIMMED(index, this), trimmed);
	}

	public int getRASize(int index) {
		Integer size = configuration.getInt(VariableKey.RA_SIZE(index, this), false);
		if(size == null || size <= 0)
			size = configuration.getInt(VariableKey.RA_SIZE(index, null), false);
		return size;
	}
	public boolean isTrimmed(int index) {
		VariableKey<Boolean> key = VariableKey.RA_TRIMMED(index, this);
		if(!configuration.keyExists(key))
			key = VariableKey.RA_TRIMMED(index, null);
		return configuration.getBoolean(key, false);
	}

	public ScrambleVariation getVariation() {
		return variation;
	}

	public void setScrambleVariation(ScrambleVariation newVariation) {
		variation = newVariation;
	}

	public void setCustomization(String custom) {
		customization = custom;
	}

	public void setGenerator(String generator) {
		this.generator = generator;
	}

	public String getGenerator() {
		return generator;
	}
	
	public ScramblePlugin getScramblePlugin() {
		return variation.getPlugin();
	}

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
}
