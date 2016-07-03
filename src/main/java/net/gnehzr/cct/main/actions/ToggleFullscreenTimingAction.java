package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CALCubeTimerFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.event.ActionEvent;

/**
 * Date: 03.07.2016
 *
 * @author 123
 */
@Service
public class ToggleFullscreenTimingAction extends AbstractNamedAction {

    public static final String TOGGLE_FULLSCREEN = "togglefullscreen";

    private final CALCubeTimerFrame calCubeTimerFrame;

    @Autowired
    public ToggleFullscreenTimingAction(CALCubeTimerFrame calCubeTimerFrame) {
        super(ToggleFullscreenTimingAction.TOGGLE_FULLSCREEN, "+");
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        calCubeTimerFrame.setFullscreen(!calCubeTimerFrame.isFullscreen());
    }

}
