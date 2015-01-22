package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
class KeyboardTimingAction extends AbstractAction {
	private CALCubeTimerFrame cct;
	public KeyboardTimingAction(CALCubeTimerFrame cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.keyboardTimingAction();
	}
}
