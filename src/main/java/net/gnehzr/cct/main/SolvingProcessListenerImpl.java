package net.gnehzr.cct.main;

import net.gnehzr.cct.misc.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
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

import javax.annotation.PostConstruct;
import javax.swing.*;

/**
* <p>
* <p>
* Created: 18.11.2014 1:42
* <p>
*
* @author OneHalf
*/
@Service
class SolvingProcessListenerImpl implements SolvingProcessListener {

    private static final Logger LOG = LogManager.getLogger(SolvingProcessListenerImpl.class);

    @Autowired
    private CalCubeTimerModel model;
    @Autowired
    private CALCubeTimerFrame calCubeTimerFrame;
    @Autowired
    private SessionsList sessionsList;
    @Autowired @Qualifier("timeLabel")
    private TimerLabel timeLabel;
    @Autowired @Qualifier("bigTimersDisplay")
    private TimerLabel bigTimersDisplay;
    @Autowired
    private Metronome metronome;

    private final Configuration configuration;
    private boolean fullScreenTiming;
    @Autowired
    private KeyboardHandler keyHandler;
    @Autowired
    private SolvingProcess solvingProcess;

    @Autowired
    public SolvingProcessListenerImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    void init() {
        configuration.addConfigurationChangeListener(p -> configurationChanged());
        solvingProcess.setSolvingProcessListener(this);
    }

    //I guess we could add an option to prompt the user to see if they want to keep this time
    @Override
    public void timerAccidentlyReset(TimerState lastTimeRead) {
        solvingProcess.setInspectionPenalty(null);
        //solvingProcess.setTiming(false);
    }

    @Override
    public void timerSplit(TimerState newSplit) {
        //
    }

    @Override
    public void timerStarted() {
        LOG.debug("timer started");
        metronome.startMetronome(configuration.getInt(VariableKey.METRONOME_DELAY));

        if(fullScreenTiming) {
			calCubeTimerFrame.setFullscreen(true);
		}
    }

    private void configurationChanged() {
        fullScreenTiming = configuration.getBoolean(VariableKey.FULLSCREEN_TIMING);
        metronome.setEnabled(configuration.getBoolean(VariableKey.METRONOME_ENABLED));
    }

    @Override
    public void timerStopped() {
        metronome.stopMetronome();

        if(fullScreenTiming) {
            calCubeTimerFrame.setFullscreen(false);
        }
    }

    @Override
    public boolean confirmDuplicateTime(TimerState timerState) {
        int choice = Utils.showYesNoDialog(calCubeTimerFrame.getMainFrame(),
                timerState.toString() + "\n"
                        + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
        return choice == JOptionPane.YES_OPTION;
    }

    @Override
    public void timerAccepted(Solution solution) {
        sessionsList.addSolutionToCurrentSession(solution);
    }

    @Override
    public void inspectionStarted() {
        LOG.info("inspection started");
    }
}
