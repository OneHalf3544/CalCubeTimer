package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class CrossSolver {

    public static final BiMap<Face, Character> FACES = Arrays.stream(Face.values())
            .collect(
                    ImmutableBiMap::<Face, Character>builder,
                    (r, face) -> r.put(face, face.getFaceChar()),
                    (b1, b2) -> b1.putAll(b2.build()))
            .build();

    private static final int MAX_TURNS = 9;

    private boolean isDuplicatedTurn(@NotNull Turn lastTurn, Face newTurnFace) {
        return newTurnFace == lastTurn.face
                || newTurnFace == lastTurn.face.getOpposite() && newTurnFace.ordinal() > lastTurn.face.ordinal();
    }

    private List<CrossSolution> solveCross(CubeWithRandomCrossState scrambledCubeWithRandomCrossState,
                                           RubicsColor solvingColor) {
        for (int maxDepth = 0; maxDepth < MAX_TURNS; maxDepth++) {
            List<CrossSolution> solutions = iddfs(
                    scrambledCubeWithRandomCrossState,
                    CubeWithSolvedCross.getSolvedState(solvingColor),
                    null,
                    maxDepth);
            if (!solutions.isEmpty()) {
                // solutions with optimal turns count found. don't go deeper.
                return solutions;
            }
        }
        throw new IllegalStateException("cannot solve cross in " + MAX_TURNS + " attempt");
    }


    public List<String> solveCross(RubicsColor solveColor, Face solveSide, String scramble) {
        Rotate setupRotate = rotateCrossToSolveSide(solveColor, solveSide);
        CubeWithRandomCrossState scrambledCube = CubeWithSolvedCross.getSolvedState(solveColor)
                .applyTurns(scramble);

        return solveCross(scrambledCube, solveColor).stream()
                .map(sol -> sol.toString(setupRotate))
                .collect(toList());
    }

    /**
     * Rotate cube to move cross from top layer to chosen one
     * @param solveCross face of chosen color
     * @return setup move
     */
    @NotNull
    Rotate rotateCrossToSolveSide(RubicsColor solveCross, Face solveSide) {
        if (solveCross.getFace() == solveSide) {
            return Rotate.identity;
        }

        for (Rotate rotate : new Rotate[]{Rotate.z, Rotate.x, Rotate.y,}) {
            if (solveCross.getFace() == solveSide.getOpposite()) {
                Rotate result = rotate.withDirection(Direction.HALF_TURN);
                if (result.mapTurnFaceToUnrotatedCubeFace(solveSide) == solveCross) {
                    return result;
                }
            } else {
                for (Direction direction : ImmutableList.of(Direction.CLOCKWISE, Direction.COUNTER_CLOCKWISE)) {
                    Rotate result = rotate.withDirection(direction);
                    if (result.mapTurnFaceToUnrotatedCubeFace(solveSide) == solveCross) {
                        return result;
                    }
                }
            }
        }
        throw new IllegalStateException("cannot solve cube rotate");
    }

    @NotNull
    private List<CrossSolution> iddfs(@NotNull CubeWithRandomCrossState scrambledCubeWithRandomCrossState,
                                      @NotNull CubeWithSolvedCross cubeWithSolvedCross,
                                      @Nullable Turn previousTurn,
                                      int depth) {
        if (depth == 0) {
            // last turn. check current state
            if (scrambledCubeWithRandomCrossState.isCrossSolvedOn(cubeWithSolvedCross.getSolvingColor())) {
                // solved!
                return singletonList(new CrossSolution(emptyList()));
            }

            // no solution
            return Collections.emptyList();
        }

        if (!scrambledCubeWithRandomCrossState.crossCanBeSolvedInNTurns(depth)) {
            return Collections.emptyList();
        }

        List<CrossSolution> solutions = new ArrayList<>();
        for (Face face : Face.values()) {
            if (previousTurn != null && isDuplicatedTurn(previousTurn, face)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                Turn newTurn = new Turn(face, direction);

                List<CrossSolution> newSolutions = iddfs(
                        scrambledCubeWithRandomCrossState.applyTurn(newTurn),
                        cubeWithSolvedCross,
                        newTurn,
                        depth - 1);

                solutions.addAll(newSolutions.stream()
                        .map(solution -> solution.withTurnBefore(newTurn))
                        .collect(toList()));
            }
        }
        return solutions;
    }
}
