package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 29.10.2015 22:44
 * <p>
 *
 * @author OneHalf
 */
public class DefaultPuzzleViewComponent extends PuzzleViewComponent {

    private final PuzzleType puzzleType;

    private String focusedFaceId = null;

    public DefaultPuzzleViewComponent(PuzzleType puzzleType, Configuration configuration,
                                      ScramblePluginManager scramblePluginManager) {
        super(configuration, scramblePluginManager);
        this.puzzleType = puzzleType;
        addMouseListener(createMouseListener());
        addMouseMotionListener(createMouseMotionListener());
    }


    public void commitColorSchemeToConfiguration() {
        for(Map.Entry<String, Color> face : colorScheme.get(getPuzzleType()).entrySet()) {
            configuration.setColor(VariableKey.PUZZLE_COLOR(puzzleType.getScramblePlugin(), face.getKey()), face.getValue());
        }
    }

    public void setDefaultPuzzleView(PuzzleType puzzleType) {
        faceShapes = puzzleType.getScramblePlugin().getFaces(GAP, getUnitSize(false, puzzleType), puzzleType.getVariationName());
        buffer = scramblePluginManager.getDefaultStateImage(puzzleType, GAP, getUnitSize(false, puzzleType), colorScheme.get(getPuzzleType()));

        repaint();	//this will cause the scramble to be drawn
        invalidate(); //this forces the component to fit itself to its layout properly
    }

    private MouseMotionAdapter createMouseMotionListener() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                findFocusedFace(e.getPoint());
            }
        };
    }

    private MouseAdapter createMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(focusedFaceId != null) {
                    Color color = JColorChooser.showDialog(DefaultPuzzleViewComponent.this,
                            StringAccessor.getString("ScrambleViewComponent.choosecolor") + ": " + focusedFaceId,
                            Objects.requireNonNull(colorScheme.get(getPuzzleType()).get(focusedFaceId)));
                    if(color != null) {
                        colorScheme.get(getPuzzleType()).put(focusedFaceId, color);
                        setDefaultPuzzleView(getPuzzleType());
                    }
                    findFocusedFace(getMousePosition());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                findFocusedFace(null);
            }

        };
    }


    private void findFocusedFace(Point p) {
        focusedFaceId = null;
        if (p == null) {
            repaint();
            return;
        }
        focusedFaceId = faceShapes.entrySet().stream()
                .filter(shape -> shape.getValue() != null && shape.getValue().contains(p))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        repaint();
    }

    @Override
    protected PuzzleType getPuzzleType() {
        return puzzleType;
    }

    @Override
    protected int getUnitSize(boolean defaults, PuzzleType puzzleType) {
        return puzzleType.getScramblePlugin().getDefaultUnitSize();
    }

    @Override
    protected void paintFace(Graphics g) {
        if (focusedFaceId == null) {
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
            ((Graphics2D)g).setComposite(ac);
            //first, draw the whole scramble opaque
            g.drawImage(buffer, 0, 0, null);

            //now prepare the surface for drawing the selected face in solid
            g.setClip(faceShapes.get(focusedFaceId));
            ac = ac.derive(1.0f);
            ((Graphics2D)g).setComposite(ac);
        }
        //if no face is selected, we draw the whole thing solid, otherwise, just the selected face
        g.drawImage(buffer, 0, 0, null);
    }
}
