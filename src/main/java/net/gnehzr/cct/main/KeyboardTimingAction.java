package net.gnehzr.cct.main;

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

	public static final String KEYBOARD_TIMING_ACTION = "keyboardtiming";

	private CALCubeTimerFrame cct;

	public KeyboardTimingAction(CALCubeTimerFrame cct){
		this.cct = cct;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
		this.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.keyboardTimingAction();
	}
}
