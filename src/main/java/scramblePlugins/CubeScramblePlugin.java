package scramblePlugins;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public class CubeScramblePlugin extends ScramblePlugin {

    private static final Logger LOG = LogManager.getLogger(CubeScramblePlugin.class);

    public static final String PUZZLE_NAME = "Cube";

    protected static final Map<String, Color> FACE_NAMES_COLORS = ImmutableMap.<String, Color>builder()
            .put("L", Utils.stringToColor("ff8000"))
            .put("D", Utils.stringToColor("ffff00"))
            .put("B", Utils.stringToColor("0000ff"))
            .put("R", Utils.stringToColor("ff0000"))
            .put("U", Utils.stringToColor("ffffff"))
            .put("F", Utils.stringToColor("00ff00"))
            .build();

    public static final String FACES = "LDBRUFldbruf";

    private static final String[] FACES_ORDER = {"L", "D", "B", "R", "U", "F"};

    private static final char DEFAULT_SOLVE_FACE = 'U';
    private static final char DEFAULT_SOLVE_SIDE = 'D';

    private static final String REGEXP_345 = "^(?:[LDBRUF]w?|[ldbruf])[2']?$";
    private static final String REGEXP = "^(\\d+)?([LDBRUF])(?:\\((\\d+)\\))?[2']?$";
    private static final Pattern SHORT_PATTERN = Pattern.compile(REGEXP);

    public static final String MULTISLICE_ATTRIBUTE = "i18n[multislice]";
    public static final String OPTIMAL_CROSS_ATTRIBUTE = "i18n[optimalcross]";
    public static final String WIDE_NOTATION_ATTRIBUTE = "i18n[widenotation]";

    private static final boolean shortNotation = true;

    private boolean multislice;
    private boolean wideNotation;
    private boolean optimalCross;

    //solve the U face on D
    public final String defaultGenerators = DEFAULT_SOLVE_FACE + " " + DEFAULT_SOLVE_SIDE;

    @SuppressWarnings("UnusedDeclaration")
    public CubeScramblePlugin() {
        super(PUZZLE_NAME, true);
    }

    @Override
    public ScrambleString createScramble(PuzzleType puzzleType, ScrambleSettings variation, List<String> attributes) {
        int cubeSize = getSizeFromVariation(puzzleType.getVariationName());

        multislice = attributes.contains(MULTISLICE_ATTRIBUTE);
        wideNotation = attributes.contains(WIDE_NOTATION_ATTRIBUTE);
        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);

        String[][][] image = initializeImage(cubeSize);
        String scramble;
        if (cubeSize == 3 && variation.getLength() > 0)
            scramble = Search.solution(Tools.randomCube(), 21, 10, false);
        else {
            scramble = generateScramble(variation.getLength(), cubeSize, image, multislice);

        }
        return new ScrambleString(puzzleType, scramble, false, variation, this, getTextComments(scramble, cubeSize, variation.getGeneratorGroup()));
    }

    @Override
    public ScrambleString importScramble(PuzzleType puzzleType, ScrambleSettings.WithoutLength variation, String scramble,
                                         List<String> attributes) throws InvalidScrambleException {

        multislice = attributes.contains(MULTISLICE_ATTRIBUTE);
        wideNotation = attributes.contains(WIDE_NOTATION_ATTRIBUTE);
        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);

        int cubeSize = getSizeFromVariation(puzzleType.getVariationName());
        String[][][] image = initializeImage(cubeSize);
        if (!isValidScramble(scramble, cubeSize, image, multislice)) {
            throw new InvalidScrambleException(scramble);
        }
        String text = getTextComments(scramble, getSizeFromVariation(puzzleType.getVariationName()), variation.getGeneratorGroup());
        return new ScrambleString(puzzleType, scramble, true, variation.withLength(parseSize(scramble)), this, text);
    }


    @NotNull
    @Override
    public final Map<String, Color> getFaceNamesColors() {
        return FACE_NAMES_COLORS;
    }

    @Override
    @NotNull
    public ImmutableList<String> getVariations() {
        return ImmutableList.of("3x3x3", "4x4x4", "5x5x5", "6x6x6", "7x7x7", "8x8x8", "9x9x9", "10x10x10", "11x11x11");
    }

    @NotNull
    @Override
    public Map<String, String> getDefaultGenerators() {
        return ImmutableMap.<String, String>builder()
                .put("3x3x3", defaultGenerators)
                .put("4x4x4", defaultGenerators)
                .put("5x5x5", defaultGenerators)
                .put("6x6x6", defaultGenerators)
                .put("7x7x7", defaultGenerators)
                .put("8x8x8", defaultGenerators)
                .put("9x9x9", defaultGenerators)
                .put("10x10x10", defaultGenerators)
                .put("11x11x11", defaultGenerators)
                .build();
    }

    @NotNull
    @Override
    public int[] getDefaultLengths() {
        return new int[]{25, 40, 60, 80, 100, 120, 140, 160, 180};
    }

    @NotNull
    @Override
    public List<String> getAttributes() {
        return ImmutableList.of(
                MULTISLICE_ATTRIBUTE,
                WIDE_NOTATION_ATTRIBUTE,
                OPTIMAL_CROSS_ATTRIBUTE);
    }

    @NotNull
    @Override
    public final List<String> getDefaultAttributes() {
        return getAttributes();
    }

    @Override
    public final int getDefaultUnitSize() {
        return 11;
    }

    @Override
    public final Pattern getTokenRegex() {
        return Pattern.compile("^((?:\\d+)?[LDBRUFldbruf](?:\\(\\d+\\))?w?[2']?)(.*)$");
    }

    protected String getTextComments(String scramble, int cubeSize, String generatorGroup) {
        if (!optimalCross || cubeSize != 3) {
            return null;
        }

        Tuple2<Character, Character> generator = parseGeneratorGroupFor3x3(generatorGroup);
        List<String> solutions = getCrossSolutions(scramble, generator.v1, generator.v2);
        Collections.sort(solutions);

        return  Joiner.on("\n").join(solutions);
    }


    @Override
    public String htmlify(String formatMe) {
        return formatMe.replaceAll("\\((\\d+)\\)", "<sub>$1</sub>");
    }

    @Override
    public BufferedImage getScrambleImage(ScrambleString scramble, int gap, int cubieSize, Map<String, Color> colorScheme) {
        int cubeSize = getSizeFromVariation(scramble.getPuzzleType().getVariationName());
        String[][][] image = initializeImage(cubeSize);
        isValidScramble(scramble.getScramble(), cubeSize, image, multislice);
        Dimension dim = getImageSize(gap, cubieSize, cubeSize);
        BufferedImage buffer = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        drawCube(buffer.createGraphics(), image, gap, cubieSize, colorScheme);
        return buffer;
    }

    @Override
    public BufferedImage getDefaultStateImage(PuzzleType puzzleType, int gap, int cubieSize, Map<String, Color> colorScheme) {
        int cubeSize = getSizeFromVariation(puzzleType.getVariationName());
        String[][][] image = initializeImage(cubeSize);

        Dimension dim = getImageSize(gap, cubieSize, cubeSize);
        BufferedImage buffer = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        drawCube(buffer.createGraphics(), image, gap, cubieSize, colorScheme);
        return buffer;
    }

    @Override
    public int getNewUnitSize(int width, int height, int gap, String variation) {
        return getNewUnitSize(width, height, gap, getSizeFromVariation(variation));
    }

    @Override
    public Dimension getImageSize(int gap, int unitSize, String variation) {
        return getImageSize(gap, unitSize, getSizeFromVariation(variation));
    }

    @Override
    public Map<String, Shape> getFaces(int gap, int cubieSize, String variation) {
        int size = getSizeFromVariation(variation);
        return ImmutableMap.<String, Shape>builder()
                .put("L", getFace(gap, 2 * gap + size * cubieSize, size, cubieSize))
                .put("D", getFace(2 * gap + size * cubieSize, 3 * gap + 2 * size * cubieSize, size, cubieSize))
                .put("B", getFace(4 * gap + 3 * size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize))
                .put("R", getFace(3 * gap + 2 * size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize))
                .put("U", getFace(2 * gap + size * cubieSize, gap, size, cubieSize))
                .put("F", getFace(2 * gap + size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize))
                .build();
    }

    protected int getSizeFromVariation(String variation) {
        checkArgument(getVariations().contains(variation));
        return variation.isEmpty() ? 3 : Integer.parseInt(variation.split("x")[0]);
    }

    public Tuple2<Character, Character> parseGeneratorGroupFor3x3(String generatorGroup) {
        if (generatorGroup == null) {
            return Tuple.tuple(DEFAULT_SOLVE_FACE, DEFAULT_SOLVE_SIDE);
        }
        String[] faces = generatorGroup.split(" ");
        if (faces.length == 2) {
            Tuple.tuple(faces[0].charAt(0), faces[1].charAt(0));
        }
        return Tuple.tuple(DEFAULT_SOLVE_FACE, DEFAULT_SOLVE_SIDE);
    }

    private List<String> getCrossSolutions(String scramble, char solveCrossFace, char solveCrossSide) {
        return CrossSolver.solveCross(solveCrossFace, solveCrossSide, scramble);
    }

    private String generateScramble(int length, int cubeSize, String[][][] image, boolean multiSlice) {
        if (length == 0) {
            return "";
        }

        StringBuilder scram = new StringBuilder();
        int lastAxis = -1;
        int axis;
        int slices = cubeSize - ((multiSlice || cubeSize % 2 != 0) ? 1 : 0);
        int[] slicesMoved = new int[slices];
        int[] directionsMoved = new int[3];
        int moved;

        for (int i = 0; i < length; i += moved) {
            moved = 0;
            do {
                axis = random(3);
            } while (axis == lastAxis);

            for (int j = 0; j < slices; j++) slicesMoved[j] = 0;
            for (int j = 0; j < 3; j++) directionsMoved[j] = 0;

            do {
                int slice;
                do {
                    slice = random(slices);
                } while (slicesMoved[slice] != 0);
                int direction = random(3);

                if (multiSlice || slices != cubeSize || (directionsMoved[direction] + 1) * 2 < slices ||
                        (directionsMoved[direction] + 1) * 2 == slices && directionsMoved[0] + directionsMoved[1] + directionsMoved[2] == directionsMoved[direction]) {
                    directionsMoved[direction]++;
                    moved++;
                    slicesMoved[slice] = direction + 1;
                }
            } while (random(3) == 0 && moved < slices && moved + i < length);

            for (int j = 0; j < slices; j++) {
                if (slicesMoved[j] > 0) {
                    int direction = slicesMoved[j] - 1;
                    int face = axis;
                    int slice = j;
                    if (2 * j + 1 >= slices) {
                        face += 3;
                        slice = slices - 1 - slice;
                        direction = 2 - direction;
                    }

                    int n = ((slice * 6 + face) * 4 + direction);
                    scram.append(" ");
                    scram.append(moveString(n, cubeSize));

                    do {
                        slice(face, slice, direction, image, cubeSize);
                        slice--;
                    } while (multiSlice && slice >= 0);
                }
            }
            lastAxis = axis;
        }
        return scram.substring(1);
    }

    private String moveString(int n, int cubeSize) {
        String move = "";
        int face = n >> 2;
        int direction = n & 3;

        if (cubeSize <= 5) {
            if (wideNotation) {
                move += FACES.charAt(face % 6);
                if (face / 6 != 0) move += "w";
            } else {
                move += FACES.charAt(face);
            }
        } else {
            String f = "" + FACES.charAt(face % 6);
            if (face / 6 == 0) {
                move += f;
            } else {
                if (shortNotation) {
                    move += (face / 6 + 1) + f;
                } else {
                    move += f + "(" + (face / 6 + 1) + ")";
                }
            }
        }
        if (direction != 0) move += " 2'".charAt(direction);

        return move;
    }

    protected boolean isValidScramble(String scramble, int cubeSize, String[][][] image, boolean multislice) {
        String[] moves = scramble.split("\\s+");

        if (!scrambleMatchRegexp(moves, cubeSize)) {
            return false;
        }

        try {
            for (String move : moves) {
                int face;
                String slice1 = null;
                if (cubeSize > 5) {
                    Matcher m = SHORT_PATTERN.matcher(move);
                    if (!m.matches()) {
                        return false;
                    }
                    slice1 = m.group(1);
                    String slice2 = m.group(3);
                    if (slice1 != null && slice2 != null) {
                        //only short notation or long notation is allowed, not both
                        return false;
                    }
                    if (slice1 == null)
                        slice1 = slice2;
                    face = FACES.indexOf(m.group(2));
                } else {
                    face = FACES.indexOf(move.substring(0, 1));
                }

                int slice = face / 6;
                face %= 6;
                if (move.contains("w")) {
                    slice++;
                }
                else if (slice1 != null)
                    slice = Integer.parseInt(slice1) - 1;

                int direction = " 2'".indexOf(move.charAt(move.length() - 1) + "");
                if (direction < 0) direction = 0;

                do {
                    slice(face, slice, direction, image, cubeSize);
                    slice--;
                } while (multislice && slice >= 0);
            }
            return true;
        }
        catch (Exception e) {
            LOG.info("unexpected exception", e);
            return false;
        }

    }

    protected boolean scrambleMatchRegexp(String[] moves, int cubeSize) {
        if (cubeSize < 3) {
            return false;
        }
        else if (cubeSize <= 5) {
            for (String move : moves) {
                if (!move.matches(REGEXP_345)) {
                    return false;
                }
            }
        } else {
            for (String move : moves) {
                if (!move.matches(REGEXP)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected String[][][] initializeImage(int cubeSize) {
        String[][][] image = new String[6][cubeSize][cubeSize];

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < cubeSize; j++) {
                for (int k = 0; k < cubeSize; k++) {
                    image[i][j][k] = FACES_ORDER[i];
                }
            }
        }
        return image;
    }

    protected void slice(int face, int slice, int direction, String[][][] image, int cubeSize) {
        face %= 6;
        int sface = face;
        int sslice = slice;
        int sdir = direction;

        if (face > 2) {
            sface -= 3;
            sslice = cubeSize - 1 - slice;
            sdir = 2 - direction;
        }
        for (int i = 0; i <= sdir; i++) {
            for (int j = 0; j < cubeSize; j++) {
                if (sface == 0) {
                    String temp = image[4][j][sslice];
                    image[4][j][sslice] = image[2][cubeSize - 1 - j][cubeSize - 1 - sslice];
                    image[2][cubeSize - 1 - j][cubeSize - 1 - sslice] = image[1][j][sslice];
                    image[1][j][sslice] = image[5][j][sslice];
                    image[5][j][sslice] = temp;
                } else if (sface == 1) {
                    String temp = image[0][cubeSize - 1 - sslice][j];
                    image[0][cubeSize - 1 - sslice][j] = image[2][cubeSize - 1 - sslice][j];
                    image[2][cubeSize - 1 - sslice][j] = image[3][cubeSize - 1 - sslice][j];
                    image[3][cubeSize - 1 - sslice][j] = image[5][cubeSize - 1 - sslice][j];
                    image[5][cubeSize - 1 - sslice][j] = temp;
                } else if (sface == 2) {
                    String temp = image[4][sslice][j];
                    image[4][sslice][j] = image[3][j][cubeSize - 1 - sslice];
                    image[3][j][cubeSize - 1 - sslice] = image[1][cubeSize - 1 - sslice][cubeSize - 1 - j];
                    image[1][cubeSize - 1 - sslice][cubeSize - 1 - j] = image[0][cubeSize - 1 - j][sslice];
                    image[0][cubeSize - 1 - j][sslice] = temp;
                }
            }
        }
        if (slice == 0) {
            for (int i = 0; i <= 2 - direction; i++) {
                for (int j = 0; j < (cubeSize + 1) / 2; j++) {
                    for (int k = 0; k < cubeSize / 2; k++) {
                        String temp = image[face][j][k];
                        image[face][j][k] = image[face][k][cubeSize - 1 - j];
                        image[face][k][cubeSize - 1 - j] = image[face][cubeSize - 1 - j][cubeSize - 1 - k];
                        image[face][cubeSize - 1 - j][cubeSize - 1 - k] = image[face][cubeSize - 1 - k][j];
                        image[face][cubeSize - 1 - k][j] = temp;
                    }
                }
            }
        }
    }

    private static int getNewUnitSize(int width, int height, int gap, int size) {
        return (int) Math.min((width - 5 * gap) / 4. / size,
                (height - 4 * gap) / 3. / size);
    }

    private static Dimension getImageSize(int gap, int unitSize, int size) {
        return new Dimension(getCubeViewWidth(unitSize, gap, size), getCubeViewHeight(unitSize, gap, size));
    }

    private void drawCube(Graphics2D g, String[][][] state, int gap, int cubieSize, @NotNull Map<String, Color> colorScheme) {
        int size = state[0].length;
        paintCubeFace(g, gap, 2 * gap + size * cubieSize, size, cubieSize, state[0], colorScheme);
        paintCubeFace(g, 2 * gap + size * cubieSize, 3 * gap + 2 * size * cubieSize, size, cubieSize, state[1], colorScheme);
        paintCubeFace(g, 4 * gap + 3 * size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize, state[2], colorScheme);
        paintCubeFace(g, 3 * gap + 2 * size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize, state[3], colorScheme);
        paintCubeFace(g, 2 * gap + size * cubieSize, gap, size, cubieSize, state[4], colorScheme);
        paintCubeFace(g, 2 * gap + size * cubieSize, 2 * gap + size * cubieSize, size, cubieSize, state[5], colorScheme);
    }

    private void paintCubeFace(Graphics2D g, int x, int y, int size, int cubeSize, String[][] faceColors, Map<String, Color> colorScheme) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                g.setColor(Color.BLACK);
                int tempX = x + col * cubeSize;
                int tempY = y + row * cubeSize;
                g.drawRect(tempX, tempY, cubeSize, cubeSize);
                g.setColor(colorScheme.get(faceColors[row][col]));
                g.fillRect(tempX + 1, tempY + 1, cubeSize - 1, cubeSize - 1);
            }
        }
    }

    private static int getCubeViewWidth(int cube, int gap, int size) {
        return (size * cube + gap) * 4 + gap;
    }

    private static int getCubeViewHeight(int cube, int gap, int size) {
        return (size * cube + gap) * 3 + gap;
    }

    private static Shape getFace(int leftBound, int topBound, int size, int cubeSize) {
        return new Rectangle(leftBound, topBound, size * cubeSize, size * cubeSize);
    }
}
