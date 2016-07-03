package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.gnehzr.cct.main.ActionMap.KEYBOARDTIMING_ACTION;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
@Component
class KeyboardTimingAction extends AbstractNamedAction {

    private final CALCubeTimerFrame cct;
    private final Configuration configuration;
    private final SolvingProcess solvingProcess;
    private final TimerLabelsHolder timerLabelsHolder;
    private final StackmatInterpreter stackmatInterpreter;

    @Autowired
    public KeyboardTimingAction(CALCubeTimerFrame cct, Configuration configuration, SolvingProcess solvingProcess,
                                TimerLabelsHolder timerLabelsHolder, StackmatInterpreter stackmatInterpreter) {
        super(KEYBOARDTIMING_ACTION);
        this.cct = cct;
        this.configuration = configuration;
        this.solvingProcess = solvingProcess;
        this.timerLabelsHolder = timerLabelsHolder;
        this.stackmatInterpreter = stackmatInterpreter;
        this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
        this.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean keyboardTimingSelected = (Boolean) getValue(SELECTED_KEY);
        configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !keyboardTimingSelected);

        cct.getTimeLabel().configurationChanged();
        cct.bigTimersDisplay.configurationChanged();
        stackmatInterpreter.enableStackmat(!keyboardTimingSelected);

        solvingProcess.resetProcess();
        timerLabelsHolder.stackmatChanged();
        if (keyboardTimingSelected) {
            cct.getTimeLabel().requestFocusInWindow();
        } else {
            solvingProcess.resetProcess(); //when the keyboard timer is disabled, we reset the timer
        }
    }
}
