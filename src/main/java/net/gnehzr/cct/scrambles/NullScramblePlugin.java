package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
* <p>
* <p>
* Created: 10.01.2015 0:36
* <p>
*
* @author OneHalf
*/
class NullScramblePlugin extends ScramblePlugin {

    private static final Logger LOG = LogManager.getLogger(NullScramblePlugin.class);

    NullScramblePlugin() {
        super("NullScramblePlugin", false);
    }

    @Override
    public ScrambleString importScramble(PuzzleType puzzleType, ScrambleSettings.WithoutLength variation, String scramble,
                                         List<String> attributes) throws InvalidScrambleException {
        LOG.debug("import scramble");
        throw new UnsupportedOperationException("NullScramblePlugin.createScramble");
    }

    @Override
    public ScrambleString createScramble(PuzzleType puzzleType, ScrambleSettings variation, List<String> attributes) {
        LOG.debug("create scramble");
        throw new UnsupportedOperationException("NullScramblePlugin.createScramble");
    }

    @Override
    public int getNewUnitSize(int width, int height, int gap, String variation) {
        return -1;
    }

    @Override
    public Dimension getImageSize(int gap, int minxRad, String variation) {
        return null;
    }

    @Override
    public Map<String, Shape> getFaces(int gap, int pieceSize, String variation) {
        return Collections.emptyMap();
    }

    @Override
    public String htmlify(String formatMe) {
        return formatMe;
    }

    @NotNull
    @Override
    public Map<String, Color> getFaceNamesColors() {
        return Collections.emptyMap();
    }

    @Override
    public int getDefaultUnitSize() {
        return -1;
    }

    @NotNull
    @Override
    public ImmutableList<String> getVariations() {
        return ImmutableList.of();
    }

    @NotNull
    @Override
    protected int[] getDefaultLengths() {
        return new int[0];
    }

    @NotNull
    @Override
    public List<String> getAttributes() {
        return getDefaultAttributes();
    }

    @NotNull
    @Override
    public List<String> getDefaultAttributes() {
        return Collections.emptyList();
    }

    @Override
    public Pattern getTokenRegex() {
        return null;
    }

    @NotNull
    @Override
    public Map<String, String> getDefaultGenerators() {
        return Collections.emptyMap();
    }
}
