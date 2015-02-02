package net.gnehzr.cct.scrambles;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.ScramblePluginMessages;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ScramblePlugin {

	private static final Logger LOG = Logger.getLogger(ScramblePlugin.class);

	private static final Random r = new Random();

	private final String puzzleName;
	private final ScramblePluginMessages messageAccessor;

	private final boolean supportsScrambleImage;

	public abstract ScrambleString importScramble(final ScrambleVariation.WithoutLength variation, final String scramble,
												  final String generatorGroup,
												  final List<String> attributes) throws InvalidScrambleException;

	public abstract ScrambleString createScramble(final ScrambleVariation variation,
													 final String generatorGroup, final List<String> attributes);

	protected int parseSize(String scramble) {
		return scramble.trim().split("\\s+").length;
	}

	public final boolean supportsScrambleImage() {
		return supportsScrambleImage;
	}

	public ScramblePluginMessages getMessageAccessor() {
		return messageAccessor;
	}

	public List<String> getEnabledPuzzleAttributes(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		if (scramblePluginManager.getAttributes() == null) {
			scramblePluginManager.setAttributes(configuration.getStringArray(VariableKey.PUZZLE_ATTRIBUTES(this), false));
			if(scramblePluginManager.getAttributes() == null) {
				scramblePluginManager.setAttributes(getDefaultAttributes());
			}
		}
		return scramblePluginManager.getAttributes();
	}

	public ScramblePlugin(String puzzleName, boolean supportsScrambleImage) {
		this.puzzleName = puzzleName;
		this.supportsScrambleImage = supportsScrambleImage;
		this.messageAccessor = new ScramblePluginMessages(getClass());
	}

	protected static int random(int choices) {
		return r.nextInt(choices);
	}

	//assumes m > 0
	protected static int modulo(int x, int m) {
		int y = x % m;
		if(y >= 0) return y;
		return y + m;
	}
	public BufferedImage getScrambleImage(ScrambleString scrambleString, int gap, int cubieSize, Map<String, Color> colorScheme) {
		return null;
	}

	public abstract int getNewUnitSize(int width, int height, int gap, String variation);

	public abstract Dimension getImageSize(int gap, int minxRad, String variation);

	public abstract Map<String, Shape> getFaces(int gap, int pieceSize, String variation);

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

	@NotNull
	public abstract Map<String, Color> getFaceNamesColors();

	public abstract int getDefaultUnitSize();

	/**
	 * This is so one class can handle 3x3x3-11x11x11, variations cannot contain the character ":"
	 * @return variation names
	 */
	@NotNull
	public abstract List<String> getVariations();

	/**
	 * HIGHLY RECOMMENDED, defines default lengths for each element of VARIATIONS (make it a one element array unless you defined VARIATIONS)
	 * @return array with default scramble lengths for each variations
	 */
	@NotNull
	protected abstract int[] getDefaultLengths();

	void checkPluginState() {
		if(getPuzzleName() == null) {
			throw new NullPointerException("puzzle name may not be null!");
		}
		if(getPuzzleName().contains(":")) {
			throw new IllegalArgumentException("PUZZLE_NAME (" + getPuzzleName() + ") may not contain ':'!");
		}
		Objects.requireNonNull(getFaceNamesColors());

		for(String var : getVariations()) {
			if(var == null || var.isEmpty()) {
				throw new NullPointerException("Scramble variations may not be null or the empty string!");
			}
			if(var.contains(":")) {
				throw new IllegalArgumentException("Scramble variation (" + var + ") may not contain ':'!");
			}
		}
		Objects.requireNonNull(getDefaultLengths(), "DEFAULT_LENGTHS may not be null!");
		//there's no need to deal w/ negative lengths here, we'll deal with it later

		if(getVariations().size() != 0 && getVariations().size() != getDefaultLengths().length) {
			throw new ArrayIndexOutOfBoundsException("VARIATIONS.length (" + getVariations().size() + ") != DEFAULT_LENGTHS.length (" + getDefaultLengths().length + ")");
		}

		for(String c : getAttributes()) {
			checkArgument(!Strings.isNullOrEmpty(c), "Attributes may not be null or empty!");
		}

		Objects.requireNonNull(getDefaultAttributes());

		for(String c : getDefaultAttributes()) {
			if(c == null || c.isEmpty()) {
				throw new IllegalArgumentException("Default attributes may not be null or empty!");
			}

			if(!getAttributes().contains(c)) {
				//indicates that this default attribute wasn't found in ATTRIBUTES
				throw new IllegalArgumentException("Default attribute (" + c + ") not found in ATTRIBUTES!");
			}
		}
		if (getVariations().size() != 0 && getDefaultGenerators().size() > getVariations().size()) {
			throw new ArrayIndexOutOfBoundsException(getClass().getSimpleName()
					+ ": DEFAULT_GENERATORS.length (" + getDefaultGenerators().size() + ") > VARIATIONS.length (" + getVariations().size() + ")");
		}
		Sets.SetView<String> difference = Sets.difference(getDefaultGenerators().keySet(), Sets.newHashSet(getVariations()));
		if (!difference.isEmpty()) {
			throw new IllegalArgumentException(getClass().getSimpleName() + ": has generators for unknown variations" + difference);
		}
	}

	@NotNull
	public abstract List<String> getAttributes();

	@NotNull
	public abstract List<String> getDefaultAttributes();

	public abstract Pattern getTokenRegex();

	/**
	 *
	 * @return variation -> generator
	 */
	@NotNull
	public abstract Map<String, String> getDefaultGenerators();

	public final String toString() {
		return "Plugin{" + puzzleName + "}";
	}
}
