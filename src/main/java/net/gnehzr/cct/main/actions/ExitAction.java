package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.Main;
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

	public ExitAction(){
		super("exit");
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_X);
		this.putValue(ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
	}

	@Override
	public void actionPerformed(ActionEvent e){
		Main.exit(0);
	}
}
