package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.ImmutableMap;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 14:05
 * <p>
 *
 * @author OneHalf
 */
class Cube {

    protected static final Map<Face, int[]> FACE_INDICES = ImmutableMap.<Face, int[]>builder()
            .put(Face.FRONT, new int[]{0, 4, 8, 5})
            .put(Face.BACK, new int[]{2, 6, 10, 7})
            .put(Face.LEFT, new int[]{1, 5, 9, 6})
            .put(Face.RIGHT, new int[]{3, 7, 11, 4})
            .put(Face.UP, new int[]{0, 1, 2, 3})
            .put(Face.DOWN, new int[]{11, 10, 9, 8})
            .build();

    public static final int HASH_EDGES_ORIENTATION_COUNT = 9 * 9 * 9 * 9 * 2 * 2 * 2 * 2 + 2 * 2 * 2 * 2;
    public static final int HASH_EDGES_POSITION_COUNT = 12 * 11 * 10 * 9; // (12! / 8!)

    protected final Face solvingSide;
    protected final List<Boolean> edgesOrientations;
    protected final List<Integer> edgesPosition;

    private int eo_solved_hash = 0;
    private int ep_solved_hash = 0;

    Cube(Face solvingSide, List<Boolean> edgesOrientations, List<Integer> edgesPosition) {
        this.solvingSide = solvingSide;
        checkArgument(edgesOrientations.size() == 12,
                "wrong edgesOrientations size: %s", edgesOrientations.size());
        checkArgument(edgesPosition.size() == 12,
                "wrong edgesPositions size: %s", edgesPosition.size());

        this.edgesOrientations = edgesOrientations;
        this.edgesPosition = edgesPosition;
    }


    public Cube applyTurns(Rotate r, String turns) {
        Cube c = this;
        for (String turn : turns.split(" ")) {
            if (turn.isEmpty()) {
                continue;
            }
            Character face = turn.charAt(0);
            String dir = turn.substring(1);
            c = c.applyTurn(new Turn(r.getOGFace(CrossSolver.FACES.inverse().get(face)), CrossSolver.DIRECTIONS.get(dir)));
        }
        return c;
    }

    Cube applyTurn(Turn turn) {
        int direction = turn.direction;
        List<Boolean> edgesOrientations = new ArrayList<>(this.edgesOrientations);
        List<Integer> edgesPosition = new ArrayList<>(this.edgesPosition);
        for (int i = 1; i <= direction; i++) {
            int[] indices = FACE_INDICES.get(turn.face);
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

        return new Cube(solvingSide, edgesOrientations, edgesPosition);
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

    public String toString() {
        return edgesOrientations + " " + edgesPosition;
    }

    public int hashEdgesOrientations() {
        if (eo_solved_hash != 0) {
            return eo_solved_hash;
        }

        eo_solved_hash = 0;
        int orientations = 0;
        int distance_to_last_edge = 0;
        int sum = 0;
        int shift = 1;
        for (Boolean anEo : edgesOrientations) {
            if (anEo != null) {
                orientations <<= 1;
                if (anEo) {
                    orientations++;
                }
                eo_solved_hash += shift * distance_to_last_edge;
                shift *= 9 - sum;
                sum += distance_to_last_edge;
                distance_to_last_edge = 0;
            } else
                distance_to_last_edge++;
        }
        eo_solved_hash = (eo_solved_hash << 4) | orientations;
        return eo_solved_hash;
    }

    public int hashEdgesPositions() {
        if (ep_solved_hash != 0) {
            return ep_solved_hash;
        }

        ep_solved_hash = 0;
        List<Integer> edges = new ArrayList<>(edgesPosition);
        for (int c = 0; c < 4; c++) {
            int i = edges.indexOf(c);
            edges.remove(i);
            ep_solved_hash *= 12 - c;
            ep_solved_hash += i;
        }
        return ep_solved_hash;
    }

    public boolean equals(Cube other) {
        return Objects.equals(edgesOrientations, other.edgesOrientations)
                && Objects.equals(edgesPosition, other.edgesPosition);
    }

    public boolean equals(Object obj) {
        return obj instanceof Cube && this.equals((Cube) obj);
    }

    @Override
    public int hashCode() {
        return hashEdgesOrientations() + HASH_EDGES_ORIENTATION_COUNT * hashEdgesPositions();
    }

    public boolean isCrossSolvedOn(Face face) {
        SolvedCube solvedState = SolvedCube.getSolvedState(face);
        return solvedState.hashEdgesOrientations() == this.hashEdgesOrientations()
                && solvedState.hashEdgesPositions() == this.hashEdgesPositions();
    }

    public Face getSolvingSide() {
        return solvingSide;
    }

    public boolean canBeSolvedInNTurns(int turnsToSolve) {
        return SolvedCube.getSolvedState(this.solvingSide)
                .prune_ep[this.hashEdgesPositions()] <= turnsToSolve;
    }

}
