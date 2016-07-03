package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.SolvingProcess;
import net.gnehzr.cct.main.TimerLabelsHolder;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static net.gnehzr.cct.main.actions.ActionMap.KEYBOARDTIMING_ACTION;

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

    private final Configuration configuration;
    private final SolvingProcess solvingProcess;
    private final TimerLabelsHolder timerLabelsHolder;
    private final StackmatInterpreter stackmatInterpreter;

    @Autowired
    private TimerLabel bigTimersDisplay;
    @Autowired
    private TimerLabel timeLabel;

    @Autowired
    public KeyboardTimingAction(Configuration configuration, SolvingProcess solvingProcess,
                                TimerLabelsHolder timerLabelsHolder, StackmatInterpreter stackmatInterpreter) {
        super(KEYBOARDTIMING_ACTION);
        this.configuration = configuration;
        this.solvingProcess = solvingProcess;
        this.timerLabelsHolder = timerLabelsHolder;
        this.stackmatInterpreter = stackmatInterpreter;
        this.putValue(MNEMONIC_KEY, KeyEvent.VK_K);
        this.putValue(SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean keyboardTimingSelected = (Boolean) getValue(SELECTED_KEY);
        configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !keyboardTimingSelected);

        timeLabel.configurationChanged();
        bigTimersDisplay.configurationChanged();
        stackmatInterpreter.enableStackmat(!keyboardTimingSelected);

        solvingProcess.resetProcess();
        timerLabelsHolder.stackmatChanged();
        if (keyboardTimingSelected) {
            timeLabel.requestFocusInWindow();
        } else {
            solvingProcess.resetProcess(); //when the keyboard timer is disabled, we reset the timer
        }
    }
}
