package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleListHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
class RequestScrambleAction extends AbstractNamedAction {

	private ScrambleListHolder scrambleListHolder;

	@Autowired
	public RequestScrambleAction(CALCubeTimerFrame cct, ScrambleListHolder scrambleListHolder){
		super("requestscramble");
		this.scrambleListHolder = scrambleListHolder;
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		scrambleListHolder.generateNext();
	}
}
