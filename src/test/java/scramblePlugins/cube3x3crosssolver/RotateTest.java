package scramblePlugins.cube3x3crosssolver;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static scramblePlugins.cube3x3crosssolver.Direction.*;
import static scramblePlugins.cube3x3crosssolver.Rotate.x;
import static scramblePlugins.cube3x3crosssolver.Rotate.y;
import static scramblePlugins.cube3x3crosssolver.Rotate.z;

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
    public void should_beY_when_zxz () {
        assertEquals(Rotate.identity.plus(x), x);

        assertEquals(x.plus(x), x.doubleRotate());
        assertEquals(x.plus(x).plus(x), x.invert());

        Rotate complexRotate = z.invert().plus(x).plus(z);
        assertEquals(complexRotate, y.invert());
    }

    @Test
    public void testFaceTransformation() throws Exception {
        assertEquals(x.mapTurnFaceToUnrotatedCubeFace(Face.FRONT), RubicsColor.YELLOW);
        assertEquals(x.mapTurnFaceToUnrotatedCubeFace(Face.UP), RubicsColor.GREEN);

        assertEquals(y.mapTurnFaceToUnrotatedCubeFace(Face.FRONT), RubicsColor.RED);
        assertEquals(y.mapTurnFaceToUnrotatedCubeFace(Face.UP), RubicsColor.WHITE);
    }

    @Test
    public void testInvert() throws Exception {
        Rotate invertedX = x.invert();
        assertEquals(invertedX.mapTurnFaceToUnrotatedCubeFace(Face.UP), RubicsColor.BLUE);
        assertEquals(invertedX.mapTurnFaceToUnrotatedCubeFace(Face.FRONT), RubicsColor.WHITE);

        Rotate invertedZ = z.invert();
        assertEquals(invertedZ.mapTurnFaceToUnrotatedCubeFace(Face.UP), RubicsColor.RED);
        assertEquals(invertedZ.mapTurnFaceToUnrotatedCubeFace(Face.FRONT), RubicsColor.GREEN);
        assertEquals(invertedZ.mapTurnFaceToUnrotatedCubeFace(Face.LEFT), RubicsColor.WHITE);

        assertEquals(x.invert().invert().invert().invert(), x);
        assertEquals(x.doubleRotate().invert(), x.doubleRotate());
        assertEquals(x.getDesc(), "x");
        assertEquals(x.invert().getDesc(), "x'");
        // IAE: assertEquals(Rotate.x.invert().doubleRotate().getDesc(), "x2");
        assertEquals(Rotate.identity.doubleRotate().invert().getDesc(), "");
    }

    @Test
    public void testDoubleRotate() throws Exception {
        // IAE: assertEquals(Rotate.x.invert().doubleRotate().getDesc(), "x2");
        assertNotEquals(x.doubleRotate(), x);
        assertEquals(x.doubleRotate(), x.doubleRotate());
        assertEquals(x.doubleRotate().getDesc(), "x2");
        // IAE: assertEquals(Rotate.x.doubleRotate().doubleRotate().getDesc(), "x2");
    }

    @Test
    public void testGetOGTurn() throws Exception {
        assertEquals(x.getOGTurn(new Turn(Face.RIGHT, COUNTER_CLOCKWISE)), new Turn(Face.RIGHT, COUNTER_CLOCKWISE));
        assertEquals(Rotate.y.getOGTurn(new Turn(Face.RIGHT, COUNTER_CLOCKWISE)), new Turn(Face.BACK, COUNTER_CLOCKWISE));
    }

    @Test
    public void testWithDirection() throws Exception {
        assertEquals(Rotate.identity.withDirection(HALF_TURN).getDesc(), "");
        assertEquals(x.withDirection(CLOCKWISE).getDesc(), "x");
        assertEquals(x.withDirection(HALF_TURN).getDesc(), "x2");
        assertEquals(x.withDirection(COUNTER_CLOCKWISE).getDesc(), "x'");
    }

    @Test
    public void should_beYellowMove_when_rotate_z2Up() {
        assertEquals(z.doubleRotate().mapTurnFaceToUnrotatedCubeFace(Face.UP), RubicsColor.YELLOW);
    }

    @Test
    public void should_beWhiteMove_when_rotate_z$Left() {
        assertEquals(z.invert().plus(x).mapTurnFaceToUnrotatedCubeFace(Face.LEFT), RubicsColor.WHITE);
    }
}