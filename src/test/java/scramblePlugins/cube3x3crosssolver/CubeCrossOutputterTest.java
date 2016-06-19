package scramblePlugins.cube3x3crosssolver;

import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CubeCrossOutputterTest {
    @Test
    public void testToTextPresentation() throws Exception {
        String textPresentation = new CubeCrossOutputter(CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE)).toTextPresentation();
        assertEquals(textPresentation,
                "" +
                        "    .W.\n" +
                        "    WWW\n" +
                        "    .W.\n" +
                        ".O. .G. .R. .B.\n" +
                        ".O. .G. .R. .B.\n" +
                        "... ... ... ...\n" +
                        "    ...\n" +
                        "    .Y.\n" +
                        "    ...\n"
        );
    }

    @Test
    public void should_haveCorrectTextPresentation_when_cubeRotated() throws Exception {
        CubeWithRandomCrossState cube = CubeWithSolvedCross
                .getSolvedState(RubicsColor.WHITE)
                .applyRotate(Rotate.x.invert());

        assertEquals(new CubeCrossOutputter(cube).toTextPresentation(),
                "" +
                        "    ...\n" +
                        "    .B.\n" +
                        "    .B.\n" +
                        "... .W. ... ...\n" +
                        ".OO WWW RR. .Y.\n" +
                        "... .W. ... ...\n" +
                        "    .G.\n" +
                        "    .G.\n" +
                        "    ...\n"
        );
    }

    @Test
    public void should_haveCorrectTextPresentation_when_crossOnFrontSide() throws Exception {
        CubeWithRandomCrossState cube = CubeWithSolvedCross
                .getSolvedState(RubicsColor.GREEN);

        assertEquals(new CubeCrossOutputter(cube).toTextPresentation(),
                "" +
                        "    ...\n" +
                        "    .W.\n" +
                        "    .W.\n" +
                        "... .G. ... ...\n" +
                        ".OO GGG RR. .B.\n" +
                        "... .G. ... ...\n" +
                        "    .Y.\n" +
                        "    .Y.\n" +
                        "    ...\n"
        );
    }

    @Test
    public void should_findUFPosition_when_indexIs0() throws Exception {
        CubeWithRandomCrossState cube = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE);

        Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> position = new CubeCrossOutputter(cube).getPositionByIndex(0);

        assertEquals(position.v1.v1, Face.UP);
        assertEquals(position.v1.v2, Integer.valueOf(2));
        assertEquals(position.v1.v3, Integer.valueOf(1));

        assertEquals(position.v2.v1, Face.FRONT);
        assertEquals(position.v2.v2, Integer.valueOf(0));
        assertEquals(position.v2.v3, Integer.valueOf(1));

    }

    @Test
    public void should_findFDPosition_when_indexIs0AndRotatedWithX() throws Exception {
        CubeWithRandomCrossState cube = CubeWithSolvedCross.getSolvedState(RubicsColor.WHITE).applyRotate(Rotate.x.invert());

        Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> position = new CubeCrossOutputter(cube).getPositionByIndex(0);

        assertEquals(position.v1.v1, Face.FRONT);
        assertEquals(position.v1.v2, Integer.valueOf(2));
        assertEquals(position.v1.v3, Integer.valueOf(1));

        assertEquals(position.v2.v1, Face.DOWN);
        assertEquals(position.v2.v2, Integer.valueOf(0));
        assertEquals(position.v2.v3, Integer.valueOf(1));
    }


}