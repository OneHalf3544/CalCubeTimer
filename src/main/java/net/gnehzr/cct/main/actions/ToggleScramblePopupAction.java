package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.ScramblePopupPanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import javax.swing.*;
import javax.swing.ActionMap;
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
public class ToggleScramblePopupAction extends AbstractNamedAction {

    @Autowired
    private Configuration configuration;

    @Autowired
    private ScramblePopupPanel scramblePopupFrame;

    public ToggleScramblePopupAction() {
        super("togglescramblepopup");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, ((AbstractButton) e.getSource()).isSelected());
        scramblePopupFrame.refreshPopup();
    }
}
