package scramblePlugins.cube3x3crosssolver;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 *
 * <p>
 * <p>
 * Created: 07.11.2015 13:44
 * <p>
 *
 * @author OneHalf
 */
public enum RubicsColor {
    WHITE(Face.UP),
    GREEN(Face.FRONT),
    RED(Face.RIGHT),
    BLUE(Face.BACK),
    ORANGE(Face.LEFT),
    YELLOW(Face.DOWN);

    private final Face face;

    RubicsColor(Face face) {
        this.face = face;
    }

    public Face getFace() {
        return face;
    }

    public static RubicsColor defaultByFace(@NotNull Face face) {
        return Arrays.stream(values())
                .filter(rubicsColor -> rubicsColor.getFace() == face)
                .findAny()
                .get();
    }
}
