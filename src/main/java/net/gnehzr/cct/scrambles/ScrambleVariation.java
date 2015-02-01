package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.ScramblePlugin.InvalidScrambleException;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.net.URL;

public class ScrambleVariation {

	private static final Logger LOG = Logger.getLogger(ScrambleVariation.class);

	public interface WithoutLength {
		ScrambleVariation withLength(int length);
		String getName();
	}

	private ScramblePlugin scramblePlugin;
	private String variationName;
	private int length = 0;
	private final Icon image;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;

	public ScrambleVariation(ScramblePlugin plugin, String variationName, Configuration configuration, ScramblePluginManager scramblePluginManager) {
		this.scramblePlugin = plugin;
		this.variationName = variationName;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		length = getScrambleLength(variationName, false);
		image = getImageIcon(variationName);
	}

	public WithoutLength withoutLength() {
		return new WithoutLength() {
			@Override
			public ScrambleVariation withLength(int length) {
				ScrambleVariation scrambleVariation = new ScrambleVariation(getPlugin(), variationName, configuration, scramblePluginManager);
				scrambleVariation.setLength(length);
				return scrambleVariation;
			}

			@Override
			public String getName() {
				return ScrambleVariation.this.getName();
			}
		};
	}

	private ImageIcon getImageIcon(String variation) {
		URL resource = scramblePlugin.getClass().getResource(ScramblePlugin.class.getSimpleName() + "_" + variation + ".png");
		return resource == null ? new ImageIcon() : new ImageIcon(resource);
	}

	public Icon getImage() {
		return image;
	}
	
	public int getScrambleLength(String variationName, boolean defaultValue) {
		if (scramblePlugin == ScramblePluginManager.NULL_SCRAMBLE_PLUGIN) {
			return 0;
		}
		Integer length = configuration.getInt(VariableKey.SCRAMBLE_LENGTH(this), defaultValue);
		int i = scramblePlugin.getVariations().indexOf(variationName);
		if (i == -1) {
			i = 0;
			LOG.warn(String.format("wrong variationName: '%s'", variationName));
		}
		return length == null ? scramblePlugin.getDefaultLengths()[i] : length;
	}

	public String getName() {
		return variationName;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public ScrambleString generateScramble() {
		return scramblePlugin.newScramble(this, scramblePluginManager.getDefaultGeneratorGroup(this), getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public ScrambleString generateScramble(String scramble) throws InvalidScrambleException {
		return scramblePlugin.importScramble(
				this.withoutLength(), scramble, scramblePluginManager.getDefaultGeneratorGroup(this),
				getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public int getPuzzleUnitSize(boolean defaults) {
		Integer unitSize = configuration.getInt(VariableKey.UNIT_SIZE(this), defaults);
		return unitSize  != null ? unitSize : scramblePlugin.getDefaultUnitSize();
	}

	public void setPuzzleUnitSize(int size) {
		if(this != scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation()) {
			configuration.setLong(VariableKey.UNIT_SIZE(this), size);
		}
	}

	public ScramblePlugin getPlugin() {
		return scramblePlugin;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof ScrambleVariation)) {
			return false;
		}
		ScrambleVariation other = (ScrambleVariation) o;
		return this.scramblePlugin.equals(other.scramblePlugin)
                && this.variationName.equals(other.variationName)
                && this.length == other.length;
	}

	public String toString() {
		return variationName.isEmpty() ? scramblePlugin.getPuzzleName() : variationName;
	}
}
