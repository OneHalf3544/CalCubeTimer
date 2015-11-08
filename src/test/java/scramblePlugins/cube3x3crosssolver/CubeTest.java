package scramblePlugins.cube3x3crosssolver;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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
    public void testIsCrossSolvedOn() throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.BACK);
        Cube scrambledCube = solvedState.applyTurn(new Turn(Face.FRONT, 2));
        assertTrue(scrambledCube.isCrossSolvedOn(Face.BACK));
        assertFalse(scrambledCube.isCrossSolvedOn(Face.FRONT));
        assertFalse(scrambledCube.isCrossSolvedOn(Face.UP));
    }

    @Test
    public void testTurns() throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.DOWN);
        Cube scrambledCube = solvedState.applyTurns(Rotate.identity, "U2 D' R L R' L' D U2");
        assertTrue(scrambledCube.isCrossSolvedOn(Face.DOWN));
        assertEquals(solvedState.hashEdgesOrientations(), scrambledCube.hashEdgesOrientations());
        assertEquals(solvedState.hashEdgesPositions(), scrambledCube.hashEdgesPositions());
    }

    @Test(dataProvider = "testScrambles")
    public void testEqualTurns(String scramble, String scramble2) throws Exception {
        SolvedCube solvedState = SolvedCube.getSolvedState(Face.DOWN);
        Cube scrambledCube1 = solvedState.applyTurns(Rotate.identity, scramble);
        Cube scrambledCube2 = solvedState.applyTurns(Rotate.identity, scramble2);

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