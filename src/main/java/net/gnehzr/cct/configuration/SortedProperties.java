package net.gnehzr.cct.configuration;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.gnehzr.cct.dao.ConfigurationDao;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class SortedProperties {

	private static final Logger LOG = LogManager.getLogger(SortedProperties.class);

	static SortedProperties NOT_LOADED_PROPERTIES = new SortedProperties(ImmutableMap.of(), ImmutableMap.of()) {
		@Override
		public String getValue(VariableKey<?> key, boolean defaultValue) {
			throw new IllegalStateException("get value from uninitialized properties. key=" + key);
		}
	};

	private final Map<String, String> properties;
	private final ImmutableSortedMap<String, String> defaultProperties;

	public SortedProperties(Map<String, String> properties, Map<String, String> defaultProperties) {
		this.properties = Maps.newHashMap(properties);
		this.defaultProperties = ImmutableSortedMap.copyOf(defaultProperties);
	}

	public static SortedProperties load(Profile profileName, ConfigurationDao configurationDao, File defaultsFile) {
		//call loadConfiguration(null) when you want to use cct without dealing with config files
		return new SortedProperties(configurationDao.getParametersForProfile(profileName), loadDefaultProperties(defaultsFile));
	}

	public static SortedProperties loadDefaults(File defaultsFile) {
		//call loadConfiguration(null) when you want to use cct without dealing with config files
		return new SortedProperties(Collections.emptyMap(), loadDefaultProperties(defaultsFile));
	}

	public void saveConfiguration(@NotNull Profile profile, @NotNull ConfigurationDao configurationDao) {
		configurationDao.storeParameters(profile, this.properties);
	}

	private static ImmutableMap<String, String> loadDefaultProperties(File defaultsFile) {
		try(InputStream in = new FileInputStream(defaultsFile)) {
			Properties defaults = new Properties();
			defaults.load(in);
			return Maps.fromProperties(defaults);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	public boolean keyExists(VariableKey<?> key) {
		return properties.containsKey(key.toKey()) || defaultProperties.containsKey(key.toKey());
	}

	@Nullable
	private String getValue(String key, boolean getOnlyDefaultValue) {
		String val = properties.get(key);
		if (getOnlyDefaultValue || val == null) {
			val = defaultProperties.get(key);
		}
		return val;
	}

	String getValue(VariableKey<?> key, boolean getOnlyDefaultValue) {
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

	@NotNull
	public String getString(VariableKey<String> key, boolean defaultValue) {
		String value = getValue(key, defaultValue);
		return getNonnullStringForKey(key, value);
	}

	private String getNonnullStringForKey(Object key, String value) {
		return value == null ? "Couldn't find key " + key : value;
	}

	@NotNull
	private String getString(String key) {
		return getNonnullStringForKey(key, getValue(key, false));
	}
	public void setString(VariableKey<String> key, String value) {
		setValue(key, value);
	}

	public Long getLong(VariableKey<Integer> key, boolean defaultValue) {
		return getLong(key.toKey(), defaultValue);
	}

	public Long getLong(String key, boolean defaultValue) {
		String value = getValue(key, defaultValue);
		return value == null ? null : Long.parseLong(value);
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

	@Nullable
	public Dimension getDimension(VariableKey<Dimension> key) {
		String value = getValue(key, false);
		if (value == null || Objects.equals(value, "auto")) {
            return null;
        }

		String[] dims = value.split("x");
		Dimension temp = new Dimension(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
		if(temp.height <= 0) //we don't allow invisible dimensions
            temp.height = 100;
		if(temp.width <= 0)
            temp.width = 100;
		return temp;
	}

	public void setDimension(VariableKey<Dimension> key, @NotNull Dimension newValue) {
		setValue(key, newValue.width + "x" + newValue.height);
	}

	public Point getPoint(VariableKey<Point> key, boolean defaultValue) {
		String value = getValue(key, defaultValue);
		if (value == null || Objects.equals(value, "auto")) {
            return null;
        }
		String[] dims = value.split(",");
		return new Point(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
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
	public List<String> getStringArray(VariableKey<List<String>> key, boolean defaultValue) {
		String value = getValue(key, defaultValue);
		return value == null ? null : Lists.newArrayList(value.split("\n"));
	}
	public void setStringArray(VariableKey<List<String>> key, List<?> arr) {
		String mashed = "";
		for(Object o : arr) {
			mashed += o.toString() + "\n";
		}
		setValue(key, mashed);
	}

	public Integer[] getIntegerArray(VariableKey<Integer[]> key, boolean defaultValue) {
		String value = getValue(key, defaultValue);
		if (value == null) {
            return null;
        }

		String[] s = value.split("\n");
		Integer[] i = new Integer[s.length];
		for(int ch = 0; ch < s.length; ch++) {
            i[ch] = Integer.parseInt(s[ch]);
        }
		return i;
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

	public String getNullableString(VariableKey<String> key, boolean defaults) {
		return getValue(key, defaults);
	}
}
