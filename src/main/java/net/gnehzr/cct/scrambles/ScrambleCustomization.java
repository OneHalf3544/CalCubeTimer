package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.Scramble.InvalidScrambleException;

public class ScrambleCustomization {
	private final Configuration configuration;
	private ScrambleVariation variation;
	private ScramblePlugin plugin;
	private String customization;
	private String generator;

	public ScrambleCustomization(Configuration configuration, ScrambleVariation variation, String customization) {
		this.configuration = configuration;
		this.variation = variation;
		this.plugin = variation.getScramblePlugin();
		this.customization = customization;
		loadGeneratorFromConfig(false);
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
		if(!configuration.props.keyExists(key))
			key = VariableKey.RA_TRIMMED(index, null);
		return configuration.getBoolean(key, false);
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
	private void loadGeneratorFromConfig(boolean defaults) {
		if(plugin.isGeneratorEnabled()) {
			generator = configuration.getString(VariableKey.SCRAMBLE_GENERATOR(this), defaults);
			if(generator == null)
				generator = plugin.getDefaultGeneratorGroup(variation);
		}
	}
	public void saveGeneratorToConfiguration() {
		if(plugin.isGeneratorEnabled())
			configuration.setString(VariableKey.SCRAMBLE_GENERATOR(this), generator == null ? "" : generator);
	}
	public String getGenerator() {
		return generator;
	}
	
	public ScramblePlugin getScramblePlugin() {
		return variation.getScramblePlugin();
	}

	public ScrambleVariation getScrambleVariation() {
		return variation;
	}

	public String getCustomization() {
		return customization;
	}

	public String toString() {
		String temp = variation.getVariation();
		if(temp.isEmpty())
			temp += variation.getScramblePlugin().getPuzzleName();
		if(customization != null)
			temp += ":" + customization;
		return temp;
	}
	public int hashCode() {
		return toString().hashCode();
	}
	public boolean equals(Object o) {
		if(o == null)
			return false;
		return this.toString().equals(o.toString());
	}

	public Scramble generateScramble() {
		return plugin.newScramble(variation.getVariation(), variation.getLength(), generator, plugin.getEnabledPuzzleAttributes());
	}

	public Scramble generateScramble(String scramble) throws InvalidScrambleException {
		return plugin.importScramble(variation.getVariation(), scramble, generator, plugin.getEnabledPuzzleAttributes());
	}
}
