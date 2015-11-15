package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.testng.Assert.assertEquals;

public class CrossSolverTest {

    private static final Logger LOG = LogManager.getLogger(CrossSolverTest.class);

    private static final CrossSolver CROSS_SOLVER = new CrossSolver();

    @Test
    public void testSolveCrossForSolvedCube() throws Exception {
        assertThat(
                CROSS_SOLVER.solveCross(Face.DOWN, Face.DOWN, ""),
                is(ImmutableList.of("")));
        assertThat(
                CROSS_SOLVER.solveCross(Face.UP, Face.DOWN, ""),
                is(ImmutableList.of("z2 ")));
        assertThat(
                CROSS_SOLVER.solveCross(Face.DOWN, Face.DOWN, ""),
                is(ImmutableList.of("")));
    }

    @Test
    public void testRotateToCrossSide() throws Exception {
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.UP, Face.UP),
                Rotate.identity);
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.DOWN, Face.UP),
                Rotate.z.doubleRotate());
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.UP,Face.DOWN),
                Rotate.z.doubleRotate());
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.LEFT, Face.UP),
                Rotate.z);
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.FRONT, Face.UP),
                Rotate.x);
        assertEquals(
                CROSS_SOLVER.rotateCrossToSolveSide(Face.BACK, Face.FRONT),
                Rotate.x.doubleRotate());
    }

    @Test
    public void testSolveCrossInOneStep() throws Exception {
        assertThat(
                CROSS_SOLVER.solveCross(Face.UP, Face.DOWN, "F"),
                is(ImmutableList.of("z2 F'")));
    }

    @Test
    public void testSolveOnTheSameSide() throws Exception {
        assertThat(
                CROSS_SOLVER.solveCross(Face.UP, Face.UP, "F"),
                is(ImmutableList.of("F'")));
    }

    @Test(dataProvider = "solveCrossExamples")
    public void testSolveCross(Face color, Face side, String scramble, Collection<String> scrambles) throws Exception {
        assertThat(CROSS_SOLVER.solveCross(color, side, scramble), containsInAnyOrder(scrambles.toArray()));
    }

    @Test
    public void testLastTurnNotParallelToCross() {
        assertThat(CROSS_SOLVER.solveCross(Face.UP, Face.DOWN,
                   "U2 L' D R U' L' F' L' U B2 R U F2 R2 D2 L2 F2 D F2 B2"),
                    not(hasItem(anyOf(endsWith("U"), endsWith("U'"), endsWith("U2")))));
    }

    @DataProvider
    private Object[][] solveCrossExamples() {
        return new Object[][] {
                {Face.UP, Face.DOWN,
                        "U2 L' D R U' L' F' L' U B2 R U F2 R2 D2 L2 F2 D F2 B2",
                        ImmutableList.of("z2 R D2 R2"),},
                {Face.UP, Face.UP,
                        "B' F D U2",
                        ImmutableList.of("U2 B F'"),},
                {Face.UP, Face.DOWN,
                        "B' F D U2",
                        ImmutableList.of("z2 D2 B F'"),},
                {Face.UP, Face.UP,
                        "B' F D U2",
                        ImmutableList.of("U2 B F'"),
                },

                {Face.UP, Face.DOWN,
                        "B' F D U2 L2 R2 D B F' L' F2 L' R2 D' U' B' R' U2 F D L R D2 U L2",
                        ImmutableSet.of(
                                "z2 F2 L' F' D R' B2 L'", "z2 D F L B2 R' U' F2", "z2 D F L B2 U' R' F2", "z2 D F L' D L2 B2 R'",
                                "z2 D F L' D B2 R' L2", "z2 D F L' D R' L2 B2", "z2 D F L' D R' B2 L2", "z2 D F R' L B2 U' F2",
                                "z2 D L' F D L B2 R'", "z2 D L' F D B2 R' L", "z2 D L' F D R' L B2", "z2 D L' F D R' B2 L",
                                "z2 D B2 F R' L U F2", "z2 D B2 F R' U L F2",  "z2 D B2 D R F D' R'", "z2 L B L2 D' F' L D2",
                                "z2 L' F D B2 R' U L2", "z2 L' F D B2 U R' L2", "z2 L' F D R' B2 U L2", "z2 L' F U R2 D R L2",
                                "z2 L' F' D' L D2 L' B2", "z2 L' F' D' B2 U2 F2 R'", "z2 L' D2 F' D L D2 B2", "z2 L' D2 F' L2 D' B2 R",
                                "z2 B2 D F R D' R2 L", "z2 B2 D R F D' R' L", "z2 B2 L' F R' U F2 L2", "z2 B2 L' F R' U L2 F2",
                                "z2 U L F' R2 B L2 D", "z2 U L B F' R2 L2 D", "z2 U L B R2 F' L2 D", "z2 U R2 L B F' L2 D",
                                "z2 U R2 L' D2 B F' D'"
                        )
                },
        };
    }
}