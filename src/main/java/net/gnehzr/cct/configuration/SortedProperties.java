package net.gnehzr.cct.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import net.gnehzr.cct.main.CALCubeTimer;
import net.gnehzr.cct.misc.Utils;

import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class SortedProperties {

	private final Map<String, String> properties;
	private final ImmutableSortedMap<String, String> defaultProperties;

	public SortedProperties(Map<String, String> properties, Map<String, String> defaultProperties) {
		this.properties = Maps.newHashMap(properties);
		this.defaultProperties = ImmutableSortedMap.copyOf(defaultProperties);
	}

	public static SortedProperties load(File propertiesFile, File defaultsFile) throws IOException {
		//call loadConfiguration(null) when you want to use cct without dealing with config files
		if (propertiesFile == null) {
			return new SortedProperties(Collections.emptyMap(), loadFile(defaultsFile));
		}

		if (!propertiesFile.exists()) {
			propertiesFile.createNewFile();
			return new SortedProperties(Collections.emptyMap(), loadFile(defaultsFile));
		}

		return new SortedProperties(loadFile(propertiesFile), loadFile(defaultsFile));
	}

	public void saveConfigurationToFile(File f) throws IOException {
		try (FileOutputStream propsOut = new FileOutputStream(f)) {
			Properties properties = new Properties();
			properties.putAll(this.properties);
			properties.store(propsOut, "CCT " + CALCubeTimer.CCT_VERSION + " Properties File");
		}
	}

	public static ImmutableMap<String, String> loadFile(File defaultsFile) throws IOException {
		try(InputStream in = new FileInputStream(defaultsFile)) {
			Properties defaults = new Properties();
			defaults.load(in);
			return Maps.fromProperties(defaults);
		}
	}

	public boolean keyExists(VariableKey<?> key) {
		return properties.containsKey(key.toKey()) || defaultProperties.containsKey(key.toKey());
	}

	private String getValue(String key, boolean getOnlyDefaultValue) {
		String val = properties.get(key);
		if (getOnlyDefaultValue || val == null) {
			val = defaultProperties.get(key);
		}
		return val == null ? "Couldn't find key " + key : val;
	}

	private String getValue(VariableKey<?> key, boolean getOnlyDefaultValue) {
		return getValue(key.toKey(), getOnlyDefaultValue);
	}

	public double getDouble(VariableKey<Double> key, boolean defaultValue) {
		return Double.parseDouble(getValue(key, defaultValue));
	}

	public void setDouble(VariableKey<Double> key, double value) {
		setValue(key, Double.toString(value));
	}

	private void setValue(VariableKey<?> key, String s) {
		properties.put(key.toKey(), s);
	}

	public String getString(VariableKey<String> key, boolean defaultValue) {
		return getValue(key, defaultValue);
	}
	private String getString(String key) {
		return getValue(key, false);
	}
	public void setString(VariableKey<String> key, String value) {
		setValue(key, value);
	}

	public Long getLong(VariableKey<Integer> key, boolean defaultValue) {
		try {
			return Long.parseLong(getValue(key, defaultValue));
		} catch(Exception e) {
			return null;
		}
	}
	public void setLong(VariableKey<Integer> key, long value) {
		setValue(key, Long.toString(value));
	}

	public Font getFont(VariableKey<Font> key, boolean defaultValue) {
		return Font.decode(getValue(key, defaultValue));
	}
	public void setFont(VariableKey<Font> key, Font newFont) {
		setValue(key, Utils.fontToString(newFont));
	}

	public boolean getBoolean(VariableKey<Boolean> key, boolean defaultValue) {
		return Boolean.parseBoolean(getValue(key, defaultValue));
	}
	public void setBoolean(VariableKey<Boolean> key, boolean newValue) {
		setValue(key, Boolean.toString(newValue));
	}

	public Dimension getDimension(VariableKey<Dimension> key, boolean defaultValue) {
		try {
			String[] dims = getValue(key, false).split("x");
			Dimension temp = new Dimension(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
			if(temp.height <= 0) //we don't allow invisible dimensions
				temp.height = 100;
			if(temp.width <= 0)
				temp.width = 100;
			return temp;
		} catch(Exception e) {
			return null;
		}
	}
	public void setDimension(VariableKey<Dimension> key, Dimension newValue) {
		setValue(key, newValue.width + "x" + newValue.height);
	}

	public Point getPoint(VariableKey<Point> key, boolean defaultValue) {
		try {
			String[] dims = getValue(key, defaultValue).split(",");
			return new Point(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
		} catch(Exception e) {
			return null;
		}
	}
	public void setPoint(VariableKey<Point> key, Point newValue) {
		setValue(key, newValue.x + "," + newValue.y);
	}

	public Color getColorNullIfInvalid(VariableKey<Color> key, boolean defaultValue) {
		return getColor(key, defaultValue, true);
	}
	public Color getColor(VariableKey<Color> key, boolean defaultValue) {
		return getColor(key, defaultValue, false);
	}
	private Color getColor(VariableKey<Color> key, boolean defaultValue, boolean nullIfInvalid) {
		return Utils.stringToColor(getValue(key, defaultValue), nullIfInvalid);
	}
	public void setColor(VariableKey<Color> key, Color c) {
		setValue(key, Utils.colorToString(c));
	}

	//special characters are for now just ';'
	public String[] getStringArray(VariableKey<String[]> key, boolean defaultValue) {
		try {
			return getValue(key, defaultValue).split("\n");
		} catch(NullPointerException e) {
			return null;
		}
	}
	public void setStringArray(VariableKey<String[]> key, Object[] arr) {
		String mashed = "";
		for(Object o : arr) {
			mashed += o.toString() + "\n";
		}
		setValue(key, mashed);
	}

	public Integer[] getIntegerArray(VariableKey<Integer[]> key, boolean defaultValue) {
		try {
			String[] s = getValue(key, defaultValue).split("\n");
			Integer[] i = new Integer[s.length];
			for(int ch = 0; ch < s.length; ch++) {
				i[ch] = Integer.parseInt(s[ch]);
			}
			return i;
		} catch(NullPointerException e) {
			return null;
		}
	}
	public void setIntegerArray(VariableKey<Integer[]> key, Integer[] arr) {
		String mashed = "";
		for(int i : arr) {
			mashed += i + "\n";
		}
		setValue(key, mashed);
	}

	public float getFloat(VariableKey<Float> key, boolean defaultValue) {
		return Float.parseFloat(getValue(key, defaultValue));
	}

	public void setFloat(VariableKey<Float> opacity, float v) {
		setValue(opacity, Float.toString(v));
	}
}
