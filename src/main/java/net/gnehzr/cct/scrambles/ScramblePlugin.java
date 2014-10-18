package net.gnehzr.cct.scrambles;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.Scramble.InvalidScrambleException;

public class ScramblePlugin {
	public static final ScrambleCustomization NULL_SCRAMBLE_CUSTOMIZATION = new ScrambleCustomization(new ScrambleVariation(new ScramblePlugin("X"), ""), null);
	public static final String SCRAMBLE_PLUGIN_PACKAGE = "scramblePlugins.";
	private static final String PLUGIN_EXTENSION = ".class";
	public static final File scramblePluginsFolder = new File(getRootDirectory(), "scramblePlugins/");
	public static File getRootDirectory() { //this is duplicated from configuration
		File root = null;
		try {
			root = new File(ScramblePlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(root.isFile())
				root = root.getParentFile();
		} catch(URISyntaxException e) {
			e.printStackTrace();
		}
		return root;
	}
	
	
	private static ArrayList<ScramblePlugin> scramblePlugins;
	public static ArrayList<ScramblePlugin> getScramblePlugins() {
		if(scramblePlugins == null) {
			scramblePlugins = new ArrayList<ScramblePlugin>();
			if(scramblePluginsFolder.isDirectory()) {
				for(File plugin : scramblePluginsFolder.listFiles()) {
					if(!plugin.getName().endsWith(".class") || plugin.getName().indexOf('$') != -1)
						continue;
					try {
						scramblePlugins.add(new ScramblePlugin(plugin));
					} catch(Exception ee) {
						System.err.println("Failed to load: " + plugin);
						ee.printStackTrace();
					}
				}
			}
		}
		return scramblePlugins;
	}
	//this has the potential to break a lot of things in cct,
	//it's only used by cctbot right now
	public static void clearScramblePlugins() {
		scramblePlugins = null;
		scrambleVariations = null;
	}

	public static void saveLengthsToConfiguration() {
		for(ScrambleVariation variation : getScrambleVariations())
			Configuration.setInt(VariableKey.SCRAMBLE_LENGTH(variation), variation.getLength());
	}
	public static void reloadLengthsFromConfiguration(boolean defaults) {
		for(ScrambleVariation v : getScrambleVariations())
			v.setLength(v.getScrambleLength(defaults));
	}
	private static ScrambleVariation[] scrambleVariations;
	public static ScrambleVariation[] getScrambleVariations() {
		if(scrambleVariations == null) {
			ArrayList<ScrambleVariation> vars = new ArrayList<ScrambleVariation>();
			for(ScramblePlugin p : getScramblePlugins()) {
				for(String var : p.VARIATIONS)
					vars.add(new ScrambleVariation(p, var));
			}
			if(vars.isEmpty())
				vars.add(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation());
			scrambleVariations = vars.toArray(new ScrambleVariation[0]);
		}
		return scrambleVariations;
	}
	public static ScrambleVariation getBestMatchVariation(String variation) {
		if(variation == null) return null;
		for(ScrambleVariation var : ScramblePlugin.getScrambleVariations())
			if(var.toString().toLowerCase().startsWith(variation))
				return var;
		return null;
	}
	public static ScrambleCustomization getCurrentScrambleCustomization() {
		String scName = Configuration.getString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, false);
		ScrambleCustomization sc = getCustomizationFromString(scName);

		//now we'll try to match the variation, if we couldn't match the customization
		if(sc == null && scName.indexOf(':') != -1) {
			scName = scName.substring(0, scName.indexOf(":"));
			sc = getCustomizationFromString(scName);
		}
		if(sc == null) {
			ArrayList<ScrambleCustomization> scs = getScrambleCustomizations(false);
			if(scs.size() > 0)
				sc = scs.get(0);
		}
		return sc;
	}
	
	public static ScrambleCustomization getCustomizationFromVariation(ScrambleVariation sv) {
		if(sv == null)
			return null;
		return getCustomizationFromString(sv.toString());
	}
	public static ScrambleCustomization getCustomizationFromString(String customName) {
		ArrayList<ScrambleCustomization> scrambleCustomizations = getScrambleCustomizations(false);
		for(ScrambleCustomization custom : scrambleCustomizations)
			if(custom.toString().equals(customName))
				return custom;
		
		return null;
	}

	public static ArrayList<ScrambleCustomization> getScrambleCustomizations(boolean defaults) {
		ArrayList<ScrambleCustomization> scrambleCustomizations = new ArrayList<ScrambleCustomization>();
		for(ScrambleVariation t : getScrambleVariations())
			scrambleCustomizations.add(new ScrambleCustomization(t, null));
		String[] customNames = Configuration.getStringArray(VariableKey.SCRAMBLE_CUSTOMIZATIONS, defaults);
		if(customNames == null)	customNames = new String[0];
		Iterator<String> databaseCustoms = Configuration.getSelectedProfile().getPuzzleDatabase().getCustomizations().iterator();
		int ch = customNames.length - 1;
		while(true) {
			String name;
			if(databaseCustoms.hasNext()) {
				name = databaseCustoms.next();
			} else {
				if(ch < 0)
					break;
				name = customNames[ch--];
			}
			int delimeter = name.indexOf(':');
			String customizationName;
			if(delimeter == -1) {
				delimeter = name.length();
				customizationName = null;
			} else
				customizationName = name.substring(delimeter + 1, name.length());
			String variationName = name.substring(0, delimeter);
			ScrambleCustomization scramCustomization = null;
			for(ScrambleCustomization custom : scrambleCustomizations) {
				if(variationName.equals(custom.toString())) {
					scramCustomization = custom;
					break;
				}
			}
			ScrambleCustomization sc;
			if(scramCustomization != null)
				sc = new ScrambleCustomization(scramCustomization.getScrambleVariation(), customizationName);
			else if(variationName.equals(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation().toString()))
				sc = new ScrambleCustomization(NULL_SCRAMBLE_CUSTOMIZATION.getScrambleVariation(), customizationName);
			else
				sc = new ScrambleCustomization(new ScrambleVariation(new ScramblePlugin(variationName), variationName), customizationName);
			if(!variationName.isEmpty()) {
				if(scrambleCustomizations.contains(sc)) {
					if(ch == customNames.length - 1) //we don't want to move this customization to the front of the list if it's from the database
						continue;
					scrambleCustomizations.remove(sc);
				}
				scrambleCustomizations.add(0, sc);
			}
		}
		return scrambleCustomizations;
	}

	public String[] getAvailablePuzzleAttributes() {
		return ATTRIBUTES;
	}
	public String[] getEnabledPuzzleAttributes() {
		if(attributes == null) {
			try {
				attributes = Configuration.getStringArray(VariableKey.PUZZLE_ATTRIBUTES(this), false);
			} catch(Throwable t) {} //we want this to work, even if there's no configuration
			if(attributes == null)
				attributes = DEFAULT_ATTRIBUTES;
		}
		return attributes;
	}
	private String[] attributes;
	public void setEnabledPuzzleAttributes(String[] attributes) {
		this.attributes = attributes;
	}

	String pluginClassName;
	
	private Class<? extends Scramble> pluginClass = null;
	
	Constructor<? extends Scramble> newScrambleConstructor = null;
	Constructor<? extends Scramble> importScrambleConstructor = null;
	
	Method getNewUnitSize;
	Method getImageSize;
	Method getScrambleImage;
	Method getFaces;
	Method htmlify;

	protected String PUZZLE_NAME;
	protected String[][] FACE_NAMES_COLORS;
	protected int DEFAULT_UNIT_SIZE;
	protected int[] DEFAULT_LENGTHS;
	protected String[] VARIATIONS;
	protected String[] ATTRIBUTES;
	protected String[] DEFAULT_ATTRIBUTES;
	protected String[] DEFAULT_GENERATORS;
	protected Pattern TOKEN_REGEX;

	public ScramblePlugin(String variationName) {
		PUZZLE_NAME = variationName;
		pluginClassName = variationName;
		FACE_NAMES_COLORS = null;
		DEFAULT_UNIT_SIZE = 0;
		VARIATIONS = new String[0];
		ATTRIBUTES = new String[0];
		DEFAULT_ATTRIBUTES = new String[0];
	}

	protected ScramblePlugin(final File plugin) throws SecurityException, NoSuchMethodException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException, NoClassDefFoundError {
		pluginClassName = plugin.getName();
		if(!pluginClassName.endsWith(PLUGIN_EXTENSION))
			throw new ClassNotFoundException("Filename (" + plugin.getAbsolutePath() + ") must end in " + PLUGIN_EXTENSION);
		pluginClassName = pluginClassName.substring(0, pluginClassName.length() - PLUGIN_EXTENSION.length());
		
		Class<?> cls = null;
		try {
			//this will initialize the class, we protect from a malicious plugin
			//whose static{} initialization block never returns
			cls = TimeoutJob.doWork(new Callable<Class<?>>() {
				public Class<?> call() throws Exception {
					for(String className : plugin.getParentFile().list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return new File(dir, name).isFile() && name.startsWith(pluginClassName + "$");
						}
					})) {
						Class.forName(SCRAMBLE_PLUGIN_PACKAGE + className.substring(0, className.length() - PLUGIN_EXTENSION.length()), true, TimeoutJob.PLUGIN_LOADER);
					}
					return Class.forName(SCRAMBLE_PLUGIN_PACKAGE + pluginClassName, true, TimeoutJob.PLUGIN_LOADER);
				}
			});
		} catch (Throwable e) {
			if(e.getCause() != null)
				e.getCause().printStackTrace();
			throw new ClassNotFoundException("Failure loading class " + SCRAMBLE_PLUGIN_PACKAGE + pluginClassName + "", e);
		}
		if(!Scramble.class.equals(cls.getSuperclass()))
			throw new ClassCastException("Superclass of " + cls + " is " + cls.getSuperclass() + ", it should be " + Scramble.class);
		
		pluginClass = cls.asSubclass(Scramble.class);

		try {
			//validating methods/constructors
			newScrambleConstructor = pluginClass.getConstructor(String.class, int.class, String.class, String[].class);
			importScrambleConstructor = pluginClass.getConstructor(String.class, String.class, String.class, String[].class);
			
			try {
				getScrambleImage = pluginClass.getMethod("getScrambleImage", int.class, int.class, Color[].class);
				if(!getScrambleImage.getReturnType().equals(BufferedImage.class))
					throw new ClassCastException("getScrambleImage() return type should be BufferedImage, not " + getScrambleImage.getReturnType());
				assertPublicNotAbstract(getScrambleImage, false);
			} catch(NoSuchMethodException e) {} //this is fine, we'll just return null for the scramble image
			
			try {
				getNewUnitSize = pluginClass.getMethod("getNewUnitSize", int.class, int.class, int.class, String.class);
				if(!getNewUnitSize.getReturnType().equals(int.class))
					throw new ClassCastException("getNewUnitSize() return type should be int, not " + getNewUnitSize.getReturnType());
				assertPublicNotAbstract(getNewUnitSize, true);
			} catch(NoSuchMethodException e) {}
			
			try {
				getImageSize = pluginClass.getMethod("getImageSize", int.class, int.class, String.class);
				if(!getImageSize.getReturnType().equals(Dimension.class))
					throw new ClassCastException("getImageSize() return type should be Dimension, not " + getImageSize.getReturnType());
				assertPublicNotAbstract(getImageSize, true);
			} catch(NoSuchMethodException e) {}
			
			try {
				getFaces = pluginClass.getMethod("getFaces", int.class, int.class, String.class);
				if(!getFaces.getReturnType().equals(Shape[].class))
					throw new ClassCastException("getFaces() return type should be Shape[], not " + getFaces.getReturnType());
				assertPublicNotAbstract(getFaces, true);
			} catch(NoSuchMethodException e) {}
			
			try {
				htmlify = pluginClass.getMethod("htmlify", String.class);
				if(!htmlify.getReturnType().equals(String.class))
					throw new ClassCastException("htmlify() return type should be String, not " + htmlify.getReturnType());
				assertPublicNotAbstract(htmlify, true);
			} catch(NoSuchMethodException e) {}
			
			//validating fields
			Field f = getPrivateStaticField(pluginClass, "PUZZLE_NAME");
			PUZZLE_NAME = (String) f.get(null);
			if(PUZZLE_NAME == null)
				throw new NullPointerException("PUZZLE_NAME may not be null!");
			if(PUZZLE_NAME.indexOf(':') != -1)
				throw new IllegalArgumentException("PUZZLE_NAME (" + PUZZLE_NAME + ") may not contain ':'!");
			
			try {
				f = getPrivateStaticField(pluginClass, "FACE_NAMES_COLORS");
				FACE_NAMES_COLORS = (String[][]) f.get(null);
				if(FACE_NAMES_COLORS != null) {
					if(FACE_NAMES_COLORS.length != 2)
						throw new ArrayIndexOutOfBoundsException("FACE_NAMES_COLORS.length (" + FACE_NAMES_COLORS.length + ") does not equal 2!");
					if(FACE_NAMES_COLORS[0].length != FACE_NAMES_COLORS[1].length)
						throw new ArrayIndexOutOfBoundsException("FACE_NAMES_COLORS[0].length (" + FACE_NAMES_COLORS[0].length + ") != FACE_NAMES_COLORS[1].length (" + FACE_NAMES_COLORS[1].length + ")");
				}
			} catch(NoSuchFieldException e) {}
	
			try {
				f = getPrivateStaticField(pluginClass, "DEFAULT_UNIT_SIZE");
				DEFAULT_UNIT_SIZE = f.getInt(null);
			} catch(NoSuchFieldException e) {}
			
			try {
				f = getPrivateStaticField(pluginClass, "VARIATIONS");
				VARIATIONS = (String[]) f.get(null);
				if(VARIATIONS == null)
					throw new NullPointerException("VARIATIONS may not be null!");
				for(String var : VARIATIONS) {
					if(var == null || var.isEmpty())
						throw new NullPointerException("Scramble variations may not be null or the empty string!");
					if(var.indexOf(':') != -1)
						throw new IllegalArgumentException("Scramble variation (" + var + ") may not contain ':'!");
				}
			} catch(NoSuchFieldException e) {
				VARIATIONS = new String[] { "" };
			}
			
			try {
				f = getPrivateStaticField(pluginClass, "DEFAULT_LENGTHS");
				DEFAULT_LENGTHS = (int[]) f.get(null);
				if(DEFAULT_LENGTHS == null)
					throw new NullPointerException("DEFAULT_LENGTHS may not be null!");
				//there's no need to deal w/ negative lengths here, we'll deal with it later
			} catch(NoSuchFieldException e) {
				DEFAULT_LENGTHS = new int[] { 0 };
			}

			if(VARIATIONS.length != DEFAULT_LENGTHS.length)
				throw new ArrayIndexOutOfBoundsException("VARIATIONS.length (" + VARIATIONS.length + ") != DEFAULT_LENGTHS.length (" + DEFAULT_LENGTHS.length + ")");
			
			try {
				f = getPrivateStaticField(pluginClass, "ATTRIBUTES");
				ATTRIBUTES = (String[]) f.get(null);
				if(ATTRIBUTES == null)
					throw new NullPointerException("ATTRIBUTES may not be null!");
				for(String c : ATTRIBUTES)
					if(c == null || c.isEmpty())
						throw new IllegalArgumentException("Attributes may not be null or empty!");
			} catch(NoSuchFieldException e) {
				ATTRIBUTES = new String[0];
			}

			try {
				f = getPrivateStaticField(pluginClass, "DEFAULT_ATTRIBUTES");
				DEFAULT_ATTRIBUTES = (String[]) f.get(null);
				if(DEFAULT_ATTRIBUTES == null)
					throw new NullPointerException("DEFAULT_ATTRIBUTES may not be null!");
				for(String c : DEFAULT_ATTRIBUTES) {
					if(c == null || c.isEmpty())
						throw new IllegalArgumentException("Default attributes may not be null or empty!");
					int ch;
					for(ch = 0; ch < ATTRIBUTES.length; ch++)
						if(c.equals(ATTRIBUTES[ch]))
							break;
					if(ch == ATTRIBUTES.length) //indicates that this default attribute wasn't found in ATTRIBUTES
						throw new IllegalArgumentException("Default attribute (" + c + ") not found in ATTRIBUTES!");
				}
			} catch(NoSuchFieldException e) {
				DEFAULT_ATTRIBUTES = new String[0];
			}
			
			try {
				f = getPrivateStaticField(pluginClass, "TOKEN_REGEX");
				TOKEN_REGEX = (Pattern) f.get(null);
			} catch(NoSuchFieldException e) {
				
			}
			
			try {
				f = getPrivateStaticField(pluginClass, "DEFAULT_GENERATORS");
				DEFAULT_GENERATORS = (String[]) f.get(null);
				if(DEFAULT_GENERATORS.length != VARIATIONS.length)
					throw new ArrayIndexOutOfBoundsException("DEFAULT_GENERATORS.length (" + DEFAULT_GENERATORS.length + ") != VARIATIONS.length (" + VARIATIONS.length + ")");
			} catch(NoSuchFieldException e) {
				
			}
		} catch(NoClassDefFoundError e) {
			if(e.getCause() != null)
				e.getCause().printStackTrace();
			throw new ClassNotFoundException("", e);
		}
	}
	
	public static Field getPrivateStaticField(Class<?> c, String name) throws NoSuchFieldException {
		Field field = null;
		for(Field f : c.getDeclaredFields())
			if(f.getName().equals(name)) {
				field = f;
				break;
			}
		if(field == null)
			throw new NoSuchFieldException("Could not find field: " + name + "!");
		if(Modifier.isAbstract(field.getModifiers()) || !Modifier.isStatic(field.getModifiers()) || !Modifier.isPrivate(field.getModifiers()))
			throw new NullPointerException(name + " must be private, static, and not abstract!"); //we can't use NoSuchFieldException, because we don't want this to be caught
		field.setAccessible(true);
		return field;
	}
	
	private static void assertPublicNotAbstract(Method m, boolean isStatic) throws NoSuchMethodException {
		if(!Modifier.isPublic(m.getModifiers()) || (isStatic ^ Modifier.isStatic(m.getModifiers())) || Modifier.isAbstract(m.getModifiers()))
			throw new NoSuchMethodException(m.toGenericString() + " must be public, not abstract, and " + (isStatic ? "" : "not ") + "static!");
	}
	
	public Class<? extends Scramble> getPluginClass() {
		return pluginClass;
	}
	
	public String getPluginClassName() {
		return pluginClassName;
	}

	public Scramble newScramble(final String variation, final int length, final String generatorGroup, final String[] attributes) {
		if(newScrambleConstructor != null) {
			try {
				return TimeoutJob.doWork(new Callable<Scramble>() {
					public Scramble call() throws Exception {
						return newScrambleConstructor.newInstance(variation, length, generatorGroup, attributes);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				if(e.getCause() != null)
					e.getCause().printStackTrace();
				e.printStackTrace();
			}
		}
		return new Scramble("");
	}
	
	public Scramble importScramble(final String variation, final String scramble, final String generatorGroup, final String[] attributes) throws InvalidScrambleException {
		if(importScrambleConstructor != null) {
			try {
				return TimeoutJob.doWork(new Callable<Scramble>() {
					public Scramble call() throws Exception {
						return importScrambleConstructor.newInstance(variation, scramble, generatorGroup, attributes);
					}
				});
			} catch (InvocationTargetException e) {
				if(e.getCause() instanceof InvalidScrambleException)
					throw (InvalidScrambleException) e.getCause();
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
				Thread.dumpStack();
			}
		}
		return new Scramble(scramble);
	}
	
	public boolean supportsScrambleImage() {
		return getScrambleImage != null;
	}
	
	public BufferedImage getScrambleImage(final Scramble instance, final int gap, final int unitSize, final Color[] colorScheme) {
		if(getScrambleImage != null && pluginClass != null && instance != null && pluginClass.equals(instance.getClass())) {
			try {
				return TimeoutJob.doWork(new Callable<BufferedImage>() {
					public BufferedImage call() throws Exception {
						return (BufferedImage) getScrambleImage.invoke(instance, gap, Math.max(unitSize, DEFAULT_UNIT_SIZE), colorScheme);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public int getDefaultScrambleLength(ScrambleVariation var) {
		int c = getIndexOfVariation(var);
		if(c == -1)
			return 0;
		return DEFAULT_LENGTHS[c];
	}
	public String getDefaultGeneratorGroup(ScrambleVariation var) {
		int c = getIndexOfVariation(var);
		if(c == -1 || DEFAULT_GENERATORS == null)
			return null;
		return DEFAULT_GENERATORS[c];
	}
	
	public boolean isGeneratorEnabled() {
		return DEFAULT_GENERATORS != null;
	}
	
	private int getIndexOfVariation(ScrambleVariation var) {
		int c;
		for(c = 0; c < VARIATIONS.length; c++)
			if(VARIATIONS[c].equals(var.getVariation()))
				break;
		
		if(c == VARIATIONS.length)
			return -1;
		return c;
	}

	public String[][] getFaceNames() {
		return FACE_NAMES_COLORS;
	}
	public String getPuzzleName() {
		return PUZZLE_NAME;
	}
	public Pattern getTokenRegex() {
		return TOKEN_REGEX;
	}
	
	public int getNewUnitSize(final int width, final int height, final int gap, final String variation) {
		if(getNewUnitSize != null) {
			try {
				return TimeoutJob.doWork(new Callable<Integer>() {
					public Integer call() throws Exception {
						return (Integer) getNewUnitSize.invoke(null, width, height, gap, variation);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return -1;
	}
	
	public Dimension getImageSize(final int gap, final int unitSize, final String variation) {
		if(getImageSize != null) {
			try {
				return TimeoutJob.doWork(new Callable<Dimension>() {
					public Dimension call() throws Exception {
						return (Dimension) getImageSize.invoke(null, gap, unitSize, variation);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public Shape[] getFaces(final int gap, final int unitSize, final String variation) {
		if(getFaces != null) {
			try {
				return TimeoutJob.doWork(new Callable<Shape[]>() {
					public Shape[] call() throws Exception {
						return (Shape[]) getFaces.invoke(null, gap, unitSize, variation);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public String htmlify(final String scramble) {
		if(htmlify != null) {
			try {
				return TimeoutJob.doWork(new Callable<String>() {
					public String call() throws Exception {
						return (String) htmlify.invoke(null, scramble);
					}
				});
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return scramble;
	}

	public Color[] getColorScheme(boolean defaults) {
		if(FACE_NAMES_COLORS == null) //this is for null scrambles
			return null;
		Color[] scheme = new Color[FACE_NAMES_COLORS[0].length];
		for(int face = 0; face < scheme.length; face++) {
			scheme[face] = Configuration.getColorNullIfInvalid(VariableKey.PUZZLE_COLOR(this, FACE_NAMES_COLORS[0][face]), defaults);
			if(scheme[face] == null)
				scheme[face] = Utils.stringToColor(FACE_NAMES_COLORS[1][face], false);
		}
		return scheme;
	}

	public int getDefaultUnitSize() {
		return DEFAULT_UNIT_SIZE;
	}
}
