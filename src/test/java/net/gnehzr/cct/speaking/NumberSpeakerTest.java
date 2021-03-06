package net.gnehzr.cct.speaking;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SolveTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Objects;

import static org.mockito.Mockito.mock;

public class NumberSpeakerTest {

    private static final Logger LOG = LogManager.getLogger(NumberSpeakerTest.class);

    private NumberSpeaker numberSpeaker = new NumberSpeaker(mock(Configuration.class));

    @Test(enabled = false)
    public void testPlay() throws Exception {
        NumberSpeaker carrie = Objects.requireNonNull(numberSpeaker.getSpeaker("carrie"));
        for(int ch = 12000; ch < 13000; ch+=10) {
            LOG.info("TIME: " + ch / 100.);
            carrie.speak(false, new SolveTime(Duration.ofMillis(ch * 10)));
        }
    }
}