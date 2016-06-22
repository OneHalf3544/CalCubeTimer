package scramblePlugins.cube3x3crosssolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static scramblePlugins.cube3x3crosssolver.Face.*;

/**
 * <p>
 * <p>
 * Created: 07.11.2015 14:05
 * <p>
 *
 * @author OneHalf
 */
class CubeCrossOutputter {

    private final CubeWithRandomCrossState cube;

    CubeCrossOutputter(CubeWithRandomCrossState cube) {
        this.cube = cube;
    }

    public String toTextPresentation() {
        Map<Face, RubicsColor[][]> c = initializeArray();

        for (int i = 0; i < 12; i++) {
            if (cube.edgesPosition.get(i) == null) {
                continue;
            }

            Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> position
                    = getPositionByIndex(cube.edgesPosition.get(i));

            if (cube.edgesOrientations.get(i)) {
                c.get(position.v1.v1)[position.v1.v2][position.v1.v3] = colorOn(position.v2.v1);
                c.get(position.v2.v1)[position.v2.v2][position.v2.v3] = colorOn(position.v1.v1);
            } else {
                c.get(position.v1.v1)[position.v1.v2][position.v1.v3] = colorOn(position.v1.v1);
                c.get(position.v2.v1)[position.v2.v2][position.v2.v3] = colorOn(position.v2.v1);
            }
        }

        return arrayToString(c);
    }

    Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> getPositionByIndex(int positionIndex) {
        checkArgument(positionIndex >= 0 && positionIndex < 12);
        Face firstFace = null;
        Face secondFace;
        for (Map.Entry<Face, int[]> faceIndex : CubeWithRandomCrossState.FACE_INDEXES.entrySet()) {
            if (Arrays.stream(faceIndex.getValue()).anyMatch(i -> i == positionIndex)) {
                if (firstFace == null) {
                    firstFace = cube.getCurrentOrientation().invert().mapTurnFaceToUnrotatedCubeFace(faceIndex.getKey()).getFace();
                } else {
                    secondFace = cube.getCurrentOrientation().invert().mapTurnFaceToUnrotatedCubeFace(faceIndex.getKey()).getFace();
                    return getPositionsBySiblingFaces(firstFace, secondFace);
                }
            }
        }
        throw new IllegalStateException();
    }

    @NotNull
    private Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> getPositionsBySiblingFaces(Face firstFace, Face secondFace) {
        Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> result = getOptionalPositionsBySiblingFaces(firstFace, secondFace);
        if (result == null) {
            result = getOptionalPositionsBySiblingFaces(secondFace, firstFace);
        }
        if (result == null) {
            throw new IllegalArgumentException("no common elements between " + firstFace + " and " + secondFace);
        }
        return result;
    }

    @Nullable
    private Tuple2<Tuple3<Face, Integer, Integer>, Tuple3<Face, Integer, Integer>> getOptionalPositionsBySiblingFaces(Face firstFace, Face secondFace) {
        switch (firstFace) {
            case UP:
                switch (secondFace) {
                    case FRONT:
                        return Tuple.tuple(
                                Tuple.tuple(UP, 2, 1),
                                Tuple.tuple(FRONT, 0, 1));
                    case LEFT:
                        return Tuple.tuple(
                                Tuple.tuple(UP, 1, 0),
                                Tuple.tuple(LEFT, 0, 1));
                    case RIGHT:
                        return Tuple.tuple(
                                Tuple.tuple(UP, 1, 2),
                                Tuple.tuple(RIGHT, 0, 1));
                    case BACK:
                        return Tuple.tuple(
                                Tuple.tuple(UP, 0, 1),
                                Tuple.tuple(BACK, 0, 1));

                }
                break;

            case FRONT:
                switch (secondFace) {
                    case LEFT:
                        return Tuple.tuple(
                                Tuple.tuple(FRONT, 1, 0),
                                Tuple.tuple(LEFT, 1, 2));
                    case RIGHT:
                        return Tuple.tuple(
                                Tuple.tuple(FRONT, 1, 2),
                                Tuple.tuple(RIGHT, 1, 0));
                    case DOWN:
                        return Tuple.tuple(
                                Tuple.tuple(FRONT, 2, 1),
                                Tuple.tuple(DOWN, 0, 1));
                }
                break;

            case LEFT:
                switch (secondFace) {
                    case BACK:
                        return Tuple.tuple(
                                Tuple.tuple(LEFT, 1, 0),
                                Tuple.tuple(BACK, 1, 2));
                    case DOWN:
                        return Tuple.tuple(
                                Tuple.tuple(LEFT, 2, 1),
                                Tuple.tuple(DOWN, 1, 0));
                }
                break;

            case RIGHT:
                switch (secondFace) {
                    case DOWN:
                        return Tuple.tuple(
                                Tuple.tuple(RIGHT, 2, 1),
                                Tuple.tuple(DOWN, 1, 2));
                    case BACK:
                        return Tuple.tuple(
                                Tuple.tuple(RIGHT, 1, 2),
                                Tuple.tuple(BACK, 1, 0));
                }
                break;

            case DOWN:
                switch (secondFace) {
                    case BACK:
                        return Tuple.tuple(
                                Tuple.tuple(DOWN, 2, 1),
                                Tuple.tuple(BACK, 2, 1));
                }
                break;
        }

        return null;
    }

    @NotNull
    private String arrayToString(Map<Face, RubicsColor[][]> c) {
        String result = "";
        result += "    " + line(c.get(UP)[0]) + "\n";
        result += "    " + line(c.get(UP)[1]) + "\n";
        result += "    " + line(c.get(UP)[2]) + "\n";
        result += line(c.get(LEFT)[0]) + " " + line(c.get(FRONT)[0]) + " " + line(c.get(RIGHT)[0]) + " " + line(c.get(BACK)[0]) + "\n";
        result += line(c.get(LEFT)[1]) + " " + line(c.get(FRONT)[1]) + " " + line(c.get(RIGHT)[1]) + " " + line(c.get(BACK)[1]) + "\n";
        result += line(c.get(LEFT)[2]) + " " + line(c.get(FRONT)[2]) + " " + line(c.get(RIGHT)[2]) + " " + line(c.get(BACK)[2]) + "\n";
        result += "    " + line(c.get(DOWN)[0]) + "\n";
        result += "    " + line(c.get(DOWN)[1]) + "\n";
        result += "    " + line(c.get(DOWN)[2]) + "\n";
        return result;
    }

    @NotNull
    private Map<Face, RubicsColor[][]> initializeArray() {
        Map<Face, RubicsColor[][]> c = new HashMap<>();
        c.put(UP, new RubicsColor[][] {
                new RubicsColor[] {null, null,        null},
                new RubicsColor[] {null, colorOn(UP), null},
                new RubicsColor[] {null, null,        null},
        });
        c.put(DOWN, new RubicsColor[][] {
                new RubicsColor[] {null, null,          null},
                new RubicsColor[] {null, colorOn(DOWN), null},
                new RubicsColor[] {null, null,          null},
        });
        c.put(LEFT, new RubicsColor[][] {
                new RubicsColor[] {null, null,          null},
                new RubicsColor[] {null, colorOn(LEFT), null},
                new RubicsColor[] {null, null,          null},
        });
        c.put(RIGHT, new RubicsColor[][] {
                new RubicsColor[] {null, null,           null},
                new RubicsColor[] {null, colorOn(RIGHT), null},
                new RubicsColor[] {null, null,           null},
        });
        c.put(FRONT, new RubicsColor[][] {
                new RubicsColor[] {null, null,           null},
                new RubicsColor[] {null, colorOn(FRONT), null},
                new RubicsColor[] {null, null,           null},
        });
        c.put(BACK, new RubicsColor[][] {
                new RubicsColor[] {null, null,          null},
                new RubicsColor[] {null, colorOn(BACK), null},
                new RubicsColor[] {null, null,          null},
        });
        return c;
    }

    private String line(RubicsColor[] rubicsColors) {
        return colorToString(rubicsColors[0]).toString()
                + colorToString(rubicsColors[1]).toString()
                + colorToString(rubicsColors[2]).toString();
    }

    private Character colorToString(RubicsColor rubicsColor) {
        return rubicsColor == null ? '.' : rubicsColor.name().charAt(0);
    }

    private RubicsColor colorOn(Face face) {
        return cube.getCurrentOrientation().mapTurnFaceToUnrotatedCubeFace(face);
    }
}
