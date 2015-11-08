package scramblePlugins.cube3x3crosssolver;

import org.testng.annotations.Test;
import scramblePlugins.cube3x3crosssolver.Face;
import scramblePlugins.cube3x3crosssolver.Rotate;
import scramblePlugins.cube3x3crosssolver.Turn;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

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
        assertEquals(Rotate.x.getOGTurn(new Turn(Face.RIGHT, 3)), new Turn(Face.RIGHT, 3));
        assertEquals(Rotate.y.getOGTurn(new Turn(Face.RIGHT, 3)), new Turn(Face.BACK, 3));
    }

    @Test
    public void testWithDirection() throws Exception {
        assertEquals(Rotate.identity.withDirection(2).getDesc(), "");
        assertEquals(Rotate.x.withDirection(1).getDesc(), "x");
        assertEquals(Rotate.x.withDirection(2).getDesc(), "x2");
        assertEquals(Rotate.x.withDirection(3).getDesc(), "x'");
    }
}