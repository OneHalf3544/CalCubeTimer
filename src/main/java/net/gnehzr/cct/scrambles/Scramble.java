package net.gnehzr.cct.scrambles;

import com.google.common.base.Strings;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.ScramblePluginMessages;
import net.gnehzr.cct.i18n.StringAccessor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class Scramble {

	private static final Logger LOG = Logger.getLogger(Scramble.class);

	public static final Scramble NULL_SCRAMBLE = new NullScramble("");

	private final String puzzleName;
	private final ScramblePluginMessages messageAccessor;
	private final boolean supportsScrambleImage;

	public final Scramble newScramble(String variation, int length, String generatorGroup, List<String> attributes) {
		try {
			return createScramble(variation, length, generatorGroup, attributes);
		} catch (Throwable e) {
			LOG.info("unexpected exception", e);
			return Scramble.NULL_SCRAMBLE;
		}
	}

	public abstract Scramble importScramble(final String variation, final String scramble, final String generatorGroup,
								   final List<String> attributes) throws InvalidScrambleException;


	protected abstract Scramble createScramble(final String variation, final int length, final String generatorGroup, final List<String> attributes);

	public final boolean supportsScrambleImage() {
		return supportsScrambleImage;
	}

	public ScramblePluginMessages getMessageAccessor() {
		return messageAccessor;
	}

	public List<String> getEnabledPuzzleAttributes(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		if(scramblePluginManager.getAttributes() == null) {
			scramblePluginManager.setAttributes(configuration.getStringArray(VariableKey.PUZZLE_ATTRIBUTES(this), false));
			if(scramblePluginManager.getAttributes() == null) {
				scramblePluginManager.setAttributes(getDefaultAttributes());
			}
		}
		return scramblePluginManager.getAttributes();
	}

	public static class InvalidScrambleException extends Exception {
		public InvalidScrambleException(String scramble) {
			super(StringAccessor.getString("InvalidScrambleException.invalidscramble") + "\n" + scramble);
		}
	}
	
	protected int length = 0;
	protected String scramble = null;

	public Scramble(String puzzleName, boolean supportsScrambleImage) {
		this.puzzleName = puzzleName;
		this.supportsScrambleImage = supportsScrambleImage;
		this.messageAccessor = new ScramblePluginMessages(getClass());
	}

	public Scramble(String puzzleName, boolean supportsScrambleImage, String scramble) {
		this(puzzleName, supportsScrambleImage);
		this.scramble = scramble;
	}

	public final int getLength() {
		return length;
	}
	
	public final String toString() {
		return scramble;
	}

	private static final Random r = new Random();
	protected static int random(int choices) {
		return r.nextInt(choices);
	}
	//assumes m > 0
	protected static int modulo(int x, int m) {
		int y = x % m;
		if(y >= 0) return y;
		return y + m;
	}

	public BufferedImage getScrambleImage(int gap, int cubieSize, Color[] colorScheme) {
		return null;
	}

	public abstract int getNewUnitSize(int width, int height, int gap, String variation);

	public abstract Dimension getImageSize(int gap, int minxRad, String variation);

	public abstract Shape[] getFaces(int gap, int pieceSize, String variation);

	/**
	 * This adds html formatting to a scramble for display purposes
	 * @param formatMe plain/text string to format
	 * @return html-wrapped text
	 */
	public abstract String htmlify(String formatMe);

	/**
	 * Name cannot contain the character ":"
	 * @return puzzle name.
	 */
	public final String getPuzzleName() {
		return puzzleName;
	}

	protected abstract String[][] getFaceNamesColors();

	public abstract int getDefaultUnitSize();

	/**
	 * This is so one class can handle 3x3x3-11x11x11, variations cannot contain the character ":"
	 * @return variation names
	 */
	@NotNull
	public abstract String[] getVariations();

	/**
	 * HIGHLY RECOMMENDED, defines default lengths for each element of VARIATIONS (make it a one element array unless you defined VARIATIONS)
	 * @return array with default scramble lengths for each variations
	 */
	@NotNull
	protected abstract int[] getDefaultLengths();

	void checkPluginState() {
		if(getPuzzleName() == null) {
			throw new NullPointerException("PUZZ LE_NAME may not be null!");
		}
		if(getPuzzleName().indexOf(':') != -1) {
			throw new IllegalArgumentException("PUZZLE_NAME (" + getPuzzleName() + ") may not contain ':'!");
		}
		if(getFaceNamesColors() != null) {
			if(getFaceNamesColors().length != 2) {
				throw new ArrayIndexOutOfBoundsException("FACE_NAMES_COLORS.length (" + getFaceNamesColors().length + ") does not equal 2!");
			}
			if(getFaceNamesColors()[0].length != getFaceNamesColors()[1].length)
				throw new ArrayIndexOutOfBoundsException("FACE_NAMES_COLORS[0].length (" + getFaceNamesColors()[0].length + ") != FACE_NAMES_COLORS[1].length (" + getFaceNamesColors()[1].length + ")");
		}
		for(String var : getVariations()) {
			if(var == null || var.isEmpty()) {
				throw new NullPointerException("Scramble variations may not be null or the empty string!");
			}
			if(var.indexOf(':') != -1) {
				throw new IllegalArgumentException("Scramble variation (" + var + ") may not contain ':'!");
			}
		}
		Objects.requireNonNull(getDefaultLengths(), "DEFAULT_LENGTHS may not be null!");
		//there's no need to deal w/ negative lengths here, we'll deal with it later

		if(getVariations().length != 0 && getVariations().length != getDefaultLengths().length) {
			throw new ArrayIndexOutOfBoundsException("VARIATIONS.length (" + getVariations().length + ") != DEFAULT_LENGTHS.length (" + getDefaultLengths().length + ")");
		}

		for(String c : getAttributes()) {
			checkArgument(!Strings.isNullOrEmpty(c), "Attributes may not be null or empty!");
		}

		Objects.requireNonNull(getDefaultAttributes());

		for(String c : getDefaultAttributes()) {
			if(c == null || c.isEmpty())
				throw new IllegalArgumentException("Default attributes may not be null or empty!");
			int ch;
			for(ch = 0; ch < getAttributes().size(); ch++)
				if(c.equals(getAttributes().get(ch)))
					break;
			if(ch == getAttributes().size()) //indicates that this default attribute wasn't found in ATTRIBUTES
			{
				throw new IllegalArgumentException("Default attribute (" + c + ") not found in ATTRIBUTES!");
			}
		}
		if (getDefaultGenerators() != null && getVariations().length != 0 && getDefaultGenerators().length != getVariations().length) {
			throw new ArrayIndexOutOfBoundsException(getClass().getSimpleName()
					+ ": DEFAULT_GENERATORS.length (" + getDefaultGenerators().length + ") != VARIATIONS.length (" + getVariations().length + ")");
		}
	}

	@NotNull
	public abstract List<String> getAttributes();

	@NotNull
	public abstract List<String> getDefaultAttributes();

	public abstract Pattern getTokenRegex();

	@Nullable
	public abstract String[] getDefaultGenerators();

	//TODO - this is poorly named, and for now, exists solely for optimal cross solutions for CubeScramble
	public String getTextComments() {
		return null;
	}
}
