package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
* <p>
* <p>
* Created: 22.01.2015 1:21
* <p>
*
* @author OneHalf
*/
@Singleton
class ToggleScramblePopupAction extends AbstractAction {

    @Inject
    private Configuration configuration;

    @Inject
    private ScramblePopupPanel scramblePopupFrame;

    @Override
    public void actionPerformed(ActionEvent e) {
        configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, ((AbstractButton) e.getSource()).isSelected());
        scramblePopupFrame.refreshPopup();
    }

    @Inject
    void registerAction(ActionMap actionMap) {
        actionMap.registerAction(ActionMap.TOGGLE_SCRAMBLE_POPUP_ACTION, this);
    }
}
