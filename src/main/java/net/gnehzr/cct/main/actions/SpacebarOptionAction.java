package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.gnehzr.cct.main.actions.ActionMap.TOGGLE_SPACEBAR_STARTS_TIMER_ACTION;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
@Component
class SpacebarOptionAction extends AbstractAction {

	private final net.gnehzr.cct.configuration.Configuration configuration;

	@Autowired
	public SpacebarOptionAction(Configuration configuration){
		super(TOGGLE_SPACEBAR_STARTS_TIMER_ACTION);
		this.configuration = configuration;
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.SPACEBAR_ONLY, ((AbstractButton)e.getSource()).isSelected());
	}
}
