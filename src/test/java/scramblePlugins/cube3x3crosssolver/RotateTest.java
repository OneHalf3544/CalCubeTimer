package scramblePlugins.cube3x3crosssolver;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static scramblePlugins.cube3x3crosssolver.Direction.*;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 23:28
 * <p>
 *
 * @author OneHalf
 */
public class RotateTest {

    @Test
    public void testInvert() throws Exception {
        assertEquals(Rotate.x.invert().invert().invert().invert(), Rotate.x);
        assertEquals(Rotate.x.doubleRotate().invert(), Rotate.x.doubleRotate());
        assertEquals(Rotate.x.getDesc(), "x");
        assertEquals(Rotate.x.invert().getDesc(), "x'");
        // IAE: assertEquals(Rotate.x.invert().doubleRotate().getDesc(), "x2");
        assertEquals(Rotate.identity.doubleRotate().invert().getDesc(), "");
    }

    @Test
    public void testDoubleRotate() throws Exception {
        // IAE: assertEquals(Rotate.x.invert().doubleRotate().getDesc(), "x2");
        assertNotEquals(Rotate.x.doubleRotate(), Rotate.x);
        assertEquals(Rotate.x.doubleRotate(), Rotate.x.doubleRotate());
        assertEquals(Rotate.x.doubleRotate().getDesc(), "x2");
        // IAE: assertEquals(Rotate.x.doubleRotate().doubleRotate().getDesc(), "x2");
    }

    @Test
    public void testGetOGTurn() throws Exception {
        assertEquals(Rotate.x.getOGTurn(new Turn(Face.RIGHT, COUNTER_CLOCKWISE)), new Turn(Face.RIGHT, COUNTER_CLOCKWISE));
        assertEquals(Rotate.y.getOGTurn(new Turn(Face.RIGHT, COUNTER_CLOCKWISE)), new Turn(Face.BACK, COUNTER_CLOCKWISE));
    }

    @Test
    public void testWithDirection() throws Exception {
        assertEquals(Rotate.identity.withDirection(HALF_TURN).getDesc(), "");
        assertEquals(Rotate.x.withDirection(CLOCKWISE).getDesc(), "x");
        assertEquals(Rotate.x.withDirection(HALF_TURN).getDesc(), "x2");
        assertEquals(Rotate.x.withDirection(COUNTER_CLOCKWISE).getDesc(), "x'");
    }
}