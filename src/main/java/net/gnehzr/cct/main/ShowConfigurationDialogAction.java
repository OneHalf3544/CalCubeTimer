package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
class ShowConfigurationDialogAction extends AbstractAction {
	private CALCubeTimerFrame cct;
	public ShowConfigurationDialogAction(CALCubeTimerFrame cct){
		this.cct = cct;
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
		putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));

	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.showConfigurationDialog();
	}
}
