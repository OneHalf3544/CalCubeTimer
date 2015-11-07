package net.gnehzr.cct.scrambles.crosssolver;

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

    protected final List<Boolean> edgesOrientations;
    protected final List<Integer> edgesPosition;

    private int eo_solved_hash = 0;
    private int ep_solved_hash = 0;

    Cube(List<Boolean> edgesOrientations, List<Integer> edgesPosition) {
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

        return new Cube(edgesOrientations, edgesPosition);
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

    public int hash_eo_count() {
        return 9 * 9 * 9 * 9 * 2 * 2 * 2 * 2 + 2 * 2 * 2 * 2;
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

    public List<Boolean> unhash_eo(int eo_hash) {
        Boolean[] ordinal_orientations = new Boolean[4];
        int ordinal_o = eo_hash & 0xF;
        ordinal_orientations[0] = ((ordinal_o & 0x8) != 0);
        ordinal_orientations[1] = ((ordinal_o & 0x4) != 0);
        ordinal_orientations[2] = ((ordinal_o & 0x2) != 0);
        ordinal_orientations[3] = ((ordinal_o & 0x1) != 0);
        int edges = eo_hash >> 4;
        int i0 = edges % 9;
        edges = edges / 9;
        int i1 = edges % (9 - i0);
        edges = edges / (9 - i0);
        int i2 = edges % (9 - i0 - i1);
        edges = edges / (9 - i0 - i1);
        int i3 = edges % (9 - i0 - i1 - i2);
        Boolean[] eo = new Boolean[12];
        eo[i0] = ordinal_orientations[0];
        eo[1 + i0 + i1] = ordinal_orientations[1];
        eo[2 + i0 + i1 + i2] = ordinal_orientations[2];
        eo[3 + i0 + i1 + i2 + i3] = ordinal_orientations[3];
        return Arrays.asList(eo);
    }

    public int hash_ep_count() {
        return 12 * 11 * 10 * 9;// (12! / 8!)
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

    public boolean isCrossSolvedOn(Face face) {
        SolvedCube solvedState = SolvedCube.getSolvedState(face);
        return solvedState.hashEdgesOrientations() == this.hashEdgesOrientations()
                && solvedState.hashEdgesPositions() == this.hashEdgesPositions();
    }
}
