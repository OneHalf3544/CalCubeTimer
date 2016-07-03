package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CALCubeTimerFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.statistics.SessionsList;

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
public class NewSessionAction extends AbstractNamedAction {

    private final CALCubeTimerFrame calCubeTimerFrame;

    @Autowired
    private SessionsList sessionsList;

    @Autowired
    public NewSessionAction(CALCubeTimerFrame calCubeTimerFrame) {
        super("newsession");
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        sessionsList.createSession(sessionsList.getCurrentSession().getPuzzleType());
        calCubeTimerFrame.getTimeLabel().reset();
        calCubeTimerFrame.updateScramble();
    }
}
