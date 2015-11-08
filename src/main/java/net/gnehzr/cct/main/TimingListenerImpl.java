package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.KeyboardHandler;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.Solution;
import net.gnehzr.cct.statistics.SolveTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
* <p>
* <p>
* Created: 18.11.2014 1:42
* <p>
*
* @author OneHalf
*/
@Singleton
class TimingListenerImpl implements TimingListener, SolvingProcessListener {

    private static final Logger LOG = LogManager.getLogger(TimingListenerImpl.class);

    @Inject
    private CalCubeTimerModel model;
    @Inject
    private CalCubeTimerGui calCubeTimerFrame;
    @Inject
    private SessionsList sessionsList;
    @Inject @Named("timeLabel")
    private TimerLabel timeLabel;
    @Inject @Named("bigTimersDisplay")
    private TimerLabel bigTimersDisplay;
    @Inject
    private Metronome metronome;

    private boolean stackmatEnabled;
    private final Configuration configuration;
    private boolean fullScreenTiming;
    @Inject
    private KeyboardHandler keyHandler;

    @Inject
    public TimingListenerImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    void init() {
        configuration.addConfigurationChangeListener(p -> configurationChanged());
    }

    @Override
    public void refreshTimer() {

    }

    @Override
    public void changeGreenLight(boolean greenLight) {
        timeLabel.greenLight = greenLight;
        bigTimersDisplay.greenLight = greenLight;
    }

    @Override
    public void refreshDisplay(TimerState newTime) {
        timeLabel.updateHandsState(newTime);
        if(!model.getSolvingProcess().isInspecting()) {
            timeLabel.setTime(newTime);
            bigTimersDisplay.setTime(newTime);
        }
    }

    //I guess we could add an option to prompt the user to see if they want to keep this time
    @Override
    public void timerAccidentlyReset(TimerState lastTimeRead) {
        model.getSolvingProcess().setInspectionPenalty(null);
        //model.getSolvingProcess().setTiming(false);
    }

    @Override
    public void timerSplit(TimerState newSplit) {
        model.getSolvingProcess().addSplit(newSplit);
    }

    @Override
    public void timerStarted() {
        LOG.debug("timer started");
        model.getSolvingProcess().startSolving();
        metronome.startMetronome(configuration.getInt(VariableKey.METRONOME_DELAY));

        if(fullScreenTiming) {
			calCubeTimerFrame.setFullscreen(true);
		}
    }

    void configurationChanged() {
        stackmatEnabled = configuration.getBoolean(VariableKey.STACKMAT_ENABLED);
        fullScreenTiming = configuration.getBoolean(VariableKey.FULLSCREEN_TIMING);
        metronome.setEnabled(configuration.getBoolean(VariableKey.METRONOME_ENABLED));
    }

    @Override
    public void timerStopped(TimerState newTime) {
        LOG.debug("timer stopped: " + new SolveTime(newTime.getTime()));
        metronome.stopMetronome();
        model.getSolvingProcess().solvingFinished(newTime);

        if(fullScreenTiming) {
            calCubeTimerFrame.setFullscreen(false);
        }
    }

    @Override
    public void timerAccepted(Solution solution) {
        sessionsList.addSolutionToCurrentSession(solution);
    }

    @Override
    public void stackmatChanged() {
        if (stackmatEnabled) {
            boolean on = model.getStackmatInterpreter().isOn();
            keyHandler.setStackmatOn(on);
            calCubeTimerFrame.getOnLabel().setText(StringAccessor.getString(on ? "CALCubeTimer.timerON" : "CALCubeTimer.timerOFF"));
        } else {
			calCubeTimerFrame.getOnLabel().setText("");
		}
    }

    @Override
    public void inspectionStarted() {
        LOG.info("inspection started");
        model.getSolvingProcess().startInspection();
    }
}
