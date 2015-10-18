package net.gnehzr.cct.misc;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.statistics.SolveTime;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    @Test(dataProvider = "timesInSeconds")
    public void testFormat(SolveTime solveTime, String expectedString) throws Exception {
        String clockFormat = Utils.format(solveTime);
        assertEquals(clockFormat, expectedString);
    }

    @Test(dataProvider = "times")
    public void testClockFormat(SolveTime solveTime, String expectedString) throws Exception {
        String clockFormat = Utils.clockFormat(solveTime);
        assertEquals(clockFormat, expectedString);
    }

    @DataProvider(name = "times")
    private Object[][] getTimes() {
        return new Object[][]{
                {new SolveTime("2134234.23"), "592:50:34.23"},
                {new SolveTime("34.23"), "34.23"},
        };
    }

    @DataProvider(name = "timesInSeconds")
    private Object[][] getTimesInSeconds() {
        return new Object[][]{
                {new SolveTime("2134234.23"), "2134234.23"},
                {new SolveTime("34.23"), "34.23"},
        };
    }

    @Test
    public void testClockFormat() throws Exception {
        assertEquals(Utils.clockFormat(new SolveTime("72.2142")), "1:12.21");
    }


    @Test
    public void testGetByCircularIndex() throws Exception {
        assertEquals("1", Utils.getByCircularIndex(1, ImmutableList.of("0", "1", "2")));
        assertEquals("2", Utils.getByCircularIndex(2, ImmutableList.of("0", "1", "2")));
        assertEquals("0", Utils.getByCircularIndex(3, ImmutableList.of("0", "1", "2")));
        assertEquals("2", Utils.getByCircularIndex(-1, ImmutableList.of("0", "1", "2")));
        assertEquals("2", Utils.getByCircularIndex(-10, ImmutableList.of("0", "1", "2")));
    }


}