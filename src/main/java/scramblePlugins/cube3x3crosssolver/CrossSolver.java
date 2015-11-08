package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class CrossSolver {

    public static final BiMap<Face, Character> FACES = Arrays.stream(Face.values())
            .collect(
                    ImmutableBiMap::<Face, Character>builder,
                    (r, face) -> r.put(face, face.getFaceChar()),
                    (b1, b2) -> b1.putAll(b2.build()))
            .build();

    /**
     * index - count of clockwise turns
     */
    public static final BiMap<String, Integer> DIRECTIONS = ImmutableBiMap.of(
            "'", 3,
            "2", 2,
            "", 1
    );
    public static final int MAX_TURNS = 9;

    @NotNull
    private List<CrossSolution> iddfs(@NotNull Cube scrambledCube,
                                      @NotNull SolvedCube solvedCube,
                                      byte[] pruneEO,
                                      @Nullable Turn lastTurn,
                                      int depth) {
        int hashEO = scrambledCube.hashEdgesOrientations();
        int hashEP = scrambledCube.hashEdgesPositions();

        if (depth == 0) {
            // last turn. check current state
            if (scrambledCube.isCrossSolvedOn(solvedCube.getSolvingSide())) {
                // solved!
                return singletonList(new CrossSolution(emptyList()));
            }

            // no solution
            return Collections.emptyList();
        }

        if ((pruneEO != null && pruneEO[hashEO] > depth)
                || solvedCube.prune_ep[hashEP] > depth) {
            return Collections.emptyList();
        }

        List<CrossSolution> sols = new ArrayList<>();
        for (Face f : Face.values()) {
            if (lastTurn != null && isDuplicatedTurn(lastTurn, f)) {
                continue;
            }

            for (Integer direction : DIRECTIONS.values()) {
                Turn newTurn = new Turn(f, direction);

                List<CrossSolution> newSolutions = iddfs(
                        scrambledCube.applyTurn(newTurn),
                        solvedCube,
                        pruneEO,
                        newTurn,
                        depth - 1);

                for (CrossSolution solution : newSolutions) {
                    sols.add(solution.withTurnBefore(newTurn));
                }
            }
        }
        return sols;
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

        Rotate rotate = rotateCrossToSolveSide(solveColorSide, solveSide);

        Cube scrambledCube = SolvedCube.getSolvedState(solveSide).applyTurns(rotate, scramble);

        return solveCross(scrambledCube, solveSide).stream()
                .map(sol -> sol.toString(rotate))
                .collect(Collectors.toList());
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
            for (Integer direction : DIRECTIONS.values()) {
                Rotate result = rotate.withDirection(direction);
                if (result.getOGFace(solveSide) == solveCross) {
                    return rotate.withDirection(direction);
                }
            }
        }
        throw new IllegalStateException("cannot solve cube rotate");
    }
}
