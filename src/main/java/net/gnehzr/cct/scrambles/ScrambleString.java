package net.gnehzr.cct.scrambles;

/**
* <p>
* <p>
* Created: 17.01.2015 12:42
* <p>
*
* @author OneHalf
*/
public class ScrambleString {

    private String scramble;
    private boolean imported;
    private int length;

	public ScrambleString(String scramble, boolean imported, int length) {
        this.scramble = scramble;
        this.imported = imported;
        this.length = length;
    }

	public String getScramble() {
        return scramble;
    }

    public boolean isImported() {
        return imported;
    }

	public int getLength() {
        return length;
    }

	public String toString() {
        return scramble;
    }
}
