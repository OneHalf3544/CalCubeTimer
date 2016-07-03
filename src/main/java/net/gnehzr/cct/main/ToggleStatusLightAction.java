package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.gnehzr.cct.main.ActionMap.TOGGLE_STATUS_LIGHT_ACTOIN;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
@Component
class ToggleStatusLightAction extends AbstractNamedAction {
	private CALCubeTimerFrame cct;
	private Configuration configuration;

	@Autowired
	public ToggleStatusLightAction(CALCubeTimerFrame cct, Configuration configuration) {
		super(TOGGLE_STATUS_LIGHT_ACTOIN);
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
