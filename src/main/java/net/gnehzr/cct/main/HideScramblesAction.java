package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

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
class HideScramblesAction extends AbstractAction {

	private CALCubeTimerFrame cctFrame;
	private Configuration configuration;
	private final ActionMap actionMap;

	public HideScramblesAction(CALCubeTimerFrame cctFrame, Configuration configuration, ActionMap actionMap){
		this.cctFrame = cctFrame;
		this.configuration = configuration;
		this.actionMap = actionMap;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.HIDE_SCRAMBLES, (Boolean) actionMap.getAction("togglehidescrambles", cctFrame).getValue(SELECTED_KEY));
		cctFrame.scrambleHyperlinkArea.refresh();
	}
}
