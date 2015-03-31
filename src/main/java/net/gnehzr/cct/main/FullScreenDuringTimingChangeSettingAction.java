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
class FullScreenDuringTimingChangeSettingAction extends AbstractAction {
	private final Configuration configuration;

	public FullScreenDuringTimingChangeSettingAction(Configuration configuration){
		this.configuration = configuration;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.FULLSCREEN_TIMING, ((AbstractButton)e.getSource()).isSelected());
	}
}
