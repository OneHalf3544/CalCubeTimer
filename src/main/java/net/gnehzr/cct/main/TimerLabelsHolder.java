package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.KeyboardHandler;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.SessionsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class TimerLabelsHolder {
	@Autowired
	private SessionsList sessionsList;
	@Autowired @Qualifier("timeLabel")
	private TimerLabel timeLabel;
	@Autowired @Qualifier("bigTimersDisplay")
	private TimerLabel bigTimersDisplay;
	@Autowired
	private KeyboardHandler keyHandler;
	@Autowired
	private SolvingProcess solvingProcess;
    @Autowired
    private Configuration configuration;
    @Autowired
    private StackmatInterpreter stackmatInterpreter;
    @Autowired
    private StackmatPluggedIndicatorLabel stackmatPluggedIndicatorLabel;

    public void refreshTimer(String s) {

	}

	public void changeGreenLight(boolean greenLight) {
		timeLabel.greenLight = greenLight;
		bigTimersDisplay.greenLight = greenLight;
	}

	public void refreshDisplay(TimerState newTime) {
		timeLabel.updateHandsState(newTime);
		if(!solvingProcess.isInspecting()) {
			timeLabel.setTime(newTime);
			bigTimersDisplay.setTime(newTime);
		}
	}

    public void stackmatChanged() {
        boolean stackmatEnabled = configuration.getBoolean(VariableKey.STACKMAT_ENABLED);
        if (stackmatEnabled) {
			boolean on = stackmatInterpreter.isOn();
			keyHandler.setStackmatOn(on);
            stackmatPluggedIndicatorLabel.setText(StringAccessor.getString(on ? "CALCubeTimer.timerON" : "CALCubeTimer.timerOFF"));
		} else {
			stackmatPluggedIndicatorLabel.setText("");
		}
	}
}
