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
class RequestScrambleAction extends AbstractAction {

	private CALCubeTimerFrame cct;

	public RequestScrambleAction(CALCubeTimerFrame cct){
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.model.getScramblesList().generateNext();
		cct.updateScramble();
	}
}
