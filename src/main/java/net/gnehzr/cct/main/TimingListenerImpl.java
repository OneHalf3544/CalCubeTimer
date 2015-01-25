package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.umts.ircclient.IRCClient;
import org.apache.log4j.Logger;

import java.time.Duration;
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

    private static final Logger LOG = Logger.getLogger(TimingListenerImpl.class);

    @Inject
    private CalCubeTimerModel model;
    @Inject
    private CalCubeTimerGui calCubeTimerFrame;

    private final Configuration configuration;

    @Inject
    private IRCClient ircClient;
    @Inject @Named("timeLabel")
    private TimerLabel timeLabel = null;
    @Inject @Named("bigTimersDisplay")
    private TimerLabel bigTimersDisplay = null;

    @Inject
    public TimingListenerImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    private void updateTime(TimerState newTime) {
        updateHandsState(newTime);
        if(!model.isInspecting()) {
            setTimeLabels(newTime);
        }
    }

    @Override
    // todo move to TimerLabel?
    public void initializeDisplay() {
        TimerState zeroTimerState = new TimerState(configuration, Duration.ZERO);
        updateHandsState(zeroTimerState);
        setTimeLabels(zeroTimerState);
    }

    private void setTimeLabels(TimerState newTime) {
        timeLabel.setTime(newTime);
        bigTimersDisplay.setTime(newTime);
    }

    private void updateHandsState(TimerState newTime) {
        if(newTime instanceof StackmatState) {
            StackmatState newState = (StackmatState) newTime;
            timeLabel.setHands(newState.leftHand(), newState.rightHand());
            timeLabel.setStackmatGreenLight(newState.isGreenLight());
        }
    }

    //I guess we could add an option to prompt the user to see if they want to keep this time
    @Override
    public void timerAccidentlyReset(TimerState lastTimeRead) {
        model.setPenalty(null);
        model.setTiming(false);
        ircClient.sendUserstate();
    }

    @Override
    public void refreshDisplay(TimerState currTime) {
        updateTime(currTime);
    }

    @Override
    public void timerSplit(TimerState newSplit) {
        calCubeTimerFrame.addSplit(newSplit);
    }

    @Override
    public void timerStarted() {
        model.setTiming(true);
        model.stopInspection();
        if(configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false)) {
			calCubeTimerFrame.setFullScreen(true);
		}
        if(configuration.getBoolean(VariableKey.METRONOME_ENABLED, false)) {
			model.startMetronome();
		}
        ircClient.sendUserstate();
    }

    @Override
    public void timerStopped(TimerState newTime) {
        LOG.info("timer stopped: " +  newTime);
        model.setTiming(false);
        model.addTime(newTime);
        if(configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false))
            calCubeTimerFrame.setFullScreen(false);
        if(configuration.getBoolean(VariableKey.METRONOME_ENABLED, false))
            model.stopMetronome();
        ircClient.sendUserstate();
    }

    @Override
    public void stackmatChanged() {
        if(!configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) {
			calCubeTimerFrame.getOnLabel().setText("");
		}
        else {
            boolean on = model.getStackmatInterpreter().isOn();
            timeLabel.setStackmatOn(on);
            if(on) {
                calCubeTimerFrame.getOnLabel().setText(StringAccessor.getString("CALCubeTimer.timerON"));
            } else {
                calCubeTimerFrame.getOnLabel().setText(StringAccessor.getString("CALCubeTimer.timerOFF"));
            }
        }
    }

    @Override
    public void inspectionStarted() {
        LOG.info("inspection started");
        model.setInspectionStart(Instant.now());
        model.startUpdateInspectionTimer();
        ircClient.sendUserstate();
    }
}
