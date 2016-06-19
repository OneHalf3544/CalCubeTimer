package scramblePlugins.cube3x3crosssolver;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 14:01
 * <p>
 *
 * @author OneHalf
 */
public class Turn implements SolveStep {

    public final Face face;
    public final Direction direction;

    public Turn(@NotNull Face face, @NotNull Direction direction) {
        this.face = face;
        this.direction = direction;
    }

    @Override
    public String getNotation() {
        return face + direction.getStringCode();
    }

    @Override
    public String toString() {
        return getNotation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Turn turn = (Turn) o;
        return face == turn.face &&
                Objects.equals(direction, turn.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(face, direction);
    }
}
