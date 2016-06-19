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
class ToggleStatusLightAction extends AbstractAction {
	private CALCubeTimerFrame cct;
	private Configuration configuration;

	public ToggleStatusLightAction(CALCubeTimerFrame cct, Configuration configuration){
		this.cct = cct;
		this.configuration = configuration;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);

	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.LESS_ANNOYING_DISPLAY, (Boolean) getValue(SELECTED_KEY));
		cct.getTimeLabel().repaint();
	}
}
