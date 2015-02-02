package scramblePlugins;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CubeScramblePlugin extends ScramblePlugin {

    private static final Logger LOG = Logger.getLogger(CubeScramblePlugin.class);

    public static final String PUZZLE_NAME = "Cube";

    private static final Map<String, Color> FACE_NAMES_COLORS = ImmutableMap.<String, Color>builder()
            .put("L", Utils.stringToColor("ff8000"))
            .put("D", Utils.stringToColor("ffff00"))
            .put("B", Utils.stringToColor("0000ff"))
            .put("R", Utils.stringToColor("ff0000"))
            .put("U", Utils.stringToColor("ffffff"))
            .put("F", Utils.stringToColor("00ff00"))
            .build();

    private static final String FACES = "LDBRUFldbruf";

    private static final String[] FACES_ORDER = {"L", "D", "B", "R", "U", "F"};

    private static final char DEFAULT_SOLVE_FACE = 'U';
    private static final char DEFAULT_SOLVE_SIDE = 'D';

    private static final String REGEXP_2 = "^[LDBRUF][2']?$";
    private static final String REGEXP_345 = "^(?:[LDBRUF]w?|[ldbruf])[2']?$";
    private static final String REGEXP = "^(\\d+)?([LDBRUF])(?:\\((\\d+)\\))?[2']?$";
    private static final Pattern SHORT_PATTERN = Pattern.compile(REGEXP);

    public static final String MULTISLICE_ATTRIBUTE = "%%multislice%%";
    public static final String OPTIMAL_CROSS_ATTRIBUTE = "%%optimalcross%%";
    public static final String WIDE_NOTATION_ATTRIBUTE = "%%widenotation%%";

    List<Integer> perm = new ArrayList<>();
    List<Integer> twst = new ArrayList<>();
    List<List<Integer>> permmv = new ArrayList<>();
    List<List<Integer>> twstmv = new ArrayList<>();

    List<Integer> posit2x2() {
        return Lists.newArrayList(
                1, 1, 1, 1,
                2, 2, 2, 2,
                5, 5, 5, 5,
                4, 4, 4, 4,
                3, 3, 3, 3,
                0, 0, 0, 0);
    }

    private static final boolean shortNotation = true;

    private boolean multislice;
    private boolean wideNotation;
    private boolean optimalCross;

    //solve the U face on D
    public final String dg = DEFAULT_SOLVE_FACE + " " + DEFAULT_SOLVE_SIDE;

    List<Integer> sol = new ArrayList<>();

    @SuppressWarnings("UnusedDeclaration")
    public CubeScramblePlugin() {
        super(PUZZLE_NAME, true);
    }

    @Override
    public ScrambleString createScramble(ScrambleVariation variation, String generatorGroup, List<String> attributes) {
        int cubeSize = getSizeFromVariation(variation.getName());

        multislice = attributes.contains(MULTISLICE_ATTRIBUTE);
        wideNotation = attributes.contains(WIDE_NOTATION_ATTRIBUTE);
        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);

        String[][][] image = initializeImage(cubeSize);
        String scramble;
        if (cubeSize == 2) {
            scramble = generateScrambleFor2x2();
        } else if (cubeSize == 3 && variation.getLength() > 0)
            scramble = Search.solution(Tools.randomCube(), 21, 10, false);
        else {
            scramble = generateScramble(variation.getLength(), cubeSize, image);

        }
        return new ScrambleString(scramble, false, variation, this, getTextComments(scramble, cubeSize, generatorGroup));
    }

    @Override
    public ScrambleString importScramble(ScrambleVariation.WithoutLength variation, String scramble,
                                         String generatorGroup, List<String> attributes) throws InvalidScrambleException {

        multislice = attributes.contains(MULTISLICE_ATTRIBUTE);
        wideNotation = attributes.contains(WIDE_NOTATION_ATTRIBUTE);
        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);

        int cubeSize = getSizeFromVariation(variation.getName());
        String[][][] image = initializeImage(cubeSize);
        if (!isValidScramble(scramble, cubeSize, image)) {
            throw new InvalidScrambleException(scramble);
        }
        String text = getTextComments(scramble, getSizeFromVariation(variation.getName()), generatorGroup);
        return new ScrambleString(scramble, true, variation.withLength(parseSize(scramble)), this, text);
    }


    @NotNull
    @Override
    public final Map<String, Color> getFaceNamesColors() {
        return FACE_NAMES_COLORS;
    }

    @Override
    @NotNull
    public final ImmutableList<String> getVariations() {
        return ImmutableList.of("2x2x2", "3x3x3", "4x4x4", "5x5x5", "6x6x6", "7x7x7", "8x8x8", "9x9x9", "10x10x10", "11x11x11");
    }

    @NotNull
    @Override
    public final Map<String, String> getDefaultGenerators() {
        return ImmutableMap.<String, String>builder()
                .put("2x2x2", dg)
                .put("3x3x3", dg)
                .put("4x4x4", dg)
                .put("5x5x5", dg)
                .put("6x6x6", dg)
                .put("7x7x7", dg)
                .put("8x8x8", dg)
                .put("9x9x9", dg)
                .put("10x10x10", dg)
                .put("11x11x11", dg)
                .build();
    }

    @NotNull
    @Override
    public final int[] getDefaultLengths() {
        return new int[]{25, 25, 40, 60, 80, 100, 120, 140, 160, 180};
    }

    @NotNull
    @Override
    public final List<String> getAttributes() {
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

    private String getTextComments(String scramble, int cubeSize, String generatorGroup) {
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
        int cubeSize = getSizeFromVariation(scramble.getVariation().getName());
        String[][][] image = initializeImage(cubeSize);
        // todo
        isValidScramble(scramble.getScramble(), cubeSize, image);
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

    private static int getSizeFromVariation(String variation) {
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

    // todo another class for 2x2x2 scramble plugin
    private String generateScrambleFor2x2() {
        calcpermFor2x2();
        List<Integer> posit = posit2x2();
        mix2x2(posit);
        return solve2x2(posit);
    }

    private ArrayList<String> getCrossSolutions(String scramble, char solveCrossFace, char solveCrossSide) {
        return CrossSolver.solveCross(solveCrossFace, solveCrossSide, scramble);
    }

    List<Integer> seq = new ArrayList<>();

    int[][] cornerIndices = new int[][]{{15, 16, 21}, {14, 20, 4}, {13, 9, 17}, {12, 5, 8}, {3, 23, 18}, {2, 6, 22}, {1, 19, 11}, {0, 10, 7}};
    String[] cornerNames = new String[]{"URF", "UFL", "UBR", "ULB", "DFR", "DLF", "DRB", "DBL"};

    Map<Character, Integer> faceToIndex = ImmutableMap.<Character, Integer>builder()
            .put('D', 1)
            .put('L', 2)
            .put('B', 5)
            .put('U', 4)
            .put('R', 3)
            .put('F', 0)
            .build();

    private void mix2x2(List<Integer> posit) {
        //Modified to choose a random state, rather than apply 500 random turns
        //-Jeremy Fleischman
        List<Integer> remaining = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
        List<Integer> cp = new ArrayList<>();
        while (remaining.size() > 0)
            cp.add(remaining.remove((int) Math.floor(Math.random() * remaining.size())));
        //it would appear that the solver only works if the BLD piece is fixed, which is fine
        cp.add(7);

        int sum = 0;
        for (int i = 0; i < cp.size(); i++) {
            int orientation;
            if (i == cp.size() - 1) {
                orientation = 0;
            }
            else if (i == cp.size() - 2) {
                orientation = (3 - sum) % 3;
            }
            else {
                orientation = (int) Math.floor(Math.random() * 3);
            }
            sum = (sum + orientation) % 3;
            for (int j = 0; j < 3; j++) {
                int jj = (j + orientation) % 3;
                posit.set(cornerIndices[i][j], faceToIndex.get(cornerNames[cp.get(i)].charAt(jj)));
            }
        }
    }

    List<Integer> piece = Lists.newArrayList(
            15, 16, 16, 21, 21, 15,
            13, 9, 9, 17, 17, 13,
            14, 20, 20, 4, 4, 14,
            12, 5, 5, 8, 8, 12,
            3, 23, 23, 18, 18, 3,
            1, 19, 19, 11, 11, 1,
            2, 6, 6, 22, 22, 2,
            0, 10, 10, 7, 7, 0);

    List<List<Integer>> adj = new ArrayList<>();
    {
        adj.add(Arrays.asList(new Integer[6]));
        adj.add(Arrays.asList(new Integer[6]));
        adj.add(Arrays.asList(new Integer[6]));
        adj.add(Arrays.asList(new Integer[6]));
        adj.add(Arrays.asList(new Integer[6]));
        adj.add(Arrays.asList(new Integer[6]));
    }

    private void calcadj2x2(List<Integer> posit) {
        //count all adjacent pairs (clockwise around corners)
        int a, b;
        for (a = 0; a < 6; a++) {
            for (b = 0; b < 6; b++) {
                adj.get(a).set(b, 0);
            }
        }
        for (a = 0; a < 48; a += 2) {
            if (posit.get(piece.get(a)) <= 5 && posit.get(piece.get(a + 1)) <= 5) {
                List<Integer> temp = adj.get(posit.get(piece.get(a)));
                int index = posit.get(piece.get(a + 1));
                temp.set(index, temp.get(index) + 1);
            }
        }
    }

    private String solve2x2(List<Integer> posit) {
        calcadj2x2(posit);
        List<Integer> opp = Arrays.asList(new Integer[6]);
        for (int a = 0; a < 6; a++) {
            for (int b = 0; b < 6; b++) {
                if (a != b && adj.get(a).get(b) + adj.get(b).get(a) == 0) {
                    opp.set(a, b);
                    opp.set(b, a);
                }
            }
        }
        //Each piece is determined by which of each pair of opposite colours it uses.
        List<Integer> ps = Arrays.asList(new Integer[7]);
        List<Integer> tws = Arrays.asList(new Integer[7]);
        int a = 0;
        for (int d = 0; d < 7; d++) {
            int p = 0;
            for (int b = a; b < a + 6; b += 2) {
                if (Objects.equals(posit.get(piece.get(b)), posit.get(piece.get(42)))) p += 4;
                if (Objects.equals(posit.get(piece.get(b)), posit.get(piece.get(44)))) p += 1;
                if (Objects.equals(posit.get(piece.get(b)), posit.get(piece.get(46)))) p += 2;
            }
            ps.set(d, p);
            if (Objects.equals(posit.get(piece.get(a)), posit.get(piece.get(42))) || Objects.equals(posit.get(piece.get(a)), opp.get(posit.get(piece.get(42)))))
                tws.set(d, 0);
            else if (Objects.equals(posit.get(piece.get(a + 2)), posit.get(piece.get(42))) || Objects.equals(posit.get(piece.get(a + 2)), opp.get(posit.get(piece.get(42)))))
                tws.set(d, 1);
            else tws.set(d, 2);
            a += 6;
        }
        //convert position to numbers
        int q = 0;
        for (a = 0; a < 7; a++) {
            int b = 0;
            for (int c = 0; c < 7; c++) {
                if (ps.get(c) == a) break;
                if (ps.get(c) > a) b++;
            }
            q = q * (7 - a) + b;
        }
        int t = 0;
        for (a = 5; a >= 0; a--) {
            t = (int) (t * 3 + tws.get(a) - 3 * Math.floor(tws.get(a) / 3));
        }
        if (q != 0 || t != 0) {
            sol.clear();
            for (int l = seq.size(); l < 100; l++) {
                if (search2x2(0, q, t, l, -1)) break;
            }
            String tt = "";
            for (q = 0; q < sol.size(); q++) {
                tt = "URF".charAt(sol.get(q) / 10) + "" + "\'2 ".charAt(sol.get(q) % 10) + " " + tt;
            }
            return tt;
        }
        return null;
    }

    private boolean search2x2(int d, int q, int t, int l, int lm) {
        //searches for solution, from position q|t, in l moves exactly. last move was lm, current depth=d
        if (l == 0) {
            if (q == 0 && t == 0) {
                return true;
            }
        } else {
            if (perm.get(q) > l || twst.get(t) > l)
                return false;
            int p, s, a, m;
            for (m = 0; m < 3; m++) {
                if (m != lm) {
                    p = q;
                    s = t;
                    for (a = 0; a < 3; a++) {
                        p = permmv.get(p).get(m);
                        s = twstmv.get(s).get(m);
                        if (sol.size() > d) {
                            sol.set(d, 10 * m + a);
                        } else {
                            sol.add(10 * m + a);
                        }
                        if (search2x2(d + 1, p, s, l - 1, m)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void calcpermFor2x2() {
        //calculate solving arrays
        //first permutation

        for (int p = 0; p < 5040; p++) {
            perm.add(p, -1);
            permmv.add(p, new ArrayList<>());
            for (int m = 0; m < 3; m++) {
                permmv.get(p).add(m, getprmmv2x2(p, m));
            }
        }

        perm.set(0, 0);
        for (int l = 0; l <= 6; l++) {
            for (int p = 0; p < 5040; p++) {
                if (perm.get(p) == l) {
                    for (int m = 0; m < 3; m++) {
                        int q = p;
                        for (int c = 0; c < 3; c++) {
                            q = permmv.get(q).get(m);
                            if (perm.get(q) == -1) {
                                perm.set(q, l + 1);
                            }
                        }
                    }
                }
            }
        }

        //then twist
        for (int p = 0; p < 729; p++) {
            twst.add(p, -1);
            twstmv.add(p, new ArrayList<>());
            for (int m = 0; m < 3; m++) {
                twstmv.get(p).add(m, gettwsmvFor2x2(p, m));
            }
        }

        twst.set(0, 0);
        for (int l = 0; l <= 5; l++) {
            for (int p = 0; p < 729; p++) {
                if (twst.get(p) == l) {
                    for (int m = 0; m < 3; m++) {
                        int q = p;
                        for (int c = 0; c < 3; c++) {
                            q = twstmv.get(q).get(m);
                            if (twst.get(q) == -1) {
                                twst.set(q, l + 1);
                            }
                        }
                    }
                }
            }
        }
        //remove wait sign
    }

    private int getprmmv2x2(int p, int m) {
        //given position p<5040 and move m<3, return new position number
        int a, b, c, q;
        //convert number into array;
        List<Integer> ps = Arrays.asList(new Integer[8]);

        q = p;
        for (a = 1; a <= 7; a++) {
            b = q % a;
            q = (q - b) / a;
            for (c = a - 1; c >= b; c--) {
                Integer ii = null;
                try {
                    ii = ps.get(c);
                } catch (IndexOutOfBoundsException e) {
                    LOG.debug("ignore error " + e);
                }
                ps.set(c + 1, ii);
            }
            ps.set(b, 7 - a);
        }
        //perform move on array
        if (m == 0) {
            //U
            c = ps.get(0);
            ps.set(0, ps.get(1));
            ps.set(1, ps.get(3));
            ps.set(3, ps.get(2));
            ps.set(2, c);
        } else if (m == 1) {
            //R
            c = ps.get(0);
            ps.set(0, ps.get(4));
            ps.set(4, ps.get(5));
            ps.set(5, ps.get(1));
            ps.set(1, c);
        } else if (m == 2) {
            //F
            c = ps.get(0);
            ps.set(0, ps.get(2));
            ps.set(2, ps.get(6));
            ps.set(6, ps.get(4));
            ps.set(4, c);
        }
        //convert array back to number
        q = 0;
        for (a = 0; a < 7; a++) {
            b = 0;
            for (c = 0; c < 7; c++) {
                if (ps.get(c) == a) break;
                if (ps.get(c) > a) b++;
            }
            q = q * (7 - a) + b;
        }
        return q;
    }

    private int gettwsmvFor2x2(int p, int m) {
        //given orientation p<729 and move m<3, return new orientation number
        int a, b, c, d, q;
        //convert number into array;
        List<Integer> ps = Arrays.asList(new Integer[7]);
        q = p;
        d = 0;
        for (a = 0; a <= 5; a++) {
            c = (int) Math.floor(q / 3);
            b = q - 3 * c;
            q = c;
            ps.set(a, b);
            d -= b;
            if (d < 0) d += 3;
        }
        ps.set(6, d);
        //perform move on array
        if (m == 0) {
            //U
            c = ps.get(0);
            ps.set(0, ps.get(1));
            ps.set(1, ps.get(3));
            ps.set(3, ps.get(2));
            ps.set(2, c);
        } else if (m == 1) {
            //R
            c = ps.get(0);
            ps.set(0, ps.get(4));
            ps.set(4, ps.get(5));
            ps.set(5, ps.get(1));
            ps.set(1, c);
            ps.set(0, ps.get(0) + 2);
            ps.set(1, ps.get(1) + 1);
            ps.set(5, ps.get(5) + 2);
            ps.set(4, ps.get(4) + 1);
        } else if (m == 2) {
            //F
            c = ps.get(0);
            ps.set(0, ps.get(2));
            ps.set(2, ps.get(6));
            ps.set(6, ps.get(4));
            ps.set(4, c);
            ps.set(2, ps.get(2) + 2);
            ps.set(0, ps.get(0) + 1);
            ps.set(4, ps.get(4) + 2);
            ps.set(6, ps.get(6) + 1);
        }
        //convert array back to number
        q = 0;
        for (a = 5; a >= 0; a--) {
            q = q * 3 + (ps.get(a) % 3);
        }
        return q;
    }

    private String generateScramble(int length, int cubeSize, String[][][] image) {
        if (length == 0) {
            return "";
        }

        StringBuilder scram = new StringBuilder();
        int lastAxis = -1;
        int axis;
        int slices = cubeSize - ((multislice || cubeSize % 2 != 0) ? 1 : 0);
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

                if (multislice || slices != cubeSize || (directionsMoved[direction] + 1) * 2 < slices ||
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
                    } while (multislice && slice >= 0);
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

    private boolean isValidScramble(String scramble, int cubeSize, String[][][] image) {
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

    private boolean scrambleMatchRegexp(String[] moves, int cubeSize) {
        if (cubeSize < 2) {
            return false;
        }
        else if (cubeSize == 2) {
            for (String move : moves) {
                if (!move.matches(REGEXP_2)) {
                    return false;
                }
            }
        } else if (cubeSize <= 5) {
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

    private String[][][] initializeImage(int cubeSize) {
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

    private void slice(int face, int slice, int direction, String[][][] image, int cubeSize) {
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

    private void drawCube(Graphics2D g, String[][][] state, int gap, int cubieSize, Map<String, Color> colorScheme) {
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
