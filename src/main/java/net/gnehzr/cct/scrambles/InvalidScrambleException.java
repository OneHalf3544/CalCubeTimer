package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.i18n.StringAccessor;

/**
* <p>
* <p>
* Created: 01.02.2015 15:48
* <p>
*
* @author OneHalf
*/
public class InvalidScrambleException extends Exception {
    public InvalidScrambleException(String scramble) {
        super(StringAccessor.getString("InvalidScrambleException.invalidscramble") + "\n" + scramble);
    }
}
