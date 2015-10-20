package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:21
 * <p>
 *
 * @author OneHalf
 */
class ResetAction extends AbstractAction {

	private CALCubeTimerFrame cubeTimerFrame;
	private final Configuration configuration;
	private final SessionsList sessionsList;

	public ResetAction(CALCubeTimerFrame cubeTimerFrame, Configuration configuration, SessionsList sessionsList){
		this.cubeTimerFrame = cubeTimerFrame;
		this.configuration = configuration;
		this.sessionsList = sessionsList;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		resetAction();
	}

	public void resetAction() {
		int choice = Utils.showYesNoDialog(cubeTimerFrame, StringAccessor.getString("CALCubeTimer.confirmreset"));
		if(choice == JOptionPane.YES_OPTION) {
			sessionsList.createSession(sessionsList.getCurrentSession().getPuzzleType());
			cubeTimerFrame.getTimeLabel().reset();
			cubeTimerFrame.bigTimersDisplay.reset();
			cubeTimerFrame.updateScramble();
		}
	}

}
