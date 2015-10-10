package scramblePlugins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Cube2x2ScramblePlugin extends CubeScramblePlugin {

    private static final Logger LOG = LogManager.getLogger(Cube2x2ScramblePlugin.class);

    private static final String REGEXP_2 = "^[LDBRUF][2']?$";

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

    List<Integer> sol = new ArrayList<>();

    @SuppressWarnings("UnusedDeclaration")
    public Cube2x2ScramblePlugin() {
    }

    @Override
    public ScrambleString createScramble(ScrambleVariation variation, List<String> attributes) {
        String scramble = generateScrambleFor2x2();
        return new ScrambleString(scramble, false, variation, this, getTextComments(scramble, 2, variation.getGeneratorGroup()));
    }

    @Override
    public ScrambleString importScramble(ScrambleVariation.WithoutLength variation, String scramble,
                                         List<String> attributes) throws InvalidScrambleException {
        int cubeSize = getSizeFromVariation(variation.getName());
        String[][][] image = initializeImage(cubeSize);
        if (!isValidScramble(scramble, cubeSize, image, false)) {
            throw new InvalidScrambleException(scramble);
        }
        String text = getTextComments(scramble, getSizeFromVariation(variation.getName()), variation.getGeneratorGroup());
        return new ScrambleString(scramble, true, variation.withLength(parseSize(scramble)), this, text);
    }


    @Override
    @NotNull
    public ImmutableList<String> getVariations() {
        return ImmutableList.of("2x2x2");
    }

    @NotNull
    @Override
    public final Map<String, String> getDefaultGenerators() {
        return ScramblePluginManager.NULL_SCRAMBLE_PLUGIN.getDefaultGenerators();
    }

    @NotNull
    @Override
    public final int[] getDefaultLengths() {
        return new int[]{25};
    }

    @NotNull
    @Override
    public final List<String> getAttributes() {
        return ScramblePluginManager.NULL_SCRAMBLE_PLUGIN.getAttributes();
    }

    @Override
    protected String getTextComments(String scramble, int cubeSize, String generatorGroup) {
        return null;
    }

    private String generateScrambleFor2x2() {
        calcpermFor2x2();
        List<Integer> posit = posit2x2();
        mix2x2(posit);
        return solve2x2(posit);
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

    @Override
    protected boolean scrambleMatchRegexp(String[] moves, int cubeSize) {
        if (cubeSize != 2) {
            return false;
        }

        for (String move : moves) {
            if (!move.matches(REGEXP_2)) {
                return false;
            }
        }
        return true;
    }
}
