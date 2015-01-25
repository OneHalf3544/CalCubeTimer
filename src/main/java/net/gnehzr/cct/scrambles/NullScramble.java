package net.gnehzr.cct.scrambles;

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
class NullScramble extends Scramble {
    @Override
    public Scramble importScramble(String variation, String scramble, String generatorGroup, List<String> attributes) throws InvalidScrambleException {
        return NULL_SCRAMBLE;
    }

    @Override
    protected Scramble createScramble(String variation, int length, String generatorGroup, List<String> attributes) {
        return NULL_SCRAMBLE;
    }

    NullScramble(String scramble) {
        super("NULL_SCRAMBLE", false, scramble, false);
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
