package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:21
 * <p>
 *
 * @author OneHalf
 */
class ExitAction extends AbstractAction {

	private final JFrame cct;

	public ExitAction(JFrame cct){
		this.cct = cct;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
		this.putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));

	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.dispose();
	}
}
