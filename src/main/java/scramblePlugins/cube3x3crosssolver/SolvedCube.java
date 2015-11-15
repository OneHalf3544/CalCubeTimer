package scramblePlugins.cube3x3crosssolver;

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

    byte[] prune_ep;

    public static SolvedCube getSolvedState(Face face) {
        return SOLVED_SATES_CACHE.computeIfAbsent(face, SolvedCube::createSolvedState);
    }

    private static SolvedCube createSolvedState(Face crossFace) {
        Cube cube = createSolvedCube(crossFace);
        return new SolvedCube(
                crossFace,
                cube.edgesOrientations,
                cube.edgesPosition);
    }

    private static Cube createSolvedCube(Face crossFace) {
        Boolean[] edgesOrientations = new Boolean[12];
        Integer[] edgesPosition = new Integer[12];
        int count = 0;
        for (int i : FACE_INDICES.get(crossFace)) {
            edgesOrientations[i] = false;
            edgesPosition[i] = count++;
        }
        return new Cube(
                crossFace,
                Arrays.asList(edgesOrientations),
                Arrays.asList(edgesPosition));
    }

    public SolvedCube(Face crossSide,
                      List<Boolean> orientations,
                      List<Integer> positions) {
        super(crossSide, orientations, positions);
        buildTables(crossSide);
    }

    void buildTables(Face crossFace) {
        Cube solved = createSolvedCube(crossFace);

        prune_ep = new byte[HASH_EDGES_POSITION_COUNT];
        Queue<Cube> fringe = new LinkedList<>();
        fringe.add(solved);
        while (!fringe.isEmpty()) {
            Cube position = fringe.poll();
            for (Face f : Face.values()) {
                for (int dir : CrossSolver.DIRECTIONS.values()) {
                    Cube newPos = position.applyTurn(new Turn(f, dir));

                    byte currentSolveTurns = prune_ep[newPos.hashEdgesPositions()];
                    byte newSolveTurns = (byte) (prune_ep[position.hashEdgesPositions()] + 1);

                    if (currentSolveTurns == 0 || currentSolveTurns > newSolveTurns) {
                        prune_ep[newPos.hashEdgesPositions()] = newSolveTurns;
                        fringe.add(newPos);
                    }
                }
            }
        }
    }

}
