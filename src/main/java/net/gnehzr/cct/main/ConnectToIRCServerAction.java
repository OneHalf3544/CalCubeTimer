package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.umts.ircclient.IRCClientGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
* <p>
* <p>
* Created: 17.01.2015 10:12
* <p>
*
* @author OneHalf
*/
@Singleton
public class ConnectToIRCServerAction extends AbstractAction {

    @Inject
    private IRCClientGUI ircClient;

    @Inject
    public ConnectToIRCServerAction() {
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e == null) { //this means that the client gui was disposed
            this.setEnabled(true);
        } else {
            ircClient.setVisible(true);
            this.setEnabled(false);
        }
    }

    @Inject
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction(ActionMap.CONNECT_TO_SERVER_ACTION, this);
    }
}
