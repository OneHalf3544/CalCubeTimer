package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class ScrambleSettings {

	private static final Logger LOG = LogManager.getLogger(ScrambleSettings.class);

	public static ScrambleSettings createScrambleVariation(ScramblePlugin scramblePlugin, String variationName,
															Configuration configuration,
															ScramblePluginManager scramblePluginManager,
															String generatorGroup) {
		int length = getScrambleLength(scramblePlugin, variationName, configuration, false);
		return new ScrambleSettings(configuration, scramblePluginManager, generatorGroup, length);
	}

	public interface WithoutLength {
		ScrambleSettings withLength(int length);
		String getGeneratorGroup();
	}

	private int length = 0;
	private String generatorGroup;
	private final Configuration configuration;

	private final ScramblePluginManager scramblePluginManager;

	public ScrambleSettings(Configuration configuration,
							ScramblePluginManager scramblePluginManager, String generatorGroup,
							int length) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.generatorGroup = generatorGroup;
		this.length = length;
	}

	public ScrambleSettings withGeneratorGroup(String generatorGroup) {
		return new ScrambleSettings(configuration, scramblePluginManager, generatorGroup, length);
	}

	public WithoutLength withoutLength() {
		return new ScrambleVariationWithoutLength(this, configuration, scramblePluginManager);
	}

	public String getGeneratorGroup() {
		return generatorGroup;
	}

	public static int getScrambleLength(ScramblePlugin scramblePlugin, String variationName, Configuration configuration, boolean defaultValue) {
		if (scramblePlugin == ScramblePluginManager.NULL_SCRAMBLE_PLUGIN) {
			return 0;
		}
		Integer length = configuration.getInt(VariableKey.scrambleLength(variationName), defaultValue);
		int i = scramblePlugin.getVariations().indexOf(variationName);
		if (i == -1) {
			i = 0;
			LOG.warn("wrong variationName: '{}'", variationName);
		}
		return length == null ? scramblePlugin.getDefaultLengths()[i] : length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setGeneratorGroup(String generatorGroup) {
		this.generatorGroup = generatorGroup;
	}

	public ScrambleSettings withLength(int length) {
		return new ScrambleSettings(
				configuration, scramblePluginManager, generatorGroup, length);
	}

	public int getLength() {
		return length;
	}

	public int hashCode() {
		return Objects.hash(generatorGroup, getLength());
	}

	public boolean equals(Object o) {
		if (!(o instanceof ScrambleSettings)) {
			return false;
		}
		ScrambleSettings other = (ScrambleSettings) o;
		return this.length == other.length
                && Objects.equals(this.generatorGroup, other.generatorGroup);
	}

	@Override
	public String toString() {
		return "ScrambleSettings{" +
				"scrambleLength=" + length +
				", generatorGroup='" + generatorGroup + '\'' +
				'}';
	}

}
