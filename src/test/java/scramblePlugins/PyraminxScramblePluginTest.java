package scramblePlugins;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class PyraminxScramblePluginTest {

    private static final Logger LOG = LogManager.getLogger(CubeScramblePluginTest.class);

    private final Configuration configuration = mock(Configuration.class);
    private final ScramblePluginManager scramblePluginManager = mock(ScramblePluginManager.class);

    private PyraminxScramblePlugin pyraminxScramblePlugin = new PyraminxScramblePlugin();

    @Test
    public void testGeneratePyraminx() {
        PuzzleType puzzleType = new PuzzleType(configuration, scramblePluginManager, pyraminxScramblePlugin, "Pyraminx");
        ScrambleString cubeScramble = pyraminxScramblePlugin.createScramble(puzzleType, createVariation(10), Collections.<String>emptyList());
        LOG.info("Scramble (pyraminx): " + cubeScramble);
    }

    private ScrambleSettings createVariation(int length) {
        return new ScrambleSettings(configuration, scramblePluginManager, "", 0).withLength(length);
    }
}