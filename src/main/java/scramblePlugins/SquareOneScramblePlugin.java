package scramblePlugins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.*;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SquareOneScramblePlugin extends ScramblePlugin {

	private static final Pattern GENERATOR = Pattern.compile("\\( *(.+), *(.+) *\\)");
	private static final Map<String, Color> FACE_NAMES_COLORS = ImmutableMap.<String, Color>builder()
			.put("L", Utils.stringToColor("ffff00"))
			.put("B", Utils.stringToColor("ff0000"))
			.put("R", Utils.stringToColor("0000ff"))
			.put("F", Utils.stringToColor("ffc800"))
			.put("U", Utils.stringToColor("ffffff"))
			.put("D", Utils.stringToColor("00ff00"))
			.build();

	private static final String FACES_ORDER = "LBRFUD";
	private static final String PUZZLE_NAME = "Square-1";
	public static final String SLASHES_ATTRIBUTE = "i18n[slashes]";
	private static final int[] DEFAULT_LENGTHS = { 40 };
	private static final int DEFAULT_UNIT_SIZE = 32;
	private static final Pattern TOKEN_REGEX = Pattern.compile("^(\\( *-?\\d+ *, *-?\\d+ *\\)|/)(.*)$");
	private static final ImmutableList<String> ATTRIBUTES = ImmutableList.of(SLASHES_ATTRIBUTE);
	private static final Map<String, String> DEFAULT_GENERATORS = ImmutableMap.of(PUZZLE_NAME, "(x, x) /");
	private static final double RADIUS_MULTIPLIER = Math.sqrt(2) * Math.cos(Math.toRadians(15));
	private static final Pattern regexp = Pattern.compile("^ *(-?\\d+) *, *(-?\\d+) *$");
	private static final double multiplier = 1.4;

	public static final String FRONT = "F";
	public static final String BACK = "B";

	static class State {
		private final boolean turnTop;
		private final boolean turnBottom;

		private int twistCount = 0; //this will tell us the state of the middle pieces
		private final int[] state = new int[]{ 0,0,1,2,2,3,4,4,5,6,6,7,8,9,9,10,11,11,12,13,13,14,15,15 };;

		public State(Tuple2<Boolean, Boolean> generator) {
			turnTop = generator.v1;
			turnBottom = generator.v2;
		}
	}

	@SuppressWarnings("unused")
	public SquareOneScramblePlugin() {
		super(PUZZLE_NAME, true);
	}

	@Override
	public ScrambleString importScramble(PuzzleType puzzleType, ScrambleSettings.WithoutLength variation, String scramble,
										 List<String> attributes) throws InvalidScrambleException {
		boolean slashes = attributes.contains(SLASHES_ATTRIBUTE);
		Tuple2<Boolean, Boolean> generator = parseGenerator(variation.getGeneratorGroup());
		State state = new State(generator);
		OptionalInt length = validateScrambleAndGetLength(state, scramble, slashes);
		if(!length.isPresent()) {
			throw new InvalidScrambleException(scramble);
		}
		return new ScrambleString(puzzleType, scramble, true, variation.withLength(length.getAsInt()), this, null);
	}

	@Override
	public ScrambleString createScramble(PuzzleType puzzleType, ScrambleSettings variation, List<String> attributes) {
		boolean slashes = attributes.contains(SLASHES_ATTRIBUTE);
		Tuple2<Boolean, Boolean> generator = parseGenerator(variation.getGeneratorGroup());
		return new ScrambleString(puzzleType, generateScramble(variation.getLength(), slashes, new State(generator)), false, variation, this, null);
	}

	@Override
	public String htmlify(String formatMe) {
		return formatMe;
	}

	@NotNull
	@Override
	public Map<String, Color> getFaceNamesColors() {
		return FACE_NAMES_COLORS;
	}

	@Override
	public int getDefaultUnitSize() {
		return DEFAULT_UNIT_SIZE;
	}

	@NotNull
	@Override
	public List<String> getVariations() {
		return ImmutableList.of(PUZZLE_NAME);
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
		return TOKEN_REGEX;
	}

	@NotNull
	@Override
	public Map<String, String> getDefaultGenerators() {
		return DEFAULT_GENERATORS;
	}

	private Tuple2<Boolean, Boolean> parseGenerator(String generator) {
		if(generator == null) {
			return new Tuple2<>(true, true);
		}
		Matcher m = GENERATOR.matcher(generator);
		if(m.find()) {
			return new Tuple2<>(
					!m.group(1).equals("0"),
					!m.group(2).equals("0"));
		}
		return new Tuple2<>(true, true);
	}

	//Ported from http://www.worldcubeassociation.org/regulations/scrambles/scramble_square1.htm by Jeremy Fleischman
	/* Javascript written by Jaap Scherphuis,  jaapsch@yahoo.com */

	private String generateScramble(int length, boolean slashes, State state) {
		int[] turns = new int[length];
		int move;
		int ls = -1;
		for(int i = 0; i < length; i++) {
			do {
				if(ls==0) {
					move=random(22)-11;
					if(move>=0) move++;
				} else if(ls==1) {
					move=random(12)-11;
				} else if(ls==2) {
					move=0;
				} else {
					move=random(23)-11;
				}
				//we don't want to apply to restriction to the bottom if we're no allowed to turn the top
				//if we did, we might loop forever making scrambles for a bandaged square 1
			} while( (state.turnTop && state.twistCount>1 && move>=-6 && move<0) || domove(state, i, move, turns));
			if(move>0) ls=1;
			else if(move<0) ls=2;
			else { ls=0; }
		}
		return finalizeScrambleString(slashes, turns);
	}

	private String finalizeScrambleString(boolean slashes, int[] turns) {
		StringBuilder scram = new StringBuilder();
		int l=-1;
		for (int k : turns) {
			if (k == 0) {
				switch (l) {
					case -1:
						scram.append(" (0,0)");
						break;
					case 1:
						scram.append("0)");
						break;
					case 2:
						scram.append(")");
						break;
				}
				if (l != 0 && slashes) {
					scram.append(" /");
				}
				l = 0;
			} else if (k > 0) {
				scram.append(" (").append(k > 6 ? k - 12 : k).append(",");
				l = 1;
			} else if (k < 0) {
				if (l <= 0) scram.append(" (0,");
				scram.append(k <= -6 ? k + 12 : k);
				l = 2;
			}
		}
		if(l==1) scram.append("0");
		if(l!=0) scram.append(")");
		return scram.length() > 0 ? scram.substring(1) : "";
	}
	
	//returns true if invalid, false if valid
	private boolean domove(State state, int index, int m, int[] turns) {
		int i,c,f=m;
		//do move f
		if(f == 0) { //slash
			for(i = 0; i < 6; i++){
				c = state.state[i+12];
				state.state[i+12] = state.state[i+6];
				state.state[i+6] = c;
			}
			state.twistCount++;
		} else if(f > 0) { //turn the top
			if(!state.turnTop) return true;
			f=modulo(12-f, 12);
			if( state.state[f]==state.state[f-1] ) return true;
			if( f<6 && state.state[f+6]==state.state[f+5] ) return true;
			if( f>6 && state.state[f-6]==state.state[f-7] ) return true;
			if( f==6 && state.state[0]==state.state[11] ) return true;
			int[] t = new int[12];
			for(i=0;i<12;i++) t[i]=state.state[i];
			c=f;
			for(i=0;i<12;i++){
				state.state[i] = t[c];
				if(c == 11)c=0; else c++;
			}
		} else if(f < 0) { //turn the bottom
			if(!state.turnBottom) return true;
			f=modulo(-f, 12);
			if( state.state[f+12]==state.state[f+11] ) return true;
			if( f<6 && state.state[f+18]==state.state[f+17] ) return true;
			if( f>6 && state.state[f+6]==state.state[f+5] ) return true;
			if( f==6 && state.state[12]==state.state[23] ) return true;
			int[] t = new int[12];
			for(i=0;i<12;i++) t[i]=state.state[i+12];
			c=f;
			for(i=0;i<12;i++){
				state.state[i+12]=t[c];
				if(c==11)c=0; else c++;
			}
		}
		turns[index]=m;
		return false;
	}
	//**********END JAAP's CODE***************

	private OptionalInt validateScrambleAndGetLength(State state, String scramble, boolean slashes) {
		//if there is no slash in the scramble, we assume that we're using implicit slashes
		boolean implicitSlashes = scramble.indexOf('/') == -1;
		//however, to get correct incremental scramble behavior, we will use the set attribute if
		//there is only one set of parens
		if(scramble.indexOf('(') == scramble.lastIndexOf('('))
			implicitSlashes = !slashes;

		int length = 0;
		String[] turns = scramble.split("(\\(|\\)|\\( *\\))", -1);
		int[] thisturns = new int[turns.length*3]; //definitely big enough, no need to trim
		for (String trn : turns) {
			Matcher match;
			if (trn.matches(" *")) {
				continue;
			}
			if (trn.matches(" */ *")) {
				domove(state, length++, 0, thisturns);
			} else {
				if ((match = regexp.matcher(trn)).matches()) {
					int top = Integer.parseInt(match.group(1));
					int bot = Integer.parseInt(match.group(2));
					top = modulo(top, 12);
					bot = modulo(bot, 12);
					if (top != 0 && domove(state, length++, top, thisturns)) {
						return OptionalInt.empty();
					}
					if (bot != 0 && domove(state, length++, bot - 12, thisturns)) {
						return OptionalInt.empty();
					}
					if (implicitSlashes)
						domove(state, length++, 0, thisturns);
				} else {
					return OptionalInt.empty();
				}
			}
		}
		finalizeScrambleString(slashes, thisturns);
		return OptionalInt.of(length);
	}

	@Override
	public BufferedImage getScrambleImage(ScrambleString scrambleString, int gap, int radius, Map<String, Color> colorScheme) {
		return createImage(scrambleString, gap, radius, colorScheme, (state, scramble) -> validateScrambleAndGetLength(state, scrambleString.getScramble(), /*todo*/ false));
	}

	@Override
	public BufferedImage getDefaultStateImage(PuzzleType puzzleType, int gap, int finalUnitSize, Map<String, Color> colorScheme) {
		return createImage(null, gap, finalUnitSize, colorScheme, (state, scramble) -> {});
	}

	@NotNull
	private BufferedImage createImage(ScrambleString scrambleString, int gap, int radius, Map<String, Color> colorScheme, BiConsumer<State, ScrambleString> scrambleValidator) {
		State state = new State(new Tuple2<>(true, true));
		scrambleValidator.accept(state, scrambleString);
		Dimension dim = getImageSize(gap, radius, null);
		int width = dim.width;
		int height = dim.height;
		BufferedImage buffer = new BufferedImage(width, height,	BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = buffer.createGraphics();
		double half_square_width = (radius * RADIUS_MULTIPLIER * multiplier) / Math.sqrt(2);
		double edge_width = 2 * radius * multiplier * Math.sin(Math.toRadians(15));
		double corner_width = half_square_width - edge_width / 2.;
		Rectangle2D.Double left_mid = new Rectangle2D.Double(width / 2. - half_square_width, height / 2. - radius * (multiplier - 1) / 2., corner_width, radius * (multiplier - 1));
		Rectangle2D.Double right_mid;
		if(state.twistCount % 2 == 0) {
			right_mid = new Rectangle2D.Double(width / 2. - half_square_width, height / 2. - radius * (multiplier - 1) / 2., 2*corner_width + edge_width, radius * (multiplier - 1));
			g.setColor(colorScheme.get(FRONT));
		} else {
			right_mid = new Rectangle2D.Double(width / 2. - half_square_width, height / 2. - radius * (multiplier - 1) / 2., corner_width + edge_width, radius * (multiplier - 1));
			g.setColor(colorScheme.get(BACK));
		}
		g.fill(right_mid);
		g.setColor(colorScheme.get(FRONT));
		g.fill(left_mid); //this will clobber part of the other guy
		g.setColor(Color.BLACK);
		g.draw(right_mid);
		g.draw(left_mid);

		double x = width / 2.0;
		double y = height / 4.0;
		g.rotate(Math.toRadians(90 + 15), x, y);
		drawFace(g, state.state, x, y, gap, radius, colorScheme);
		g.dispose();

		y *= 3.0;
		g = buffer.createGraphics();
		g.rotate(Math.toRadians(-90 - 15), x, y);
		drawFace(g, Arrays.copyOfRange(state.state, 12, state.state.length), x, y, gap, radius, colorScheme);
		g.dispose();
		return buffer;
	}

	private void drawFace(Graphics2D g, int[] face, double x, double y, int gap, int radius, Map<String, Color> colorScheme) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		for(int ch = 0; ch < 12; ch++) {
			if(ch < 11 && face[ch] == face[ch+1])
				ch++;
			drawPiece(g, face[ch], x, y, gap, radius, colorScheme);
		}
		g.dispose();
	}

	private int drawPiece(Graphics2D g, int piece, double x, double y, int gap,	int radius, Map<String, Color> colorScheme) {
		boolean corner = isCornerPiece(piece);
		int degree = 30 * (corner ? 2 : 1);
		GeneralPath[] p = corner ? getCornerPoly(x, y, radius) : getWedgePoly(x, y, radius);

		Color[] cls = getPieceColors(piece, colorScheme);
		for(int ch = cls.length - 1; ch >= 0; ch--) {
			g.setColor(cls[ch]);
			g.fill(p[ch]);
			g.setColor(Color.BLACK);
			g.draw(p[ch]);
		}
		g.rotate(Math.toRadians(degree), x, y);
		return degree;
	}

	private boolean isCornerPiece(int piece) {
		return ((piece + (piece <= 7 ? 0 : 1)) % 2) == 0;
	}

	private Color[] getPieceColors(int piece, Map<String, Color> colorScheme) {
		boolean up = piece <= 7;
		Color top = up ? getColorByIndex(colorScheme, 4) : getColorByIndex(colorScheme, 5);
		if(isCornerPiece(piece)) { //corner piece
			if(!up)
				piece = 15 - piece;
			Color a = getColorByIndex(colorScheme, (piece / 2 + 3) % 4);
			Color b = getColorByIndex(colorScheme, piece / 2);
			if(!up) { //mirror for bottom
				Color t = a;
				a = b;
				b = t;
			}
			return new Color[] { top, a, b }; //ordered counter-clockwise
		} else { //wedge piece
			if(!up)
				piece = 14 - piece;
			return new Color[]{top, getColorByIndex(colorScheme, piece / 2)};
		}
	}

	private Color getColorByIndex(Map<String, Color> colorScheme, int i) {
		return colorScheme.get(String.valueOf(FACES_ORDER.charAt(i)));
	}

	private GeneralPath[] getWedgePoly(double x, double y, int radius) {
		AffineTransform trans = AffineTransform.getTranslateInstance(x, y);
		GeneralPath p = new GeneralPath();
		p.moveTo(0, 0);
		p.lineTo(radius, 0);
		double tempx = Math.sqrt(3) * radius / 2.0;
		double tempy = radius / 2.0;
		p.lineTo(tempx, tempy);
		p.closePath();
		p.transform(trans);

		GeneralPath side = new GeneralPath();
		side.moveTo(radius, 0);
		side.lineTo(multiplier * radius, 0);
		side.lineTo(multiplier * tempx, multiplier * tempy);
		side.lineTo(tempx, tempy);
		side.closePath();
		side.transform(trans);
		return new GeneralPath[]{ p, side };
	}
	private GeneralPath[] getCornerPoly(double x, double y, int radius) {
		AffineTransform trans = AffineTransform.getTranslateInstance(x, y);
		GeneralPath p = new GeneralPath();
		p.moveTo(0, 0);
		p.lineTo(radius, 0);
		double tempx = radius*(1 + Math.cos(Math.toRadians(75))/Math.sqrt(2));
		double tempy = radius*Math.sin(Math.toRadians(75))/Math.sqrt(2);
		p.lineTo(tempx, tempy);
		double tempX = radius / 2.0;
		double tempY = Math.sqrt(3) * radius / 2.0;
		p.lineTo(tempX, tempY);
		p.closePath();
		p.transform(trans);

		GeneralPath side1 = new GeneralPath();
		side1.moveTo(radius, 0);
		side1.lineTo(multiplier * radius, 0);
		side1.lineTo(multiplier * tempx, multiplier * tempy);
		side1.lineTo(tempx, tempy);
		side1.closePath();
		side1.transform(trans);

		GeneralPath side2 = new GeneralPath();
		side2.moveTo(multiplier * tempx, multiplier * tempy);
		side2.lineTo(tempx, tempy);
		side2.lineTo(tempX, tempY);
		side2.lineTo(multiplier * tempX, multiplier * tempY);
		side2.closePath();
		side2.transform(trans);
		return new GeneralPath[]{ p, side1, side2 };
	}

	@Override
	public Dimension getImageSize(int gap, int radius, String variation) {
		return new Dimension(getWidth(gap, radius), getHeight(gap, radius));
	}

	private static int getWidth(int gap, int radius) {
		return (int) (2 * RADIUS_MULTIPLIER * multiplier * radius);
	}

	private static int getHeight(int gap, int radius) {
		return (int) (4 * RADIUS_MULTIPLIER * multiplier * radius);
	}

	@Override
	public int getNewUnitSize(int width, int height, int gap, String variation) {
		return (int) Math.round(Math.min(width / (2 * RADIUS_MULTIPLIER * multiplier), height / (4 * RADIUS_MULTIPLIER * multiplier)));
	}

	//***NOTE*** this works only for the simple case where the puzzle is a cube
	@Override
	public Map<String, Shape> getFaces(int gap, int radius, String variation) {
		int width = getWidth(gap, radius);
		int height = getHeight(gap, radius);
		double half_width = (radius * RADIUS_MULTIPLIER) / Math.sqrt(2);
		
		Area up = getSquare(width / 2.0, height / 4.0, half_width);
		Area down = getSquare(width / 2.0, 3 * height / 4.0, half_width);
		Area front = new Area(new Rectangle2D.Double(width / 2. - half_width * multiplier, height / 2. - radius * (multiplier - 1) / 2., 2 * half_width * multiplier, radius * (multiplier - 1)));
		
		Map<String, Shape> faces = new HashMap<>(6);
		for(char ch : FACES_ORDER.substring(0, 4).toCharArray()) {
			Area value = new Area();
			value.add(getTri(width / 2.0, height / 4.0, 2 * half_width * multiplier, (5 - ch) % 4));
			value.add(getTri(width / 2.0, 3 * height / 4.0, 2 * half_width * multiplier, (ch + 1) % 4));
			value.subtract(up);
			value.subtract(down);
			faces.put(Character.toString(ch), value);
		}
		((Area)faces.get(FRONT)).add(front);
		faces.put("U", up);
		faces.put("D", down);
		return faces;
	}
	//x, y are the coordinates of the center of the square
	private static Area getSquare(double x, double y, double half_width) {
		return new Area(new Rectangle2D.Double(x - half_width, y - half_width, 2 * half_width, 2 * half_width));
	}
	//type is the orientation of the triangle, in multiples of 90 degrees ccw
	private static Area getTri(double x, double y, double width, int type) {
		GeneralPath tri = new GeneralPath();
		tri.moveTo(width / 2.0, width / 2.0);
		tri.lineTo((type == 3) ? width : 0, (type < 2) ? 0 : width);
		tri.lineTo((type == 1) ? 0 : width, (type % 3 == 0) ? 0 : width);
		tri.closePath();
		tri.transform(AffineTransform.getTranslateInstance(x - width / 2.0, y - width / 2.0));
		return new Area(tri);
	}
}
