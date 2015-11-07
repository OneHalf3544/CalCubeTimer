package net.gnehzr.cct.scrambles.crosssolver;

import java.util.*;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 16:15
 * <p>
 *
 * @author OneHalf
 */
public class SolvedCube extends Cube {

    private static final Map<Face, SolvedCube> SOLVED_SATES_CACHE = new HashMap<>();
    private final Face solvingSide;

    public static SolvedCube getSolvedState(Face face) {
        return SOLVED_SATES_CACHE.computeIfAbsent(face, SolvedCube::createSolvedState);
    }

    private static SolvedCube createSolvedState(Face crossFace) {
        Boolean[] edgesOrientations = new Boolean[12];
        Integer[] edgesPosition = new Integer[12];
        int count = 0;
        for (int i : FACE_INDICES.get(crossFace)) {
            edgesOrientations[i] = false;
            edgesPosition[i] = count++;
        }
        return new SolvedCube(
                crossFace,
                Arrays.asList(edgesOrientations),
                Arrays.asList(edgesPosition));
    }

    public int[][] transitions_eo;
    public int[][] transitions_ep;
    byte[] prune_ep;

    public SolvedCube(Face crossSide,
                      List<Boolean> orientations,
                      List<Integer> positions) {
        super(orientations, positions);
        buildTables(crossSide);
        this.solvingSide = crossSide;
    }

    void buildTables(Face crossFace) {
        Boolean[] edgesOrientations = new Boolean[12];
        Integer[] edgesPosition = new Integer[12];
        int count = 0;
        for (int i : FACE_INDICES.get(crossFace)) {
            edgesOrientations[i] = false;
            edgesPosition[i] = count++;
        }
        Cube solved = new Cube(
                Arrays.asList(edgesOrientations),
                Arrays.asList(edgesPosition));
        //building transition tables
        transitions_eo = new int[solved.hash_eo_count()][6 * 3];
        for (int i = 0; i < transitions_eo.length; i++) {
            for (Face f : Face.values()) {
                solved = new Cube(unhash_eo(i), solved.edgesPosition);
                Turn turn = new Turn(f, 1);
                for (int d = 0; d < 3; d++) {
                    solved = solved.applyTurn(turn);
                    transitions_eo[i][f.ordinal() * 3 + d] = solved.hashEdgesOrientations();
                }
            }
        }
        transitions_ep = new int[solved.hash_ep_count()][6 * 3];
        for (int i = 0; i < transitions_ep.length; i++) {
            for (Face f : Face.values()) {
                solved = new Cube(solved.edgesOrientations, unhash_edgesPositions(i));
                Turn turn = new Turn(f, 1);
                for (int d = 0; d < 3; d++) {
                    solved = solved.applyTurn(turn);
                    transitions_ep[i][f.ordinal() * 3 + d] = solved.hashEdgesPositions();
                }
            }
        }

        prune_ep = new byte[solved.hash_ep_count()];
        ArrayList<Integer> fringe = new ArrayList<>();
        fringe.add(hashEdgesPositions());
        while (!fringe.isEmpty()) {
            int pos = fringe.remove(0);
            for (Face f : Face.values()) {
                for (int dir = 1; dir <= 3; dir++) {
                    int turnIndex = f.ordinal() * 3 + dir - 1;
                    int newPos = transitions_ep[pos][turnIndex];
                    if (prune_ep[newPos] == 0) {
                        prune_ep[newPos] = (byte) (prune_ep[pos] + 1);
                        fringe.add(newPos);
                    }
                }
            }
        }
    }

    public List<Integer> unhash_edgesPositions(int ep_hash) {
        List<Integer> edgesPositions = new ArrayList<>();
        for (int c = 3; c >= 0; c--) {
            int i = ep_hash % (12 - c);
            ep_hash /= (12 - c);
            for (int ch = edgesPositions.size() - 1; ch < i; ch++) {
                edgesPositions.add(null);
            }
            edgesPositions.add(i, c);
        }
        return Arrays.asList(edgesPositions.toArray(new Integer[12])).subList(0, 12);
    }

    public Face getSolvingSide() {
        return solvingSide;
    }
}
