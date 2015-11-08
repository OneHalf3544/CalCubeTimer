package scramblePlugins.cube3x3crosssolver;

import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * <p>
 * <p>
 * Created: 07.11.2015 16:15
 * <p>
 *
 * @author OneHalf
 */
public class CubeWithSolvedCross extends CubeWithRandomCrossState {

    private static final Map<RubicsColor, CubeWithSolvedCross> SOLVED_STATES_CACHE = new HashMap<>();

    byte[] prunePositions;

    public static CubeWithSolvedCross getSolvedState(RubicsColor rubicsColor) {
        return SOLVED_STATES_CACHE.computeIfAbsent(rubicsColor, CubeWithSolvedCross::createSolvedState);
    }

    private static CubeWithSolvedCross createSolvedState(RubicsColor crossColor) {
        CubeWithRandomCrossState cubeWithRandomCrossState = createSolvedCube(crossColor);
        return new CubeWithSolvedCross(
                crossColor,
                cubeWithRandomCrossState.edgesOrientations,
                cubeWithRandomCrossState.edgesPosition);
    }

    private static CubeWithRandomCrossState createSolvedCube(RubicsColor crossFace) {
        Boolean[] edgesOrientations = new Boolean[12];
        Integer[] edgesPosition = new Integer[12];
        for (int i : FACE_INDEXES.get(crossFace.getFace())) {
            edgesOrientations[i] = false;
            edgesPosition[i] = i;
        }
        return new CubeWithRandomCrossState(
                crossFace,
                Arrays.asList(edgesOrientations),
                Arrays.asList(edgesPosition),
                createEmptyScramble());
    }

    @NotNull
    private static ScrambleString createEmptyScramble() {
        return new ScrambleString(null, "", false, new ScrambleSettings(null, null, "", 0), null, "");
    }

    private CubeWithSolvedCross(RubicsColor crossSide,
                                List<Boolean> orientations,
                                List<Integer> positions) {
        super(crossSide, orientations, positions, createEmptyScramble());
        buildTables(crossSide);
    }

    private void buildTables(RubicsColor crossFace) {
        CubeWithRandomCrossState solved = createSolvedCube(crossFace);

        prunePositions = new byte[HASH_EDGES_POSITION_COUNT];

        Queue<CubeWithRandomCrossState> fringe = new LinkedList<>();
        fringe.add(solved);
        while (!fringe.isEmpty()) {
            CubeWithRandomCrossState position = fringe.poll();
            for (Face f : Face.values()) {
                for (Direction dir : Direction.values()) {
                    CubeWithRandomCrossState newPos = position.applyTurn(new Turn(f, dir));

                    byte currentSolveTurns = prunePositions[newPos.hashEdgesPositions()];
                    byte newSolveTurns = (byte) (prunePositions[position.hashEdgesPositions()] + 1);

                    if (currentSolveTurns == 0) {
                        checkArgument(currentSolveTurns <= newSolveTurns);
                        prunePositions[newPos.hashEdgesPositions()] = newSolveTurns;
                        fringe.add(newPos);
                    }
                }
            }
        }
    }

}
