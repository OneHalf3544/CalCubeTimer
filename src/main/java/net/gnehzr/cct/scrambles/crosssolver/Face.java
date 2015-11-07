package net.gnehzr.cct.scrambles.crosssolver;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 13:44
 * <p>
 *
 * @author OneHalf
 */
public enum Face {
    FRONT('F'), UP('U'), RIGHT('R'), BACK('B'), LEFT('L'), DOWN('D');

    private final char faceChar;

    Face(char faceChar) {
        this.faceChar = faceChar;
    }

    @Override
    public String toString() {
        return Character.toString(faceChar);
    }

    public char getFaceChar() {
        return faceChar;
    }

    public Face getOpposite() {
        switch (this) {
            case FRONT:
                return BACK;
            case BACK:
                return FRONT;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            default:
                return null;
        }
    }
}
