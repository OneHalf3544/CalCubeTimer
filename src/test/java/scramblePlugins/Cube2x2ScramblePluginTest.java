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

public class Cube2x2ScramblePluginTest {

    private static final Logger LOG = LogManager.getLogger(Cube2x2ScramblePluginTest.class);
    private final Configuration configuration = mock(Configuration.class);
    private final ScramblePluginManager scramblePluginManager = mock(ScramblePluginManager.class);

    private Cube2x2ScramblePlugin cubeScramblePlugin = new Cube2x2ScramblePlugin();

    @Test
    public void testGenerate2x2() {
        ScrambleString cubeScramble = cubeScramblePlugin.createScramble(
                createPuzzleType("2x2x2"),
                createVariation(10, "U D"),
                Collections.<String>emptyList());
        LOG.info("cubeScramble (2x2): " + cubeScramble);
    }

    private ScrambleSettings createVariation(int length, String generator) {
        return new ScrambleSettings(configuration, scramblePluginManager, generator, length);
    }

    private PuzzleType createPuzzleType(String variationName) {
        return new PuzzleType(configuration, scramblePluginManager, cubeScramblePlugin, variationName);
    }

}