package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static scramblePlugins.cube3x3crosssolver.Face.*;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 14:05
 * <p>
 *
 * @author OneHalf
 */
class CubeWithRandomCrossState {

    static final Map<Face, int[]> FACE_INDEXES = ImmutableMap.<Face, int[]>builder()
            .put(UP, new int[]{0, 1, 2, 3})
            .put(Face.FRONT, new int[]{0, 4, 8, 5})
            .put(Face.LEFT, new int[]{1, 5, 9, 6})
            .put(Face.RIGHT, new int[]{3, 7, 11, 4})
            .put(Face.DOWN, new int[]{11, 10, 9, 8})
            .put(Face.BACK, new int[]{2, 6, 10, 7})
            .build();

    private static final int HASH_EDGES_ORIENTATION_COUNT = 9 * 9 * 9 * 9 * 2 * 2 * 2 * 2 + 2 * 2 * 2 * 2;
    static final int HASH_EDGES_POSITION_COUNT = 12 * 11 * 10 * 9; // (12! / 8!)

    private final RubicsColor solvingColor;

    final List<Boolean> edgesOrientations;
    final List<Integer> edgesPosition;

    private final Rotate currentOrientation;
    private final ScrambleString scrambleString;

    private int edgesOrient_solved_hash = 0;
    private int edgesPosition_solved_hash = 0;

    CubeWithRandomCrossState(RubicsColor solvingColor, List<Boolean> edgesOrientations, List<Integer> edgesPosition, ScrambleString scrambleString) {
        this(solvingColor, edgesOrientations, edgesPosition, Rotate.identity, scrambleString);
    }

    private CubeWithRandomCrossState(RubicsColor solvingColor, List<Boolean> edgesOrientations,
                                     List<Integer> edgesPosition, Rotate cubeOrientation,
                                     ScrambleString scrambleString) {
        checkArgument(edgesOrientations.size() == 12,
                "wrong edgesOrientations size: %s", edgesOrientations.size());
        checkArgument(edgesPosition.size() == 12,
                "wrong edgesPositions size: %s", edgesPosition.size());

        this.solvingColor = solvingColor;
        this.scrambleString = scrambleString;
        this.edgesOrientations = edgesOrientations;
        this.edgesPosition = edgesPosition;
        this.currentOrientation = cubeOrientation;
    }

    public CubeWithRandomCrossState applyTurns(String turns) {
        CubeWithRandomCrossState cubeWithRandomCrossState = this;
        for (String turn : turns.split(" ")) {
            if (turn.isEmpty()) {
                continue;
            }
            Character faceChar = turn.charAt(0);
            String dir = turn.substring(1);
            cubeWithRandomCrossState = cubeWithRandomCrossState
                    .applyTurn(new Turn(CrossSolver.FACES.inverse().get(faceChar), Direction.byStringCode(dir)));
        }
        return cubeWithRandomCrossState;
    }

    public CubeWithRandomCrossState applyTurn(Face face, Direction direction) {
        return applyTurn(new Turn(face, direction));
    }

    public CubeWithRandomCrossState applyTurn(Turn originalTurn) {
        Turn turn = currentOrientation.getOGTurn(originalTurn);
        List<Boolean> edgesOrientations = new ArrayList<>(this.edgesOrientations);
        List<Integer> edgesPosition = new ArrayList<>(this.edgesPosition);

        for (int i = 1; i <= turn.direction.getClockwiseTurnCount(); i++) {
            int[] indices = FACE_INDEXES.get(turn.face);
            if (turn.face == Face.FRONT || turn.face == Face.BACK) {
                for (int index : indices) {
                    if (edgesOrientations.get(index) == null) {
                        continue;
                    }
                    // flip edge
                    edgesOrientations.set(index, !edgesOrientations.get(index));
                }
            }
            cycle(edgesOrientations, indices);
            cycle(edgesPosition, indices);
        }

        return new CubeWithRandomCrossState(solvingColor, edgesOrientations, edgesPosition, currentOrientation, concatScrambleString(originalTurn));
    }

    @NotNull
    private ScrambleString concatScrambleString(SolveStep solveStep) {
        return new ScrambleString(
                scrambleString.getPuzzleType(),
                scrambleString.getScramble() + " " + solveStep.getNotation(),
                false,
                scrambleString.getVariation(),
                scrambleString.getScramblePlugin(),
                scrambleString.getTextComments());
    }

    private <H> void cycle(List<H> arr, int[] indices) {
        cycle(arr, indices[0], indices[1], indices[2], indices[3]);
    }

    private <H> void cycle(List<H> arr, int i, int j, int k, int l) {
        H temp = arr.get(l);
        arr.set(l, arr.get(k));
        arr.set(k, arr.get(j));
        arr.set(j, arr.get(i));
        arr.set(i, temp);
    }

    @Override
    public String toString() {
        return solvingColor + " cross: " + scrambleString.getScramble();
    }

    public int hashEdgesOrientations() {
        if (edgesOrient_solved_hash != 0) {
            return edgesOrient_solved_hash;
        }

        edgesOrient_solved_hash = 0;
        int orientations = 0;
        int distanceToLastEdge = 0;
        int sum = 0;
        int shift = 1;
        for (Boolean edgeOrientation : edgesOrientations) {
            if (edgeOrientation != null) {
                orientations <<= 1;
                if (edgeOrientation) {
                    orientations++;
                }
                edgesOrient_solved_hash += shift * distanceToLastEdge;
                shift *= 9 - sum;
                sum += distanceToLastEdge;
                distanceToLastEdge = 0;
            } else
                distanceToLastEdge++;
        }
        edgesOrient_solved_hash = (edgesOrient_solved_hash << 4) | orientations;
        return edgesOrient_solved_hash;
    }

    int hashEdgesPositions() {
        if (edgesPosition_solved_hash != 0) {
            return edgesPosition_solved_hash;
        }

        edgesPosition_solved_hash = 0;
        List<Integer> edges = new ArrayList<>(edgesPosition);
        for (int c : FACE_INDEXES.get(getSolvingColor().getFace())) {
            int i = edges.indexOf(c);
            edges.remove(i);
            edgesPosition_solved_hash *= 12 - c;
            edgesPosition_solved_hash += i;
        }
        return edgesPosition_solved_hash;
    }

    public boolean equals(CubeWithRandomCrossState other) {
        return Objects.equals(edgesOrientations, other.edgesOrientations)
                && Objects.equals(edgesPosition, other.edgesPosition);
    }

    public boolean equals(Object obj) {
        return obj instanceof CubeWithRandomCrossState && this.equals((CubeWithRandomCrossState) obj);
    }

    @Override
    public int hashCode() {
        return hashEdgesOrientations() + HASH_EDGES_ORIENTATION_COUNT * hashEdgesPositions();
    }

    public boolean isCrossSolvedOn(RubicsColor crossColor) {
        checkState(this.getSolvingColor() == crossColor);

        CubeWithSolvedCross solvedState = CubeWithSolvedCross.getSolvedState(crossColor);
        return solvedState.equals(this);
    }

    public RubicsColor getSolvingColor() {
        return solvingColor;
    }

    public boolean crossCanBeSolvedInNTurns(int turnsToSolve) {
        return CubeWithSolvedCross.getSolvedState(this.solvingColor)
                .prunePositions[this.hashEdgesPositions()] <= turnsToSolve;
    }

    public CubeWithRandomCrossState applyRotate(Rotate rotate) {
        return new CubeWithRandomCrossState(solvingColor, edgesOrientations, edgesPosition, currentOrientation.plus(rotate), concatScrambleString(rotate));
    }

    public String toTextPresentation() {
        return new CubeCrossOutputter(this).toTextPresentation();
    }

    public Rotate getCurrentOrientation() {
        return currentOrientation;
    }
}
