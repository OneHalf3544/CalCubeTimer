package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Created: 29.10.2015 22:42
 * <p>
 *
 * @author OneHalf
 */
public abstract class PuzzleViewComponent extends JComponent {

    private static final int DEFAULT_GAP = 5;
    private static final Dimension PREFERRED_SIZE = new Dimension(0, 0);

    protected static Integer GAP = DEFAULT_GAP;
    protected final Configuration configuration;
    protected final ScramblePluginManager scramblePluginManager;

    protected BufferedImage buffer;

    // todo move to scramblePluginManager:
    protected Map<String, Color> colorScheme = null;
    protected Map<String, Shape> faceShapes = null;

    public PuzzleViewComponent(Configuration configuration, ScramblePluginManager scramblePluginManager) {
        this.scramblePluginManager = scramblePluginManager;
        configuration.addConfigurationChangeListener(createConfigurationListener(configuration));
        this.configuration = configuration;
    }

    public void syncColorScheme(boolean defaults) {
        colorScheme = scramblePluginManager.getColorScheme(getPuzzleType().getScramblePlugin(), defaults, configuration);
    }

    @Override
    public Dimension getPreferredSize() {
        if(buffer == null) {
            return PREFERRED_SIZE;
        }
        return new Dimension(buffer.getWidth(), buffer.getHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        if(buffer == null) {
            return PREFERRED_SIZE;
        }
        PuzzleType puzzleType = getPuzzleType();
        Dimension d = puzzleType.getScramblePlugin().getImageSize(GAP, getUnitSize(true, puzzleType), puzzleType.getVariationName());
        if(d != null) {
            return d;
        }
        return PREFERRED_SIZE;
    }

    protected abstract PuzzleType getPuzzleType();

    @Override
    public Dimension getMaximumSize() {
        return getMinimumSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, width, height);
        }

        if (buffer != null) {
            paintFace(g);
        }

        g.dispose();
    }

    protected abstract void paintFace(Graphics g);

    protected abstract int getUnitSize(boolean defaults, PuzzleType puzzleType);

    private ConfigurationChangeListener createConfigurationListener(final Configuration configuration) {
        return profile -> {
            GAP = configuration.getInt(VariableKey.POPUP_GAP);
            if(GAP == null) {
                GAP = DEFAULT_GAP;
            }
        };
    }
}
