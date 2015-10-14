package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.Session;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;

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

	public ResetAction(CALCubeTimerFrame cubeTimerFrame, Configuration configuration){
		this.cubeTimerFrame = cubeTimerFrame;
		this.configuration = configuration;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		resetAction();
	}

	public void resetAction() {
		int choice = Utils.showYesNoDialog(cubeTimerFrame, StringAccessor.getString("CALCubeTimer.confirmreset"));
		if(choice == JOptionPane.YES_OPTION) {
			Session newSession = new Session(LocalDateTime.now(), configuration, cubeTimerFrame.model.getScramblesList().getPuzzleType());

			cubeTimerFrame.getTimeLabel().reset();
			cubeTimerFrame.bigTimersDisplay.reset();
			cubeTimerFrame.model.getScramblesList().asGenerating().setSession(newSession);
			cubeTimerFrame.currentSessionSolutionsTableModel.setCurrentSession(newSession);
			cubeTimerFrame.updateScramble();
		}
	}

}
