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

    private final ScramblePlugin scramblePluginPlugin;
    private final PuzzleType puzzleType;
    private String scramble;
    private boolean imported;
    private ScrambleSettings variation;
    private String textComments;

    public ScrambleString(PuzzleType puzzleType, String scramble, boolean imported, ScrambleSettings variation, ScramblePlugin scramblePluginPlugin, String textComments) {
        this.puzzleType = puzzleType;
        this.scramble = scramble;
        this.imported = imported;
        this.variation = variation;
        this.scramblePluginPlugin = scramblePluginPlugin;
        this.textComments = textComments;
    }

    public ScramblePlugin getScramblePlugin() {
        return scramblePluginPlugin;
    }

    public String getScramble() {
        return scramble;
    }

    public boolean isImported() {
        return imported;
    }

	public PuzzleType getPuzzleType() {
        return puzzleType;
    }

	public ScrambleSettings getVariation() {
        return variation;
    }

	public String toString() {
        return scramble;
    }

    public String getTextComments() {
        return textComments;
    }
}
