package scramblePlugins;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class CubeScramblePluginTest {

    private static final Logger LOG = LogManager.getLogger(CubeScramblePluginTest.class);

    private final Configuration configuration = mock(Configuration.class);
    private final ScramblePluginManager scramblePluginManager = mock(ScramblePluginManager.class);

    private CubeScramblePlugin cubeScramblePlugin = new CubeScramblePlugin();

    @Test
    public void testGenerate6x6() {
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(createPuzzleType("6x6x6"), createVariation(10, "U D"), Collections.<String>emptyList());
        LOG.info("cubeScramble (6x6): " + cubeScramble);
    }

    private ScrambleSettings createVariation(int length, String generator) {
        return new ScrambleSettings(configuration, scramblePluginManager, generator, length);
    }

    private PuzzleType createPuzzleType(String variationName) {
        return new PuzzleType(configuration, scramblePluginManager, cubeScramblePlugin, variationName);
    }

}