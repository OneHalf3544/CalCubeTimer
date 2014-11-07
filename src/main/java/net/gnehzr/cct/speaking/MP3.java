package net.gnehzr.cct.speaking;

/*************************************************************************
 *  Compilation:  javac -classpath .:jl1.0.jar MP3.java         (OS X)
 *                javac -classpath .;jl1.0.jar MP3.java         (Windows)
 *  Execution:    java -classpath .:jl1.0.jar MP3 filename.mp3  (OS X / Linux)
 *                java -classpath .;jl1.0.jar MP3 filename.mp3  (Windows)
 *  
 *  Plays an MP3 file using the JLayer MP3 library.
 *
 *  Reference:  http://www.javazoom.net/javalayer/sources.html
 *
 *
 *  To execute, get the file jl1.0.jar from the website above or from
 *
 *      http://www.cs.princeton.edu/introcs/24inout/jl1.0.jar
 *
 *  and put it in your working directory with this file MP3.java.
 *
 *************************************************************************/

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class MP3 {

	private static final Logger LOG = Logger.getLogger(MP3.class);

	private Player player;

    // constructor that takes the name of an MP3 file
    public MP3(InputStream fis) throws Exception {
    	if(fis == null) {
    		throw new Exception("File not found!");
    	}
    	try {
	        BufferedInputStream bis = new BufferedInputStream(fis);
		    player = new Player(bis);
	    } catch (Exception e) {
	    	throw new Exception("Problem playing file: " + e.getLocalizedMessage());
	    }
    }
    
    public MP3(String file) throws Exception {
//    	this(new FileInputStream(file));
    	this(MP3.class.getResourceAsStream(file));
    }

    public void close() { if (player != null) player.close(); }

    // play the MP3 file to the sound card
    public void play() throws JavaLayerException {
    	if(player == null)
    		throw new JavaLayerException();
    	player.play();
    }


    // test client
    public static void main(String[] args) {
		for(int ch = 0; ch < 20; ch++) {
			try {
				MP3 mp3 = new MP3(ch + ".mp3");
				mp3.play();
			} catch (Exception e) {
				LOG.info("unexpected exception", e);
			}
		}
    }
}
