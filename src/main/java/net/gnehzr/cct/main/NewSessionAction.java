package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
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
@Service
public class NewSessionAction extends AbstractAction {

    private final CALCubeTimerFrame calCubeTimerFrame;

    @Autowired
    private SessionsList sessionsList;

    @Autowired
    public NewSessionAction(CALCubeTimerFrame calCubeTimerFrame) {
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    @Autowired
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
