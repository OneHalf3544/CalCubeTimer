package net.gnehzr.cct.statistics;

import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertEquals;

public class SolveTimeTest {

    @Test
    public void testConstructor() throws Exception {
        SolveTime solveTime = new SolveTime(123.45, "R' U2 B");
        assertEquals(solveTime.hundredths, Duration.parse("PT123.45S"));
    }

    @Test
    public void testParseTime() throws Exception {
        SolveTime solveTime = new SolveTime("123.45", "R' U2 B");
        assertEquals(solveTime.hundredths, Duration.parse("PT123.45S"));
    }
}