package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CrossSolverTest {

    private static final Logger LOG = Logger.getLogger(CrossSolverTest.class);

    @Test
    public void testSolveCross() throws Exception {

        assertEquals(CrossSolver.solveCross('U', 'D', "B' F D U2 L2 R2 D B F' L' F2 L' R2 D' U' B' R' U2 F D L R D2 U L2"),
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
                ));
		assertEquals(CrossSolver.solveCross('U', 'D', "B' F D U2"), ImmutableList.of("z2 D2 B F'"));
        assertEquals(CrossSolver.solveCross('U', 'U', "B' F D U2"), ImmutableList.of("U2 B F'"));
    }
}