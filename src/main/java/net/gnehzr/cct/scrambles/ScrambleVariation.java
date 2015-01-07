package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.Scramble.InvalidScrambleException;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;

public class ScrambleVariation {

	private static final Logger LOG = Logger.getLogger(ScrambleVariation.class);

	private String variation;
	private int length = 0;
	private ScramblePlugin scramblePlugin;
	private Icon image;
	private final Configuration configuration;

	public ScrambleVariation(ScramblePlugin plugin, String variation, Configuration configuration) {
		this.scramblePlugin = plugin;
		this.variation = variation;
		this.configuration = configuration;
		length = getScrambleLength(false);
	}
	
	public Icon getImage() {
		if(image == null) {
			try {
				image = new ImageIcon(new File(scramblePlugin.scramblePluginsFolder, variation + ".png").toURI().toURL());
			} catch (MalformedURLException e) {
				LOG.info("unexpected exception", e);
				image = new ImageIcon();
			}
		}
		return image;
	}
	
	public int getScrambleLength(boolean defaultValue) {
		try {
			return configuration.getInt(VariableKey.SCRAMBLE_LENGTH(this), defaultValue).intValue();
		} catch(Throwable e) {} //we don't want things to break even if configuration.class doesn't exists
		return scramblePlugin.getDefaultScrambleLength(this);
	}

	public ScramblePlugin getScramblePlugin() {
		return scramblePlugin;
	}
	public String getVariation() {
		return variation;
	}
	public void setLength(int l) {
		length = l;
	}
	public int getLength() {
		return length;
	}

	public Scramble generateScramble() {
		return scramblePlugin.newScramble(variation, length, scramblePlugin.getDefaultGeneratorGroup(this), scramblePlugin.getEnabledPuzzleAttributes());
	}
	
	public Scramble generateScrambleFromGroup(String generatorGroup) {
		return scramblePlugin.newScramble(variation, length, generatorGroup, scramblePlugin.getEnabledPuzzleAttributes());
	}

	public Scramble generateScramble(String scramble) throws InvalidScrambleException {
		return scramblePlugin.importScramble(variation, scramble, scramblePlugin.getDefaultGeneratorGroup(this), scramblePlugin.getEnabledPuzzleAttributes());
	}

	public int getPuzzleUnitSize(boolean defaults) {
		try {
			return configuration.getInt(VariableKey.UNIT_SIZE(this), defaults).intValue();
		} catch(Exception e) {}
		return scramblePlugin.DEFAULT_UNIT_SIZE;
	}
	public void setPuzzleUnitSize(int size) {
		if(this != scramblePlugin.NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation())
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
}
