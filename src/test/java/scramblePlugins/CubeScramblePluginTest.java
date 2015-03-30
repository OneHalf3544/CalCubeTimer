package scramblePlugins;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class CubeScramblePluginTest {

    private static final Logger LOG = LogManager.getLogger(CubeScramblePluginTest.class);

    private CubeScramblePlugin cubeScramblePlugin = new CubeScramblePlugin();

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
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(createVariation("2x2x2", 10), "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (2x2): " + cubeScramble);
    }

    @Test
    public void testGenerate3x3() {
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(createVariation("3x3x3", 10), "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (3x3): " + cubeScramble);
    }

    @Test
    public void testGenerate6x6() {
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(createVariation("6x6x6", 10), "U D", Collections.<String>emptyList());
        LOG.info("cubeScramble (6x6): " + cubeScramble);
    }

    private ScrambleVariation createVariation(String variationName, int length) {
        ScrambleVariation scrambleVariation = new ScrambleVariation(cubeScramblePlugin, variationName, mock(Configuration.class), mock(ScramblePluginManager.class));
        scrambleVariation.setLength(length);
        return scrambleVariation;
    }

}