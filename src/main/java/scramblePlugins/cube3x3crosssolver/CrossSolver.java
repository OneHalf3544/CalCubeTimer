package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
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

    public static final int MAX_TURNS = 9;

    @NotNull
    private List<CrossSolution> iddfs(@NotNull Cube scrambledCube,
                                      @NotNull SolvedCube solvedCube,
                                      @Nullable Turn previousTurn,
                                      int depth) {
        if (depth == 0) {
            // last turn. check current state
            if (scrambledCube.isCrossSolvedOn(solvedCube.getSolvingSide())) {
                // solved!
                return singletonList(new CrossSolution(emptyList()));
            }

            // no solution
            return Collections.emptyList();
        }

        if (!scrambledCube.canBeSolvedInNTurns(depth)) {
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
                        scrambledCube.applyTurn(newTurn),
                        solvedCube,
                        newTurn,
                        depth - 1);

                solutions.addAll(newSolutions.stream()
                        .map(solution -> solution.withTurnBefore(newTurn))
                        .collect(toList()));
            }
        }
        return solutions;
    }

    private boolean isDuplicatedTurn(@NotNull Turn lastTurn, Face newTurnFace) {
        return newTurnFace == lastTurn.face
                || newTurnFace == lastTurn.face.getOpposite() && newTurnFace.ordinal() > lastTurn.face.ordinal();
    }


    private List<CrossSolution> solveCross(Cube scrambledCube, Face face) {
        SolvedCube solvedState = SolvedCube.getSolvedState(face);
        if (scrambledCube.isCrossSolvedOn(face)) {
            return Collections.singletonList(new CrossSolution(Collections.emptyList()));
        }
        for (int maxDepth = 0; maxDepth < MAX_TURNS; maxDepth++) {
            List<CrossSolution> solutions = iddfs(
                    scrambledCube,
                    solvedState,
                    null,
                    maxDepth);
            if (!solutions.isEmpty()) {
                // solutions with optimal turns count found. don't go deeper.
                return solutions;
            }
        }
        throw new IllegalStateException("cannot solve cross in " + MAX_TURNS + " attempt");
    }

    public List<String> solveCross(Face solveColorSide, Face solveSide, String scramble) {

        Rotate rotate = rotateCrossToSolveSide(solveSide, solveColorSide);

        Cube scrambledCube = SolvedCube.getSolvedState(solveColorSide).applyTurns(scramble);

        return solveCross(scrambledCube, solveColorSide).stream()
                .map(sol -> sol.toString(rotate))
                .collect(toList());
    }

    /**
     * Rotate cube to move cross from top layer to chosen one
     * @param solveCross face of chosen color
     * @return setup move
     */
    @NotNull
    Rotate rotateCrossToSolveSide(Face solveCross, Face solveSide) {
        if (solveCross == solveSide) {
            return Rotate.identity;
        }
        for (Rotate rotate : new Rotate[]{Rotate.z, Rotate.x, Rotate.y,}) {
            for (Direction direction : Direction.values()) {
                Rotate result = rotate.withDirection(direction);
                if (result.getOGFace(solveSide) == solveCross) {
                    return result;
                }
            }
        }
        throw new IllegalStateException("cannot solve cube rotate");
    }
}
