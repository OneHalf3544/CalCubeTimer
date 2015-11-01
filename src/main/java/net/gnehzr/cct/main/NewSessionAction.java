package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
* <p>
* <p>
* Created: 22.01.2015 0:38
* <p>
*
* @author OneHalf
*/
@Singleton
public class NewSessionAction extends AbstractAction {

    private final CALCubeTimerFrame calCubeTimerFrame;

    @Inject
    private SessionsList sessionsList;

    @Inject
    public NewSessionAction(CALCubeTimerFrame calCubeTimerFrame) {
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    @Inject
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction(ActionMap.NEWSESSION_ACTION, this);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        sessionsList.createSession(sessionsList.getCurrentSession().getPuzzleType());
        calCubeTimerFrame.getTimeLabel().reset();
        calCubeTimerFrame.updateScramble();
    }
}
