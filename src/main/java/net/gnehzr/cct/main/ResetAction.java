package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;

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

	public ResetAction(CALCubeTimerFrame cubeTimerFrame){
		this.cubeTimerFrame = cubeTimerFrame;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		resetAction();
	}

	public void resetAction() {
		int choice = Utils.showYesNoDialog(cubeTimerFrame, StringAccessor.getString("CALCubeTimer.confirmreset"));
		if(choice == JOptionPane.YES_OPTION) {
			cubeTimerFrame.getTimeLabel().reset();
			cubeTimerFrame.bigTimersDisplay.reset();
			cubeTimerFrame.model.getScramblesList().asGenerating().clear();
			cubeTimerFrame.updateScramble();
			cubeTimerFrame.currentSessionSolutionsTableModel.getCurrentSession().getSessionPuzzleStatistics().clear();
		}
	}

}
