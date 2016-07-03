package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:21
 * <p>
 *
 * @author OneHalf
 */
@Component
class ResetAction extends AbstractNamedAction {

	private CALCubeTimerFrame cubeTimerFrame;
	private final SessionsList sessionsList;

	@Autowired
	private TimerLabel bigTimersDisplay;

	@Autowired
	public ResetAction(CALCubeTimerFrame cubeTimerFrame, SessionsList sessionsList){
		super("reset");
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		this.cubeTimerFrame = cubeTimerFrame;
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
			bigTimersDisplay.reset();
			cubeTimerFrame.updateScramble();
		}
	}

}
