package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CurrentProfileHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import javax.swing.*;
import javax.swing.ActionMap;
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
class FullScreenDuringTimingChangeSettingAction extends AbstractNamedAction {

	public static final String TOGGLE_FULLSCREEN_TIMING_ACTION = "togglefullscreentiming";

	private final Configuration configuration;
	private final CurrentProfileHolder currentProfileHolder;

	@Autowired
	public FullScreenDuringTimingChangeSettingAction(Configuration configuration,
													 CurrentProfileHolder currentProfileHolder){
		super(TOGGLE_FULLSCREEN_TIMING_ACTION);
		this.configuration = configuration;
		this.currentProfileHolder = currentProfileHolder;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.FULLSCREEN_TIMING, ((AbstractButton)e.getSource()).isSelected());
		// save now, because this setting is changed from main menu, and user have no "save" button
		configuration.saveConfiguration(currentProfileHolder.getSelectedProfile());
	}
}
