package scramblePlugins.cube3x3crosssolver;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * <p>
 * <p>
 * Created: 15.11.2015 20:50
 * <p>
 *
 * @author OneHalf
 */
public enum Direction {

    CLOCKWISE("", 1) {
        @Override
        public Direction invert() {
            return COUNTER_CLOCKWISE;
        }
    },
    COUNTER_CLOCKWISE("'", 3) {
        @Override
        public Direction invert() {
            return CLOCKWISE;
        }
    },
    HALF_TURN("2", 2) {
        @Override
        public Direction invert() {
            return HALF_TURN;
        }
    };

    private final String stringCode;
    private final int clockwiseTurnCount;

    Direction(String stringCode, int clockwiseTurnCount) {
        this.stringCode = stringCode;
        this.clockwiseTurnCount = clockwiseTurnCount;
    }

    public String getStringCode() {
        return stringCode;
    }

    public int getClockwiseTurnCount() {
        return clockwiseTurnCount;
    }

    /**
     * index - count of clockwise turns
     */
    private static final Map<String, Direction> DIRECTIONS;
    static {
        ImmutableMap.Builder<String, Direction> builder = ImmutableMap.builder();
        for (Direction direction : Direction.values()) {
            builder.put(direction.getStringCode(), direction);
        }
        DIRECTIONS = builder.build();
    }

    public static Direction byStringCode(String stringCode) {
        return DIRECTIONS.get(stringCode);
    }

    public abstract Direction invert();
}
