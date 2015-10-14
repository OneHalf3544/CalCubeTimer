package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsListTableModel;

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
    private SessionsListTableModel sessionsListTableModel;

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
    public void actionPerformed(ActionEvent event) {
        Session session = new Session(LocalDateTime.now(), configuration, calCubeTimerModel.getScramblesList().getPuzzleType());
        sessionsListTableModel.getSessionsList().addSession(
                session,
                calCubeTimerModel.getSelectedProfile());

        calCubeTimerModel.getScramblesList().asGenerating().setSession(session);
        calCubeTimerFrame.getTimeLabel().reset();
        calCubeTimerFrame.updateScramble();
    }
}
