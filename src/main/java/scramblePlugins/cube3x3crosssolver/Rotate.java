package scramblePlugins.cube3x3crosssolver;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 13:47
 * <p>
 *
 * @author OneHalf
 */
public class Rotate {

    public static final Rotate identity = new Rotate("", null) {
        @Override
        public Rotate invert() {
            return this;
        }

        @Override
        public Rotate doubleRotate() {
            return this;
        }
    };
    public static final Rotate x = new Rotate("x", 1, Face.FRONT, Face.UP, Face.BACK, Face.DOWN);
    public static final Rotate y = new Rotate("y", 1, Face.FRONT, Face.LEFT, Face.BACK, Face.RIGHT);
    public static final Rotate z = new Rotate("z", 1, Face.UP, Face.RIGHT, Face.DOWN, Face.LEFT);
    private final Integer direction;

    private Map<Face, Face> new_og = new HashMap<>();

    private final String desc;

    private Rotate(@NotNull String desc, Integer direction) {
        this.desc = desc;
        this.direction = direction;
        for (Face f : Face.values()) {
            new_og.put(f, f);
        }
    }

    private Rotate(String desc, Integer direction, Face a, Face b, Face c, Face d) {
        this(desc, direction);
        new_og.put(b, a);
        new_og.put(c, b);
        new_og.put(d, c);
        new_og.put(a, d);
    }

    public Rotate invert() {
        Rotate newRotate = new Rotate(desc, (4 - direction) % 4);
        for (Face newFace : new_og.keySet()) {
            newRotate.new_og.put(getOGFace(newFace), newFace);
        }
        return newRotate;
    }

    public Rotate doubleRotate() {
        checkArgument(direction == 1);
        Rotate newRotate = new Rotate(this.desc, (direction + 1) % 4);
        for (Face newFace : this.new_og.keySet()) {
            newRotate.new_og.put(newFace, getOGFace(getOGFace(newFace)));
        }
        return newRotate;
    }

    public Face getOGFace(Face newFace) {
        return new_og.get(newFace);
    }

    public Turn getOGTurn(@NotNull Turn turn) {
        return new Turn(new_og.get(turn.face), turn.direction);
    }

    public String toString() {
        return String.format("%s: %s", getDesc(), new_og.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rotate rotate = (Rotate) o;
        return Objects.equals(new_og, rotate.new_og);
    }

    @Override
    public int hashCode() {
        return Objects.hash(new_og);
    }

    public Rotate withDirection(Direction direction) {
        switch (direction) {
            case CLOCKWISE:
                return this;
            case HALF_TURN:
                return this.doubleRotate();
            case COUNTER_CLOCKWISE:
                return this.invert();
            default:
                throw new IllegalArgumentException();
        }
    }

    public String getDesc() {
        if (direction == null) {
            return "";
        }
        switch (direction) {
            case 1:
                return desc;
            case 2:
                return desc + "2";
            case 3:
                return desc + "'";
            default:
                throw new IllegalStateException();
        }
    }

    public String getDescWithSpace() {
        String result = getDesc();
        if (result.isEmpty()) {
            return result;
        }
        return result + " ";
    }

}
