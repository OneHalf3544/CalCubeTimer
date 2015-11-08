package scramblePlugins;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class Cube3x3ScramblePluginTest {

    private static final Logger LOG = LogManager.getLogger(Cube3x3ScramblePluginTest.class);
    private final Configuration configuration = mock(Configuration.class);
    private final ScramblePluginManager scramblePluginManager = mock(ScramblePluginManager.class);

    private Cube3x3ScramblePlugin cubeScramblePlugin = new Cube3x3ScramblePlugin();

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
    public void testGenerate3x3() {
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(createPuzzleType("3x3x3"), createVariation(10, "U D"), Collections.<String>emptyList());
        LOG.info("cubeScramble (3x3): " + cubeScramble);
    }

    private ScrambleSettings createVariation(int length, String generator) {
        return new ScrambleSettings(configuration, scramblePluginManager, generator, length);
    }

    private PuzzleType createPuzzleType(String variationName) {
        return new PuzzleType(configuration, scramblePluginManager, cubeScramblePlugin, variationName);
    }

}