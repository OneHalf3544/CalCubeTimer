package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.Session;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;

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
    private final CalCubeTimerModel calCubeTimerModel;

    @Inject
    private CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;
    @Inject
    private ScrambleList scramblesList;
    @Inject
    private Configuration configuration;

    @Inject
    public NewSessionAction(CALCubeTimerFrame calCubeTimerFrame, CalCubeTimerModel calCubeTimerModel) {
        this.calCubeTimerFrame = calCubeTimerFrame;
        this.calCubeTimerModel = calCubeTimerModel;
    }

    @Inject
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction(ActionMap.NEWSESSION_ACTION, this);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        //only create a new session if we've added any times to the current one
        if (currentSessionSolutionsTableModel.getRowCount() > 0) {
            currentSessionSolutionsTableModel.setCurrentSession(calCubeTimerModel.getSelectedProfile(),
                    new Session(LocalDateTime.now(), configuration, scramblesList.getCurrentScrambleCustomization()));
            calCubeTimerFrame.getTimeLabel().reset();
            scramblesList.clear();
            calCubeTimerFrame.updateScramble();
        }
    }
}
