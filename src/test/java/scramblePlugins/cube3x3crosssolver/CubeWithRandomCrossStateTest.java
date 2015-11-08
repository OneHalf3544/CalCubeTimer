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
public class CubeWithRandomCrossStateTest {

    @Test
    public void testCanBeSolved() {
        CubeWithRandomCrossState cubeWithRandomCrossState = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)
                .applyTurn(UP, HALF_TURN)
                .applyTurn(LEFT, COUNTER_CLOCKWISE)
                .applyTurn(RIGHT, CLOCKWISE);
        assertTrue(cubeWithRandomCrossState.crossCanBeSolvedInNTurns(3));
        assertFalse(cubeWithRandomCrossState.crossCanBeSolvedInNTurns(2));

        cubeWithRandomCrossState = CubeWithSolvedCross.getSolvedState(RubicsColor.YELLOW)
                .applyTurn(UP, HALF_TURN)
                .applyTurn(LEFT, COUNTER_CLOCKWISE);
        assertTrue(cubeWithRandomCrossState.crossCanBeSolvedInNTurns(2));

        cubeWithRandomCrossState = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)
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
        assertTrue(cubeWithRandomCrossState.crossCanBeSolvedInNTurns(3));

        cubeWithRandomCrossState = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)
                .applyTurns("L' D");
        assertTrue(cubeWithRandomCrossState.crossCanBeSolvedInNTurns(1));
    }

    @Test
    public void testEqualsHashIfCrossUnmodified1() {
        CubeWithRandomCrossState cubeWithRandomCrossStateState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)
                .applyRotate(Rotate.z.doubleRotate())
                .applyTurns("R2 D2 R'");

        CubeWithRandomCrossState cubeWithRandomCrossStateState2 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)
                .applyRotate(Rotate.z.doubleRotate())
                .applyTurns("R2 D2 R' U2");

        assertEquals(cubeWithRandomCrossStateState1, cubeWithRandomCrossStateState2);
        assertEquals(cubeWithRandomCrossStateState1.hashEdgesOrientations(), cubeWithRandomCrossStateState2.hashEdgesOrientations());
        assertEquals(cubeWithRandomCrossStateState1.hashEdgesPositions(), cubeWithRandomCrossStateState2.hashEdgesPositions());
    }

    @Test
    public void testEqualsHashIfCrossUnmodified2() {
        CubeWithRandomCrossState cubeWithRandomCrossStateState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE).applyTurn(new Turn(Face.DOWN, HALF_TURN));
        CubeWithRandomCrossState cubeWithRandomCrossStateState2 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);

        assertEquals(cubeWithRandomCrossStateState1.hashEdgesOrientations(), cubeWithRandomCrossStateState2.hashEdgesOrientations());
        assertEquals(cubeWithRandomCrossStateState1.hashEdgesPositions(), cubeWithRandomCrossStateState2.hashEdgesPositions());
    }

    @Test
    public void testEqualsHashIfCrossUnmodified3() {
        CubeWithRandomCrossState cubeWithRandomCrossStateState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE).applyTurn(new Turn(Face.FRONT, HALF_TURN));
        CubeWithRandomCrossState cubeWithRandomCrossStateState2 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);

        assertNotEquals(cubeWithRandomCrossStateState1.hashEdgesOrientations(), cubeWithRandomCrossStateState2.hashEdgesOrientations());
        assertNotEquals(cubeWithRandomCrossStateState1.hashEdgesPositions(), cubeWithRandomCrossStateState2.hashEdgesPositions());
    }

    @Test
    public void testEqualsHashIfCrossUnmodified4() {
        CubeWithRandomCrossState cubeWithRandomCrossStateState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);
        CubeWithRandomCrossState cubeWithRandomCrossStateState2 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);

        assertEquals(cubeWithRandomCrossStateState1.hashEdgesOrientations(), cubeWithRandomCrossStateState2.hashEdgesOrientations());
        assertEquals(cubeWithRandomCrossStateState1.hashEdgesPositions(), cubeWithRandomCrossStateState2.hashEdgesPositions());
    }

    @Test
    public void testEqualsHashIfCrossUnmodified5() {
        CubeWithRandomCrossState cubeWithRandomCrossStateState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE).applyTurn(new Turn(UP, CLOCKWISE));
        CubeWithRandomCrossState cubeWithRandomCrossStateState2 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);

        assertNotEquals(cubeWithRandomCrossStateState1.hashEdgesPositions(), cubeWithRandomCrossStateState2.hashEdgesPositions());
        assertEquals(cubeWithRandomCrossStateState1.hashEdgesOrientations(), cubeWithRandomCrossStateState2.hashEdgesOrientations());
    }

    @Test
    public void testIsCrossSolvedOn() throws Exception {
        CubeWithRandomCrossState scrambledCubeWithRandomCrossState = CubeWithSolvedCross
                .getSolvedState(RubicsColor.BLUE)
                .applyTurn(Face.FRONT, HALF_TURN);

        assertTrue(scrambledCubeWithRandomCrossState.isCrossSolvedOn(RubicsColor.BLUE));
    }

    @Test
    public void testTurns() throws Exception {
        CubeWithSolvedCross solvedState = CubeWithSolvedCross.getSolvedState(RubicsColor.YELLOW);
        CubeWithRandomCrossState scrambledCubeWithRandomCrossState = solvedState.applyTurns("U2 D' R L R' L' D U2");
        assertTrue(scrambledCubeWithRandomCrossState.isCrossSolvedOn(RubicsColor.YELLOW));
        assertEquals(solvedState.hashEdgesOrientations(), scrambledCubeWithRandomCrossState.hashEdgesOrientations());
        assertEquals(solvedState.hashEdgesPositions(), scrambledCubeWithRandomCrossState.hashEdgesPositions());
    }

    @Test(dataProvider = "testScrambles")
    public void testEqualTurns(String scramble, String scramble2) throws Exception {
        CubeWithSolvedCross solvedState = CubeWithSolvedCross.getSolvedState(RubicsColor.YELLOW);
        CubeWithRandomCrossState scrambledCubeWithRandomCrossState1 = solvedState.applyTurns(scramble);
        CubeWithRandomCrossState scrambledCubeWithRandomCrossState2 = solvedState.applyTurns(scramble2);

        assertEquals(scrambledCubeWithRandomCrossState1.hashEdgesOrientations(), scrambledCubeWithRandomCrossState2.hashEdgesOrientations());
        assertEquals(scrambledCubeWithRandomCrossState1.hashEdgesPositions(), scrambledCubeWithRandomCrossState2.hashEdgesPositions());
    }

    @Test
    public void testEqualCubesInDifferentOrientation() throws Exception {
        CubeWithRandomCrossState solvedState1 = CubeWithSolvedCross
                .getSolvedState(RubicsColor.YELLOW)
                .applyRotate(Rotate.x)
                .applyTurns("R2 D2");

        CubeWithRandomCrossState solvedState2 = CubeWithSolvedCross
                .getSolvedState(RubicsColor.YELLOW)
                .applyRotate(Rotate.x.invert())
                .applyTurns("R2 U2");
        assertEquals(solvedState1, solvedState2);
    }

    @Test
    public void should_haveCorrectTextPresentation_when_crossInStartPosition() throws Exception {
        CubeWithRandomCrossState solvedState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);
        assertEquals(solvedState1.toTextPresentation(),
                "" +
                        "    .W.\n" +
                        "    WWW\n" +
                        "    .W.\n" +
                        ".O. .G. .R. .B.\n" +
                        ".O. .G. .R. .B.\n" +
                        "... ... ... ...\n" +
                        "    ...\n" +
                        "    .Y.\n" +
                        "    ...\n"
        );
    }

    @Test
    public void should_haveCorrectTextPresentation_when_crossIsUnchanged() throws Exception {
        CubeWithRandomCrossState solvedState1 = CubeWithSolvedCross
                .getSolvedState(RubicsColor.WHITE)
                .applyTurn(RIGHT, COUNTER_CLOCKWISE)
                .applyTurn(DOWN, HALF_TURN)
                .applyTurn(RIGHT, CLOCKWISE)
                ;

        assertEquals(solvedState1.toTextPresentation(),
                "" +
                        "    .W.\n" +
                        "    WWW\n" +
                        "    .W.\n" +
                        ".O. .G. .R. .B.\n" +
                        ".O. .G. .R. .B.\n" +
                        "... ... ... ...\n" +
                        "    ...\n" +
                        "    .Y.\n" +
                        "    ...\n"
        );
    }

    @Test
    public void should_haveCorrectTextPresentation_when_crossIsGreen() throws Exception {
        CubeWithRandomCrossState solvedState1 = CubeWithSolvedCross.getSolvedState(RubicsColor.GREEN);

        assertEquals(solvedState1.toTextPresentation(),
                "" +
                        "    ...\n" +
                        "    .W.\n" +
                        "    .W.\n" +
                        "... .G. ... ...\n" +
                        ".OO GGG RR. .B.\n" +
                        "... .G. ... ...\n" +
                        "    .Y.\n" +
                        "    .Y.\n" +
                        "    ...\n"
        );
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