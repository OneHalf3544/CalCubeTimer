package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
@Service
class ToggleScramblePopupAction extends AbstractAction {

    @Autowired
    private Configuration configuration;

    @Autowired
    private ScramblePopupPanel scramblePopupFrame;

    @Override
    public void actionPerformed(ActionEvent e) {
        configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, ((AbstractButton) e.getSource()).isSelected());
        scramblePopupFrame.refreshPopup();
    }

    @Autowired
    void registerAction(ActionMap actionMap) {
        actionMap.registerAction("togglescramblepopup", this);
    }
}
