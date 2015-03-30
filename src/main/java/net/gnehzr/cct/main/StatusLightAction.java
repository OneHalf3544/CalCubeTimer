package net.gnehzr.cct.main;

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
class StatusLightAction extends AbstractAction {
	private CALCubeTimerFrame cct;

	public StatusLightAction(CALCubeTimerFrame cct){
		this.cct = cct;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);

	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.statusLightAction();
	}
}
