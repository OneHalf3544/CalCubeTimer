package net.gnehzr.cct.scrambles;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
* <p>
* <p>
* Created: 10.01.2015 0:36
* <p>
*
* @author OneHalf
*/
class NullScramble extends Scramble {
    @Override
    public Scramble importScramble(String variation, String scramble, String generatorGroup, List<String> attributes) throws InvalidScrambleException {
        return NULL_SCRAMBLE;
    }

    @Override
    protected Scramble createScramble(String variation, int length, String generatorGroup, List<String> attributes) {
        return NULL_SCRAMBLE;
    }

    @Override
    public boolean supportsScrambleImage() {
        return false;
    }

    NullScramble(String scramble) {
        super("NULL_SCRAMBLE", scramble);
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
    public Shape[] getFaces(int gap, int pieceSize, String variation) {
        return new Shape[0];
    }

    @Override
    public String htmlify(String formatMe) {
        return formatMe;
    }

    @Override
    protected String[][] getFaceNamesColors() {
        return new String[0][];
    }

    @Override
    public int getDefaultUnitSize() {
        return -1;
    }

    @NotNull
    @Override
    public String[] getVariations() {
        return new String[0];
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

    @Override
    public String[] getDefaultGenerators() {
        return null;
    }
}
