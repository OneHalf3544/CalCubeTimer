package scramblePlugins;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class PyraminxScramblePluginTest {


    private static final Logger LOG = LogManager.getLogger(CubeScramblePluginTest.class);

    private PyraminxScramblePlugin pyraminxScramblePlugin = new PyraminxScramblePlugin();

    @Test
    public void testGeneratePyraminx() {
        ScrambleString cubeScramble = pyraminxScramblePlugin.createScramble(createVariation("Pyraminx", 10), Collections.<String>emptyList());
        LOG.info("Scramble (pyraminx): " + cubeScramble);
    }

    private ScrambleVariation createVariation(String variationName, int length) {
        ScrambleVariation scrambleVariation = new ScrambleVariation(pyraminxScramblePlugin, variationName, mock(Configuration.class), mock(ScramblePluginManager.class), "");
        scrambleVariation.setLength(length);
        return scrambleVariation;
    }
}