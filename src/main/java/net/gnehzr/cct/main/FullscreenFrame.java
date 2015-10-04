package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * <p>
 * <p>
 * Created: 31.03.2015 11:49
 * <p>
 *
 * @author OneHalf
 */
public class FullscreenFrame extends JFrame {

    private final Configuration configuration;

    @Inject
    public FullscreenFrame(Configuration configuration, @Named("bigTimersDisplay") TimerLabel bigTimersDisplay,
                           ActionMap.ToggleFullscreenTimingAction toggleFullscreenTimingAction) {
        super(getGraphicsConfiguration(configuration));
        this.configuration = configuration;
        this.add(createFullscreenPanel(toggleFullscreenTimingAction, bigTimersDisplay));
    }

    private static GraphicsConfiguration getGraphicsConfiguration(Configuration configuration) {
        int index = configuration.isPropertiesLoaded() ? configuration.getInt(VariableKey.FULLSCREEN_DESKTOP) : 0;
        GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        return screenDevices[index].getDefaultConfiguration();
    }

    void resizeFrame() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsDevice gd = gs[configuration.getInt(VariableKey.FULLSCREEN_DESKTOP)];
        DisplayMode screenSize = gd.getDisplayMode();
        setSize(screenSize.getWidth(), screenSize.getHeight());
        validate();
    }

    private JLayeredPane createFullscreenPanel(ActionMap.ToggleFullscreenTimingAction toggleFullscreenTimingAction,
                                               TimerLabel bigTimersDisplay) {
        JLayeredPane fullscreenPanel = new JLayeredPane();
        // todo add property modification on settings change
        fullscreenPanel.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);

        final JButton fullScreenButton = new JButton(toggleFullscreenTimingAction);

        fullscreenPanel.add(bigTimersDisplay, Integer.valueOf(0));
        fullscreenPanel.add(fullScreenButton, Integer.valueOf(1));

        fullscreenPanel.addComponentListener(new ComponentAdapter() {
            private static final int LENGTH = 30;
            @Override
            public void componentResized(ComponentEvent e) {
                bigTimersDisplay.setBounds(0, 0, e.getComponent().getWidth(), e.getComponent().getHeight());
                fullScreenButton.setBounds(e.getComponent().getWidth() - LENGTH, 0, LENGTH, LENGTH);
            }
        });
        return fullscreenPanel;
    }

}
