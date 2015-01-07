package net.gnehzr.cct.misc;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void testClockFormat() throws Exception {
        assertEquals(Utils.clockFormat(72.2142), "1:12:21");
    }
}