package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.SortedProperties;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.KeyboardTimerState;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.testng.Assert.assertEquals;

public class SolveTimeTest {

    Configuration configuration = new Configuration(new SortedProperties(ImmutableMap.<String, String>of(), ImmutableMap.<String, String>of()));

    @Test
    public void testParseDouble() {
        SolveTime solveTime = new SolveTime("123.45");
        assertEquals(solveTime.getTime(), Duration.parse("PT123.45S"));
    }

    @Test
    public void testParseString() {
        SolveTime solveTime = new SolveTime("123.45");
        assertEquals(solveTime.getTime(), Duration.parse("PT123.45S"));
    }

    @Test
    public void testParseStringWithMinutes() {
        SolveTime solveTime = new SolveTime("2:03.45");
        assertEquals(solveTime.getTime(), Duration.parse("PT123.45S"));
    }

    @Test(enabled = false)
    public void testSplitsToString() {
        Solution solveTime = new Solution(new KeyboardTimerState(Duration.ofMinutes(2), Optional.<InspectionState>empty()),
                null, ImmutableList.of(
                        new SolveTime("30.02"),
                        new SolveTime("89.98")
        ));
        assertThat(solveTime.toSplitsString(), containsString("jghhfug"));
    }

    @Test
    public void testEmptySplitsTimeToString() {
        assertThat(new Solution(new SolveTime("123.22"), new ScrambleString(null, "", true, null, null, "")).toSplitsString(), isEmptyString());
    }
}