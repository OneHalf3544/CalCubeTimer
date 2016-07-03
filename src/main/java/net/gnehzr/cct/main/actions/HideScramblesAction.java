package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.ScrambleHyperlinkArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.gnehzr.cct.main.actions.ActionMap.TOGGLE_HIDE_SCRAMBLES;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
@Service
class HideScramblesAction extends AbstractAction {

	private Configuration configuration;
	@Autowired
	private ScrambleHyperlinkArea scrambleHyperlinkArea;

	@Autowired
	public HideScramblesAction(Configuration configuration){
		super(TOGGLE_HIDE_SCRAMBLES);
		this.configuration = configuration;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.HIDE_SCRAMBLES, (Boolean) getValue(SELECTED_KEY));
		scrambleHyperlinkArea.refresh();
	}
}
