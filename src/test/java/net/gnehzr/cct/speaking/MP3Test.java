package net.gnehzr.cct.speaking;

import org.testng.annotations.Test;

public class MP3Test {

    @Test(enabled = true)
    public void testPlay() throws Exception {
        for(int ch = 0; ch < 20; ch++) {
            MP3 mp3 = new MP3(ch + ".mp3");
            mp3.play();
        }
    }
}