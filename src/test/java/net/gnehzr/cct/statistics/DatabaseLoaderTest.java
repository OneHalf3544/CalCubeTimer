package net.gnehzr.cct.statistics;

import org.testng.annotations.Test;

import java.io.File;

public class DatabaseLoaderTest {

    @Test
    public void testStatisticLoading() throws Exception {
        new DatabaseLoader(new Profile(
                "/tmp",
                new File("/tmp"),
                new File("/tmp/tmp.properties"),
                new File("/tmp/tmp.xml")));
    }
}