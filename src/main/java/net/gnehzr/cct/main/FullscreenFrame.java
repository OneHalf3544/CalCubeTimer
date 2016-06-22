package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.keyboardTiming.TimerLabel;

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

    @Autowired
    public FullscreenFrame(Configuration configuration,
                           @Qualifier("bigTimersDisplay") TimerLabel bigTimersDisplay,
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
