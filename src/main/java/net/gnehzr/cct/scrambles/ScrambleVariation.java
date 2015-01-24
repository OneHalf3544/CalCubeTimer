package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.Scramble.InvalidScrambleException;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.net.URL;

public class ScrambleVariation {

	private static final Logger LOG = Logger.getLogger(ScrambleVariation.class);

	private String variation;
	private int length = 0;
	private Scramble scramblePlugin;
	private final Icon image;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;

	public ScrambleVariation(Scramble plugin, String variation, Configuration configuration, ScramblePluginManager scramblePluginManager) {
		this.scramblePlugin = plugin;
		this.variation = variation;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		length = getScrambleLength(false);
		image = getImageIcon(variation);
	}

	private ImageIcon getImageIcon(String variation) {
		URL resource = scramblePlugin.getClass().getResource(Scramble.class.getSimpleName() + "_" + variation + ".png");
		return resource == null ? new ImageIcon() : new ImageIcon(resource);
	}

	public Icon getImage() {
		return image;
	}
	
	public int getScrambleLength(boolean defaultValue) {
		if (scramblePlugin == Scramble.NULL_SCRAMBLE) {
			return 0;
		}
		Integer length = configuration.getInt(VariableKey.SCRAMBLE_LENGTH(this), defaultValue);
		// TODO use correct default value instead first
		return length == null ? scramblePlugin.getDefaultLengths()[0] : length;
	}

	public String getVariation() {
		return variation;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getLength() {
		return length;
	}

	public Scramble generateScramble() {
		return scramblePlugin.newScramble(variation, length, scramblePluginManager.getDefaultGeneratorGroup(this), getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}
	
	public Scramble generateScrambleFromGroup(String generatorGroup) {
		return scramblePlugin.newScramble(variation, length, generatorGroup, getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public Scramble generateScramble(String scramble) throws InvalidScrambleException {
		return scramblePlugin.importScramble(variation, scramble, scramblePluginManager.getDefaultGeneratorGroup(this), getPlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration));
	}

	public int getPuzzleUnitSize(boolean defaults) {
		Integer unitSize = configuration.getInt(VariableKey.UNIT_SIZE(this), defaults);
		return unitSize  != null ? unitSize : scramblePlugin.getDefaultUnitSize();
	}
	public void setPuzzleUnitSize(int size) {
		if(this != scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation())
			configuration.setLong(VariableKey.UNIT_SIZE(this), size);
	}
	
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean equals(Object o) {
		try {
			ScrambleVariation other = (ScrambleVariation) o;
			return this.scramblePlugin.equals(other.scramblePlugin) && this.variation.equals(other.variation) && this.length == other.length;
		} catch(Exception e) {
			return false;
		}
	}
	public String toString() {
		return variation.isEmpty() ? scramblePlugin.getPuzzleName() : variation;
	}

	public Scramble getPlugin() {
		return scramblePlugin;
	}
}
