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

    public TimingListenerImpl(CALCubeTimer calCubeTimer) {
        this.calCubeTimer = calCubeTimer;
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
    public void timerAccidentlyReset(TimerState lastTimeRead) {
        calCubeTimer.penalty = null;
        calCubeTimer.timing = false;
        calCubeTimer.sendUserstate();
    }

    public void refreshDisplay(TimerState currTime) {
        updateTime(currTime);
    }

    public void timerSplit(TimerState newSplit) {
        calCubeTimer.addSplit(newSplit);
    }

    public void timerStarted() {
        calCubeTimer.timing = true;
        calCubeTimer.stopInspection();
        if(Configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false)) {
			calCubeTimer.setFullScreen(true);
		}
        if(Configuration.getBoolean(VariableKey.METRONOME_ENABLED, false)) {
			calCubeTimer.startMetronome();
		}
        calCubeTimer.sendUserstate();
    }

    public void timerStopped(TimerState newTime) {
        calCubeTimer.timing = false;
        calCubeTimer.addTime(newTime);
        if(Configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false))
            calCubeTimer.setFullScreen(false);
        if(Configuration.getBoolean(VariableKey.METRONOME_ENABLED, false))
            calCubeTimer.stopMetronome();
        calCubeTimer.sendUserstate();
    }

    public void stackmatChanged() {
        if(!Configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) {
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

    public void inspectionStarted() {
        calCubeTimer.inspectionStart = Instant.now();
        calCubeTimer.updateInspectionTimer.start();
        calCubeTimer.sendUserstate();
    }
}
