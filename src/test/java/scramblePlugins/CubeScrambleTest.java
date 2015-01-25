package scramblePlugins;

import org.apache.log4j.Logger;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.testng.annotations.Test;

import java.util.Collections;

public class CubeScrambleTest {

    private static final Logger LOG = Logger.getLogger(CubeScrambleTest.class);

    @Test
    public void testMain() {
        long start = System.nanoTime();
        LOG.info(Search.solution(Tools.randomCube(), 21, 10, false));
        LOG.info((System.nanoTime() - start) / 1e9);

        start = System.nanoTime();
        LOG.info(Search.solution(Tools.randomCube(), 21, 10, false));
        LOG.info((System.nanoTime() - start) / 1e9);
    }

    @Test
    public void testGenerate2x2() {
        CubeScramble cubeScramble = new CubeScramble("2x2x2", 10, "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (2x2): " + cubeScramble);
    }

    @Test
    public void testGenerate3x3() {
        CubeScramble cubeScramble = new CubeScramble("3x3x3", 10, "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (3x3): " + cubeScramble);
    }

    @Test
    public void testGenerate6x6() {
        CubeScramble cubeScramble = new CubeScramble("6x6x6", 10, "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (6x6): " + cubeScramble);
    }

}