package net.gnehzr.cct.misc;

import net.gnehzr.cct.statistics.SolveTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    @Test(dataProvider = "times")
    public void testClockFormat(SolveTime solveTime, String expectedString) throws Exception {
        String clockFormat = Utils.clockFormat(solveTime);
        assertEquals(clockFormat, expectedString);
    }

    @DataProvider(name = "times")
    private Object[][] getTimes() {
        return new Object[][]{
                {new SolveTime(2134234.23), "592:50:34.23"},
                {new SolveTime(34.23), "34.23"},
        };
    }

    @Test
    public void testClockFormat() throws Exception {
        assertEquals(Utils.clockFormat(new SolveTime(72.2142)), "1:12.21");
    }
}