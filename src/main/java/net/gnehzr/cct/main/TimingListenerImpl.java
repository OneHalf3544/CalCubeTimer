package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;

import java.time.Instant;

/**
* <p>
* <p>
* Created: 18.11.2014 1:42
* <p>
*
* @author OneHalf
*/
class TimingListenerImpl implements TimingListener {

    private CALCubeTimer calCubeTimer;

    private final Configuration configuration;

    public TimingListenerImpl(CALCubeTimer calCubeTimer, Configuration configuration) {
        this.calCubeTimer = calCubeTimer;
        this.configuration = configuration;
    }

    private void updateTime(TimerState newTime) {
        if(newTime instanceof StackmatState) {
            StackmatState newState = (StackmatState) newTime;
            calCubeTimer.timeLabel.setHands(newState.leftHand(), newState.rightHand());
            calCubeTimer.timeLabel.setStackmatGreenLight(newState.isGreenLight());
        }
        if(!calCubeTimer.isInspecting()) {
            calCubeTimer.timeLabel.setTime(newTime);
            calCubeTimer.bigTimersDisplay.setTime(newTime);
        }
    }

    //I guess we could add an option to prompt the user to see if they want to keep this time
    @Override
    public void timerAccidentlyReset(TimerState lastTimeRead) {
        calCubeTimer.penalty = null;
        calCubeTimer.timing = false;
        calCubeTimer.sendUserstate();
    }

    @Override
    public void refreshDisplay(TimerState currTime) {
        updateTime(currTime);
    }

    @Override
    public void timerSplit(TimerState newSplit) {
        calCubeTimer.addSplit(newSplit);
    }

    @Override
    public void timerStarted() {
        calCubeTimer.timing = true;
        calCubeTimer.stopInspection();
        if(configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false)) {
			calCubeTimer.setFullScreen(true);
		}
        if(configuration.getBoolean(VariableKey.METRONOME_ENABLED, false)) {
			calCubeTimer.startMetronome();
		}
        calCubeTimer.sendUserstate();
    }

    @Override
    public void timerStopped(TimerState newTime) {
        calCubeTimer.timing = false;
        calCubeTimer.addTime(newTime);
        if(configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false))
            calCubeTimer.setFullScreen(false);
        if(configuration.getBoolean(VariableKey.METRONOME_ENABLED, false))
            calCubeTimer.stopMetronome();
        calCubeTimer.sendUserstate();
    }

    @Override
    public void stackmatChanged() {
        if(!configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) {
			calCubeTimer.onLabel.setText("");
		}
        else {
            boolean on = calCubeTimer.stackmatTimer.isOn();
            calCubeTimer.timeLabel.setStackmatOn(on);
            if(on) {
                calCubeTimer.onLabel.setText(StringAccessor.getString("CALCubeTimer.timerON"));
            } else {
                calCubeTimer.onLabel.setText(StringAccessor.getString("CALCubeTimer.timerOFF"));
            }
        }
    }

    @Override
    public void inspectionStarted() {
        calCubeTimer.inspectionStart = Instant.now();
        calCubeTimer.updateInspectionTimer.start();
        calCubeTimer.sendUserstate();
    }
}
