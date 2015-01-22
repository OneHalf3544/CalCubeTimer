package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.statistics.ProfileDao;
import net.gnehzr.cct.statistics.StatisticsTableModel;

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
    private StatisticsTableModel statsModel;
    @Inject
    private ProfileDao profileDao;
    @Inject
    private ScrambleList scramblesList;

    @Inject
    public NewSessionAction(CALCubeTimerFrame calCubeTimerFrame) {
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    @Inject
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction(ActionMap.NEWSESSION_ACTION, this);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (statsModel.getRowCount() > 0) { //only create a new session if we've added any times to the current one
            statsModel.setSession(calCubeTimerFrame.createNewSession(profileDao.getSelectedProfile(), scramblesList.getScrambleCustomization().toString()));
            calCubeTimerFrame.getTimeLabel().reset();
            scramblesList.clear();
            calCubeTimerFrame.updateScramble();
        }
    }
}
