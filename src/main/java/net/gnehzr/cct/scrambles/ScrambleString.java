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
    private ScrambleVariation variation;
    private final ScramblePlugin scramblePluginPlugin;
    private String textComments;

    public ScrambleString(String scramble, boolean imported, ScrambleVariation variation, ScramblePlugin scramblePluginPlugin, String textComments) {
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

	public ScrambleVariation getVariation() {
        return variation;
    }

	public String toString() {
        return scramble;
    }

    public String getTextComments() {
        return textComments;
    }
}
