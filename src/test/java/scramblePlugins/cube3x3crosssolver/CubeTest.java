package scramblePlugins.cube3x3crosssolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static scramblePlugins.cube3x3crosssolver.Direction.CLOCKWISE;
import static scramblePlugins.cube3x3crosssolver.Direction.COUNTER_CLOCKWISE;
import static scramblePlugins.cube3x3crosssolver.Direction.HALF_TURN;
import static scramblePlugins.cube3x3crosssolver.Face.*;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 19:40
 * <p>
 *
 * @author OneHalf
 */
public class CubeTest {

    @Test
    public void testCanBeSolved() {
        Cube cube = SolvedCube.getSolvedState(Face.DOWN)
                .applyTurn(UP, HALF_TURN)
                .applyTurn(LEFT, COUNTER_CLOCKWISE)
                .applyTurn(RIGHT, CLOCKWISE);
        assertTrue(cube.canBeSolvedInNTurns(3));
        assertFalse(cube.canBeSolvedInNTurns(2));

        cube = SolvedCube.getSolvedState(Face.DOWN)
                .applyTurn(UP, HALF_TURN)
                .applyTurn(LEFT, COUNTER_CLOCKWISE);
        assertTrue(cube.canBeSolvedInNTurns(2));

        cube = SolvedCube.getSolvedState(Face.UP)
                .applyTurn(UP, HALF_TURN)
                .applyTurn(LEFT, COUNTER_CLOCKWISE)
                .applyTurn(DOWN, CLOCKWISE)
                .applyTurn(RIGHT, CLOCKWISE)
                .applyTurn(UP, COUNTER_CLOCKWISE)
                .applyTurn(LEFT, COUNTER_CLOCKWISE)
                .applyTurn(FRONT, COUNTER_CLOCKWISE)
                .applyTurn(LEFT, COUNTER_CLOCKWISE)
                .applyTurn(UP, CLOCKWISE)
                .applyTurn(BACK, HALF_TURN)
                .applyTurn(RIGHT, CLOCKWISE)
                .applyTurn(UP, CLOCKWISE)
                .applyTurn(FRONT, HALF_TURN)
                .applyTurn(RIGHT, HALF_TURN)
                .applyTurn(DOWN, HALF_TURN)
                .applyTurn(LEFT, HALF_TURN)
                .applyTurn(FRONT, HALF_TURN)
                .applyTurn(DOWN, CLOCKWISE)
                .applyTurn(FRONT, HALF_TURN)
                .applyTurn(BACK, HALF_TURN);
        assertTrue(cube.canBeSolvedInNTurns(3));

        cube = SolvedCube.getSolvedState(UP)
                .applyTurns("L' D");
        assertTrue(cube.canBeSolvedInNTurns(1));
    }

    @Test
    public void testEqualsHashIfCrossUnmodified() {
        Cube cubeState1;
        Cube cubeState2;

        cubeState1 = SolvedCube.getSolvedState(DOWN)
                .applyTurns("R2 D2 R'");
        cubeState2 = SolvedCube.getSolvedState(DOWN)
                .applyTurns("R2 D2 R' U2");

        assertEquals(cubeState1.hashEdgesOrientations(), cubeState2.hashEdgesOrientations());
        assertEquals(cubeState1.hashEdgesPositions(), cubeState2.hashEdgesPositions());

        cubeState1 = SolvedCube.getSolvedState(UP).applyTurn(new Turn(Face.DOWN, HALF_TURN));
        cubeState2 = SolvedCube.getSolvedState(UP);

        assertEquals(cubeState1.hashEdgesOrientations(), cubeState2.hashEdgesOrientations());
        assertEquals(cubeState1.hashEdgesPositions(), cubeState2.hashEdgesPositions());

        cubeState1 = SolvedCube.getSolvedState(UP).applyTurn(new Turn(Face.FRONT, HALF_TURN));
        cubeState2 = SolvedCube.getSolvedState(UP);

        assertNotEquals(cubeState1.hashEdgesOrientations(), cubeState2.hashEdgesOrientations());
        assertNotEquals(cubeState1.hashEdgesPositions(), cubeState2.hashEdgesPositions());

        cubeState1 = SolvedCube.getSolvedState(UP);
        cubeState2 = SolvedCube.getSolvedState(UP);

        assertEquals(cubeState1.hashEdgesOrientations(), cubeState2.hashEdgesOrientations());
        assertEquals(cubeState1.hashEdgesPositions(), cubeState2.hashEdgesPositions());

        cubeState1 = SolvedCube.getSolvedState(UP).applyTurn(new Turn(UP, CLOCKWISE));
        cubeState2 = SolvedCube.getSolvedState(UP);

        assertNotEquals(cubeState1.hashEdgesPositions(), cubeState2.hashEdgesPositions());
        assertEquals(cubeState1.hashEdgesOrientations(), cubeState2.hashEdgesOrientations());
    }

    @Test
    public void testIsCrossSolvedOn() throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.BACK);
        Cube scrambledCube = solvedState.applyTurn(new Turn(Face.FRONT, HALF_TURN));
        assertTrue(scrambledCube.isCrossSolvedOn(Face.BACK));
        assertFalse(scrambledCube.isCrossSolvedOn(Face.FRONT));
        assertFalse(scrambledCube.isCrossSolvedOn(UP));
    }

    @Test
    public void testTurns() throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.DOWN);
        Cube scrambledCube = solvedState.applyTurns("U2 D' R L R' L' D U2");
        assertTrue(scrambledCube.isCrossSolvedOn(Face.DOWN));
        assertEquals(solvedState.hashEdgesOrientations(), scrambledCube.hashEdgesOrientations());
        assertEquals(solvedState.hashEdgesPositions(), scrambledCube.hashEdgesPositions());
    }

    @Test(dataProvider = "testScrambles")
    public void testEqualTurns(String scramble, String scramble2) throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.DOWN);
        Cube scrambledCube1 = solvedState.applyTurns(scramble);
        Cube scrambledCube2 = solvedState.applyTurns(scramble2);

        assertEquals(scrambledCube1.hashEdgesOrientations(), scrambledCube2.hashEdgesOrientations());
        assertEquals(scrambledCube1.hashEdgesPositions(), scrambledCube2.hashEdgesPositions());
    }

    @DataProvider
    public Object[][] testScrambles() throws Exception {
        return new String[][]{
                {"U", "U' U2"},
                {"U", "U' U' U'"},
                {"D'", "D2 D"},
                {"D2 R2 L2 D2 R2 L2", "L2 R2 U2 L2 R2"},
        };
    }
}