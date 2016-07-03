package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
class ExitAction extends AbstractNamedAction {

	private final JFrame cct;

	@Autowired
	public ExitAction(JFrame cct){
		super("exit");
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
