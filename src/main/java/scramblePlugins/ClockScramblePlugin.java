package scramblePlugins;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.scrambles.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ClockScramblePlugin extends ScramblePlugin {

	private static final String PUZZLE_NAME = "Clock";
	private static final List<String> VARIATIONS = ImmutableList.of("Clock");
	private static final int[] DEFAULT_LENGTHS = {10};
	private static final List<String> ATTRIBUTES = ImmutableList.of("i18n[verbose]");

	private boolean verbose;

	@SuppressWarnings("UnusedDeclaration")
	public ClockScramblePlugin() throws InvalidScrambleException {
		super(PUZZLE_NAME, false);
		verbose = false;
	}

	@Override
	public ScrambleString importScramble(PuzzleType puzzleType, ScrambleSettings.WithoutLength variation, String scramble,
										 List<String> attributes) throws InvalidScrambleException {
		verbose = hasVerboseAttribute(attributes);
		return new ScrambleString(puzzleType, scramble, true, variation.withLength(parseSize(scramble)), this, null);
	}

	@Override
	public ScrambleString createScramble(PuzzleType puzzleType, ScrambleSettings variation, List<String> attributes) {
		verbose = hasVerboseAttribute(attributes);

		return new ScrambleString(puzzleType, generateScramble(), false, variation, this, null);
	}

	private boolean hasVerboseAttribute(List<String> attributes) {
		return attributes.contains("i18n[verbose]");
	}

	@Override
	public Dimension getImageSize(int gap, int minxRad, String variation) {
		return NULL_SCRAMBLE_PLUGIN.getImageSize(gap, minxRad, variation);
	}

	@Override
	public int getNewUnitSize(int width, int height, int gap, String variation) {
		return NULL_SCRAMBLE_PLUGIN.getNewUnitSize(width, height, gap, variation);
	}

	@Override
	public Map<String, Shape> getFaces(int gap, int pieceSize, String variation) {
		return NULL_SCRAMBLE_PLUGIN.getFaces(gap, pieceSize, variation);
	}

	@Override
	public String htmlify(String formatMe) {
		return formatMe;
	}

	@NotNull
	@Override
	public Map<String, Color> getFaceNamesColors() {
		return NULL_SCRAMBLE_PLUGIN.getFaceNamesColors();
	}

	@Override
	public int getDefaultUnitSize() {
		return NULL_SCRAMBLE_PLUGIN.getDefaultUnitSize();
	}

	@NotNull
	@Override
	public List<String> getVariations() {
		return VARIATIONS;
	}

	@NotNull
	@Override
	protected int[] getDefaultLengths() {
		return DEFAULT_LENGTHS;
	}

	@NotNull
	@Override
	public List<String> getAttributes() {
		return ATTRIBUTES;
	}

	@NotNull
	@Override
	public List<String> getDefaultAttributes() {
		return NULL_SCRAMBLE_PLUGIN.getDefaultAttributes();
	}

	@Override
	public Pattern getTokenRegex() {
		return null;
	}

	@NotNull
	@Override
	public Map<String, String> getDefaultGenerators() {
		return NULL_SCRAMBLE_PLUGIN.getDefaultGenerators();
	}

	private String generateScramble(){
		StringBuilder scramble = new StringBuilder();
		String[] peg={"U","d"};
		String[] pegs={"UUdd ","dUdU ","ddUU ","UdUd "};
		String[] upegs={"dUUU ","UdUU ","UUUd ","UUdU ","UUUU "};

		for(int x=0; x<4; x++){
			if (verbose){
				scramble.append(pegs[x]);
			}
			scramble.append("u=").append(random(12)-5).append(",d=").append(random(12)-5).append(" / ");
		}
		for(int x=0;x<5; x++){
			if (verbose){	
				scramble.append(upegs[x]);
			}
			scramble.append("u=").append(random(12)-5).append(" / ");
		}
		if (verbose){
			scramble.append("dddd ");
		}
		scramble.append("d=").append(random(12)-5).append(" / ");
		for (int x=0;x<4;x++){
			scramble.append(peg[random(2)]);
		}

		return scramble.toString();
	}
}

