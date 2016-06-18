package scramblePlugins;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import scramblePlugins.cube3x3crosssolver.CrossSolver;
import scramblePlugins.cube3x3crosssolver.Face;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * <p>
 * Created: 08.11.2015 12:47
 * <p>
 *
 * @author OneHalf
 */
public class Cube3x3ScramblePlugin extends CubeScramblePlugin {

    private static final Face DEFAULT_SOLVE_FACE = Face.UP;
    private static final Face DEFAULT_SOLVE_SIDE = Face.DOWN;

    public static final String OPTIMAL_CROSS_ATTRIBUTE = "i18n[optimalcross]";

    public static final CrossSolver CROSS_SOLVER = new CrossSolver();

    private boolean optimalCross;

    //solve the U face on D
    public final String defaultGenerator = DEFAULT_SOLVE_FACE + " " + DEFAULT_SOLVE_SIDE;

    @NotNull
    @Override
    public Map<String, String> getDefaultGenerators() {
        return Collections.singletonMap("3x3x3", defaultGenerator);
    }

    @Override
    public ScrambleString createScramble(PuzzleType puzzleType, ScrambleSettings variation, List<String> attributes) {

        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);

        String scramble = Search.solution(Tools.randomCube(), 21, 10, false);
        return new ScrambleString(puzzleType, scramble, false, variation, this,
                getTextComments(scramble, variation.getGeneratorGroup()));
    }

    @Override
    @NotNull
    public ImmutableList<String> getVariations() {
        return ImmutableList.of("3x3x3");
    }

    @NotNull
    @Override
    public int[] getDefaultLengths() {
        return new int[]{25};
    }

    @Override
    public ScrambleString importScramble(PuzzleType puzzleType, ScrambleSettings.WithoutLength variation,
                                         String scramble, List<String> attributes) throws InvalidScrambleException {
        optimalCross = attributes.contains(OPTIMAL_CROSS_ATTRIBUTE);
        return super.importScramble(puzzleType, variation, scramble, attributes);
    }

    @Override
    protected String getTextComments(String scramble, int sizeFromVariation, String generatorGroup) {
        return getTextComments(scramble, generatorGroup);
    }

    @Override
    protected int getSizeFromVariation(String variation) {
        return 3;
    }

    @NotNull
    @Override
    public List<String> getAttributes() {
        return ImmutableList.<String>builder()
                .add(OPTIMAL_CROSS_ATTRIBUTE)
                .build();
    }

    public Tuple2<Face, Face> parseGeneratorGroupFor3x3(String generatorGroup) {
        if (generatorGroup == null) {
            return Tuple.tuple(DEFAULT_SOLVE_FACE, DEFAULT_SOLVE_SIDE);
        }
        String[] faces = generatorGroup.split(" ");
        if (faces.length == 2) {
            return Tuple.tuple(
                    CrossSolver.FACES.inverse().get(faces[0].charAt(0)),
                    CrossSolver.FACES.inverse().get(faces[1].charAt(0)));
        }
        return Tuple.tuple(DEFAULT_SOLVE_FACE, DEFAULT_SOLVE_SIDE);
    }

    private List<String> getCrossSolutions(String scramble, Face solveCrossFace, Face solveCrossSide) {
        return CROSS_SOLVER.solveCross(solveCrossFace, solveCrossSide, scramble);
    }

    protected String getTextComments(String scramble, String generatorGroup) {
        if (!optimalCross) {
            return null;
        }

        Tuple2<Face, Face> generator = parseGeneratorGroupFor3x3(generatorGroup);
        List<String> solutions = getCrossSolutions(scramble, generator.v1, generator.v2);
        Collections.sort(solutions);

        return  Joiner.on("\n").join(solutions);
    }

}
