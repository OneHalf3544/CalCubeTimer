package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
class KeyboardTimingAction extends AbstractAction {

	private final CALCubeTimerFrame cct;
	private final Configuration configuration;

	public KeyboardTimingAction(CALCubeTimerFrame cct, Configuration configuration){
		this.cct = cct;
		this.configuration = configuration;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
		this.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
	}

	@Override
	public void actionPerformed(ActionEvent e){
		boolean selected = (Boolean) getValue(SELECTED_KEY);
		configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !selected);
		cct.getTimeLabel().configurationChanged();
		cct.bigTimersDisplay.configurationChanged();
		cct.model.getStackmatInterpreter().enableStackmat(!selected);

		SolvingProcess solvingProcess = cct.model.getSolvingProcess();
		solvingProcess.resetProcess();
		solvingProcess.getTimingListener().stackmatChanged();
		if(selected) {
			cct.getTimeLabel().requestFocusInWindow();
		}
		else {
			solvingProcess.resetProcess(); //when the keyboard timer is disabled, we reset the timer
		}
	}
}
