package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.SortedProperties;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.testng.Assert.assertEquals;

public class SolveTimeTest {

    Configuration configuration = new Configuration(new SortedProperties(ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of()));

    @Test
    public void testConstructor() throws Exception {
        SolveTime solveTime = new SolveTime(123.45, "R' U2 B");
        assertEquals(solveTime.getTime(), Duration.parse("PT123.45S"));
    }

    @Test
    public void testParseTime() throws Exception {
        SolveTime solveTime = new SolveTime("123.45", "R' U2 B");
        assertEquals(solveTime.getTime(), Duration.parse("PT123.45S"));
    }

    @Test(enabled = false)
    public void testSplitsToString() {
        SolveTime solveTime = new SolveTime(new TimerState(configuration, Duration.ofMinutes(2)), "R' U2", ImmutableList.of(
                new SolveTime(30.02, "R"),
                new SolveTime(89.98, "U2")
        ));
        assertThat(solveTime.toSplitsString(), containsString("jghhfug"));
    }

    @Test
    public void testEmptySplitsTimeToString() {
        assertThat(new SolveTime(123.22, "U B' D2").toSplitsString(), isEmptyString());
    }
}