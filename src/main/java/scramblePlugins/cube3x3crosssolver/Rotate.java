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
public class Rotate implements SolveStep {

    public static final Rotate identity = new Rotate("", null) {
        @Override
        public Rotate invert() {
            return this;
        }

        @Override
        public String toString() {
            return "identity";
        }

        @Override
        public Rotate plus(Rotate anotherRotate) {
            return anotherRotate;
        }

        @Override
        public Rotate doubleRotate() {
            return this;
        }
    };
    public static final Rotate x = new Rotate("x", Direction.CLOCKWISE, Face.FRONT, Face.UP, Face.BACK, Face.DOWN);
    public static final Rotate y = new Rotate("y", Direction.CLOCKWISE, Face.FRONT, Face.LEFT, Face.BACK, Face.RIGHT);
    public static final Rotate z = new Rotate("z", Direction.CLOCKWISE, Face.UP, Face.RIGHT, Face.DOWN, Face.LEFT);

    private final Direction direction;

    /**
     * Colors of the cube sides after rotation from default scramble position:
     */
    private Map<Face, RubicsColor> rotatedFaceToFixedCubeFace = new HashMap<>();

    private final String rotateNotation;

    private Rotate(@NotNull String desc, Direction direction) {
        this.rotateNotation = desc;
        this.direction = direction;
        for (RubicsColor color : RubicsColor.values()) {
            rotatedFaceToFixedCubeFace.put(color.getFace(), color);
        }
    }

    private Rotate(String desc, Direction direction, Face a, Face b, Face c, Face d) {
        this(desc, direction);
        rotatedFaceToFixedCubeFace.put(b, RubicsColor.defaultByFace(a));
        rotatedFaceToFixedCubeFace.put(c, RubicsColor.defaultByFace(b));
        rotatedFaceToFixedCubeFace.put(d, RubicsColor.defaultByFace(c));
        rotatedFaceToFixedCubeFace.put(a, RubicsColor.defaultByFace(d));
    }

    public Rotate invert() {
        Objects.requireNonNull(direction, "cannot inverse rotation when rotate direction is not defined");
        if (direction == Direction.HALF_TURN) {
            return this;
        }
        Rotate newRotate = new Rotate(rotateNotation, direction.invert());
        for (Face face : rotatedFaceToFixedCubeFace.keySet()) {
            newRotate.rotatedFaceToFixedCubeFace.put(
                    mapTurnFaceToUnrotatedCubeFace(face).getFace(), RubicsColor.defaultByFace(face));
        }
        return newRotate;
    }

    public Rotate doubleRotate() {
        checkArgument(direction == Direction.CLOCKWISE);
        return plus(this, this.getNotation());
    }

    public Rotate plus(Rotate anotherRotate) {
        return plus(anotherRotate, this.getNotation() + " " + anotherRotate.getNotation());
    }

    private Rotate plus(Rotate anotherRotate, String desc) {
        Direction complexDirection = (this == anotherRotate && this.direction == Direction.CLOCKWISE) ? Direction.HALF_TURN : null;
        Rotate newRotate = new Rotate(desc, complexDirection);
        for (Face newFace : this.rotatedFaceToFixedCubeFace.keySet()) {
            newRotate.rotatedFaceToFixedCubeFace.put(newFace, this.mapTurnFaceToUnrotatedCubeFace(anotherRotate.mapTurnFaceToUnrotatedCubeFace(newFace).getFace()));
        }
        return newRotate;
    }

    /**
     * transform current face of random oriented cube to face,
     * @param newFace
     * @return
     */
    public RubicsColor mapTurnFaceToUnrotatedCubeFace(Face newFace) {
        return rotatedFaceToFixedCubeFace.get(newFace);
    }

    public Turn getOGTurn(@NotNull Turn turn) {
        return new Turn(mapTurnFaceToUnrotatedCubeFace(turn.face).getFace(), turn.direction);
    }

    public String toString() {
        return String.format("%s: %s", getDesc(), rotatedFaceToFixedCubeFace.toString());
    }

    @Override
    public String getNotation() {
        return rotateNotation + (direction == null ? "" : direction.getStringCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rotate rotate = (Rotate) o;
        return Objects.equals(rotatedFaceToFixedCubeFace, rotate.rotatedFaceToFixedCubeFace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rotatedFaceToFixedCubeFace);
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
            case CLOCKWISE:
                return rotateNotation;
            case HALF_TURN:
                return rotateNotation + "2";
            case COUNTER_CLOCKWISE:
                return rotateNotation + "'";
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
