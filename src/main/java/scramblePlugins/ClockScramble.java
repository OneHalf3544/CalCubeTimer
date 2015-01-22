package scramblePlugins;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.scrambles.Scramble;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ClockScramble extends Scramble {

	private static final String[][] FACE_NAMES_COLORS = {{},{}};
	private static final String PUZZLE_NAME = "Clock";
	private static final String[] VARIATIONS = { "Clock" };
	private static final int[] DEFAULT_LENGTHS = {10};
	private static final List<String> ATTRIBUTES = ImmutableList.of("%%verbose%%");
//	private static final Pattern TOKEN_REGEX = Pattern.compile("^(\\S)(.*)$");
	private boolean verbose;

	public ClockScramble() throws InvalidScrambleException {
		this("", Collections.<String>emptyList());
	}

	public ClockScramble(String s, List<String> attrs) throws InvalidScrambleException {
		super(s);
		if(!setAttributes(attrs)) {
			throw new InvalidScrambleException(s);
		}
	}

	public ClockScramble(int length, List<String> attrs) {
		super(PUZZLE_NAME);
		this.length = length;
		setAttributes(attrs);
	}

	@Override
	public Scramble importScramble(String variation, String scramble, String generatorGroup, List<String> attributes) throws InvalidScrambleException {
		return new ClockScramble(scramble, attributes);
	}

	@Override
	protected Scramble createScramble(String variation, int length, String generatorGroup, List<String> attributes) {
		return new ClockScramble(length, attributes);
	}

	@Override
	public boolean supportsScrambleImage() {
		return false;
	}

	@Override
	public Dimension getImageSize(int gap, int minxRad, String variation) {
		return NULL_SCRAMBLE.getImageSize(gap, minxRad, variation);
	}

	@Override
	public int getNewUnitSize(int width, int height, int gap, String variation) {
		return NULL_SCRAMBLE.getNewUnitSize(width, height, gap, variation);
	}

	@Override
	public Shape[] getFaces(int gap, int pieceSize, String variation) {
		return NULL_SCRAMBLE.getFaces(gap, pieceSize, variation);
	}

	@Override
	public String htmlify(String formatMe) {
		return formatMe;
	}

	@Override
	protected String[][] getFaceNamesColors() {
		return FACE_NAMES_COLORS;
	}

	@Override
	public int getDefaultUnitSize() {
		return NULL_SCRAMBLE.getDefaultUnitSize();
	}

	@NotNull
	@Override
	public String[] getVariations() {
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
		return NULL_SCRAMBLE.getDefaultAttributes();
	}

	@Override
	public Pattern getTokenRegex() {
		return null;
	}

	@Override
	public String[] getDefaultGenerators() {
		return NULL_SCRAMBLE.getDefaultGenerators();
	}

	private boolean setAttributes(List<String> attributes){
		verbose=false;
		for(String attr : attributes) {
			if (attr.equals(ATTRIBUTES.get(0))) {
				verbose = true;
			}
		}
		if(scramble != null) {
			return validateScramble();
		}
		generateScramble();
		return true;
	}


	private boolean validateScramble() {
		return true;
	}
	
	private void generateScramble(){
		scramble = "";
		StringBuilder scram = new StringBuilder();
		String[] peg={"U","d"};
		String[] pegs={"UUdd ","dUdU ","ddUU ","UdUd "};
		String[] upegs={"dUUU ","UdUU ","UUUd ","UUdU ","UUUU "};
		for(int x=0; x<4; x++){
			if (verbose){
				scram.append(pegs[x]);
			}
			scram.append("u=").append(random(12)-5).append(",d=").append(random(12)-5).append(" / ");
		}
		for(int x=0;x<5; x++){
			if (verbose){	
				scram.append(upegs[x]);
			}
			scram.append("u=").append(random(12)-5).append(" / ");
		}
		if (verbose){
			scram.append("dddd ");
		}
		scram.append("d=").append(random(12)-5).append(" / ");
		for(int x=0;x<4;x++){
			scram.append(peg[random(2)]);
		}
		
		if(scram.length() > 0)
			scramble = scram.substring(0);
	}
}

