package net.gnehzr.cct.misc;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SolveTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    private Configuration configuration;

    @BeforeClass
    public void setUp() throws Exception {
        configuration = new Configuration(Configuration.getRootDirectory());
    }

    @Test(dataProvider = "times")
    public void testClockFormat(SolveTime solveTime, String expectedString) throws Exception {
        String clockFormat = Utils.clockFormat(solveTime);
        assertEquals(clockFormat, expectedString);
    }

    @DataProvider(name = "times")
    private Object[][] getTimes() {
        return new Object[][] {
                {new SolveTime(2134234.23, "F' U2 R", configuration), "592:50:2.30"},
                {new SolveTime(34.23, "F' U2 R", configuration), "592:50:2.30"},
        };

    @Test
    public void testClockFormat() throws Exception {
        assertEquals(Utils.clockFormat(72.2142), "1:12:21");
    }
}