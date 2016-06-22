package net.gnehzr.cct.main;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.scrambles.ScrambleListHolder;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.KeyboardTimerState;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Solution;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.SolveType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * <p>
 * Created: 02.11.2015 7:50
 * <p>
 *
 * @author OneHalf
 */
@Service
public class SolvingProcess {

    private static final Logger LOG = LogManager.getLogger(SolvingProcess.class);
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
    private static final Duration PERIOD = Duration.ofMillis(45);
    private final CalCubeTimerGui calCubeTimerGui;
    protected final Configuration configuration;
    private final NumberSpeaker numberSpeaker;
    private final SolvingProcessListener solvingProcessListener;
    private final CompositeTimingListener timingListeners = new CompositeTimingListener();
    private ScrambleListHolder scrambleListHolder;
    private ScheduledFuture<?> scheduledFuture;
    private List<SolveTime> splits = new ArrayList<>();
    private InspectionState previousInspectionState = new InspectionState(Instant.now(), Instant.now());
    private SolveType penalty = null;
    private long lastSplit;
    private StackmatState lastAccepted = new StackmatState(null, Collections.emptyList());
    private Instant inspectionStartTime = null;
    private Instant solvingStartTime;
    private Instant currentTime;

    @Autowired
    public SolvingProcess(NumberSpeaker numberSpeaker, ScrambleListHolder scrambleListHolder,
                          CalCubeTimerGui calCubeTimerGui, Configuration configuration,
                          SolvingProcessListener solvingProcessListener) {
        this.numberSpeaker = numberSpeaker;
        this.scrambleListHolder = scrambleListHolder;
        this.calCubeTimerGui = calCubeTimerGui;
        this.configuration = configuration;
        this.solvingProcessListener = solvingProcessListener;
    }

    private void refreshTime() {
        if (isInspecting()) {
            InspectionState inspection = getInspectionState().get();

            if (inspection.isDisqualification()) {
                setInspectionPenalty(SolveType.DNF);
                setTextToTimeLabels(StringAccessor.getString("CALCubeTimer.disqualification"));
            }
            else if (inspection.isPenalty()) {
                setInspectionPenalty(SolveType.PLUS_TWO);
                setTextToTimeLabels(StringAccessor.getString("CALCubeTimer.+2penalty"));
            }
            else {
                setTextToTimeLabels(String.valueOf(inspection.getElapsedTime().getSeconds()));
            }
        }

        currentTime = Instant.now();
        timingListeners.refreshDisplay(getTimerState());
    }

    private void setTextToTimeLabels(String time) {
        timingListeners.refreshTimer();
    }

    public void resetProcess() {
        LOG.debug("reset process");
        if (isRunning()) {
            scheduledFuture.cancel(true);
        }
        inspectionStartTime = null;
        solvingStartTime = null;
        scheduledFuture = null;

        timingListeners.refreshTimer();
    }

    @PostConstruct
    void initialize() {
        LOG.debug("initialize solvingProcess");
        currentTime = Instant.now();
    }

    public boolean canStartProcess() {
        return Duration.between(currentTime, Instant.now()).toMillis() > configuration.getInt(VariableKey.DELAY_BETWEEN_SOLVES);
    }

    public boolean isRunning() {
        return scheduledFuture != null && !scheduledFuture.isCancelled();
    }

    public void startProcess() {
        if (!canStartProcess()) {
            return;
        }

        LOG.debug("start process");
        Objects.requireNonNull(currentScramble());

        boolean inspectionEnabled = configuration.getBoolean(VariableKey.COMPETITION_INSPECTION);
        currentTime = Instant.now();

        scheduledFuture = EXECUTOR.scheduleAtFixedRate(
                this::refreshTime, 0, PERIOD.toMillis(), TimeUnit.MILLISECONDS);

        if (inspectionEnabled && !isInspecting()) {
            this.inspectionStartTime = currentTime;
            solvingProcessListener.inspectionStarted();
        }
        else {
            startSolving();
        }
    }

    public void startInspection() {
        LOG.debug("start inspection");
        Objects.requireNonNull(currentScramble());
        this.inspectionStartTime = Instant.now();
    }

    public boolean isInspecting() {
        return isRunning() && inspectionStartTime != null && solvingStartTime == null;
    }

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    public Optional<InspectionState> getInspectionState() {
        if (!isInspecting()) {
            return Optional.<InspectionState>empty();
        }
        InspectionState inspectionState = new InspectionState(inspectionStartTime, currentTime);

        if (!configuration.getBoolean(VariableKey.SPEAK_INSPECTION) || Objects.equals(inspectionState, previousInspectionState)) {
            return Optional.of(inspectionState);
        }
        previousInspectionState = inspectionState;

        if (inspectionState.getElapsedTime().getSeconds() == InspectionState.FIRST_WARNING.getSeconds()) {
            numberSpeaker.sayInspectionWarning(InspectionState.FIRST_WARNING);
        }
        else if (inspectionState.getElapsedTime().getSeconds() == InspectionState.FINAL_WARNING.getSeconds()) {
            numberSpeaker.sayInspectionWarning(InspectionState.FINAL_WARNING);
        }
        return Optional.of(inspectionState);
    }

    public void setInspectionPenalty(SolveType penalty) {
        LOG.debug("inspection penalty {}", penalty);
        this.penalty = penalty;
    }

    public void startSolving() {
        LOG.debug("start solving");
        solvingStartTime = currentTime;
        solvingProcessListener.timerStarted();
    }

    public boolean isSolving() {
        return isRunning() && solvingStartTime != null;
    }

    public void addSplit(TimerState state) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastSplit) / 1000. > configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)) {
            LOG.debug("add split {}", currentTime);
            getSplits().add(state.toSolution(currentScramble(), ImmutableList.of()).getTime());
            this.lastSplit = currentTime;
        }
    }

    public List<SolveTime> getSplits() {
        return splits;
    }

    public void solvingFinished(TimerState timerState) {
        LOG.debug("solving finished");
        Solution solution = timerState.toSolution(currentScramble(), splits);
        if (penalty == null) {
            solution.getTime().deleteTypes();
        }
        else {
            solution.getTime().setTypes(Collections.singletonList(penalty));
        }
        penalty = null;
        splits = new ArrayList<>();
        boolean sameAsLast = timerState.compareTo(lastAccepted) == 0;
        if(sameAsLast) {
            int choice = Utils.showYesNoDialog(calCubeTimerGui.getMainFrame(),
                    timerState.toString() + "\n"
                            + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
            if(choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (promptNewTime(solution, sameAsLast)) {
            solvingProcessListener.timerAccepted(solution);
        }
        scrambleListHolder.generateNext();
        calCubeTimerGui.updateScramble();

        timingListeners.refreshDisplay(getTimerState());
    }

    @NotNull
    private ScrambleString currentScramble() {
        return scrambleListHolder.getCurrentScramble();
    }

    public Duration getElapsedTime() {
        if (!isRunning()) {
            return Duration.ZERO;
        }
        return Duration.between(solvingStartTime == null ? inspectionStartTime : solvingStartTime, currentTime);
    }

    public TimingListener getTimingListener() {
        return timingListeners;
    }

    private boolean promptNewTime(Solution protect, boolean sameAsLast) {
        int choice = JOptionPane.YES_OPTION;
        if(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME) && !sameAsLast) {
            String[] OPTIONS = { StringAccessor.getString("CALCubeTimer.accept"), SolveType.PLUS_TWO.toString(), SolveType.DNF.toString() };
            //This leaves +2 and DNF enabled, even if the user just got a +2 or DNF from inspection.
            //I don't really care however, since I doubt that anyone even uses this feature. --Jeremy
            choice = JOptionPane.showOptionDialog(null,
                    StringAccessor.getString("CALCubeTimer.yourtime")
                            + protect.getTime().toString()
                            + StringAccessor.getString("CALCubeTimer.newtimedialog"),
                    StringAccessor.getString("CALCubeTimer.confirm"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    OPTIONS,
                    OPTIONS[0]);
        }

        switch (choice) {
            case JOptionPane.YES_OPTION:
                return true;
            case JOptionPane.NO_OPTION:
                protect.getTime().setTypes(Collections.singletonList(SolveType.PLUS_TWO));
                return true;
            case JOptionPane.CANCEL_OPTION:
                protect.getTime().setTypes(Collections.singletonList(SolveType.DNF));
                return true;
            default:
                return false;
        }
    }

    public TimerState getTimerState() {
        return new KeyboardTimerState(getElapsedTime(), getInspectionState());
    }

    private static class CompositeTimingListener implements TimingListener {

        private final List<TimingListener> timingListeners = new ArrayList<>();

        @Override
        public void refreshDisplay(TimerState currTime) {
            timingListeners.forEach(tl -> tl.refreshDisplay(currTime));
        }

        @Override
        public void stackmatChanged() {
            timingListeners.forEach(TimingListener::stackmatChanged);
        }

        @Override
        public void refreshTimer() {
            timingListeners.forEach(TimingListener::refreshTimer);
        }

        @Override
        public void changeGreenLight(boolean b) {
            timingListeners.forEach(tl -> tl.changeGreenLight(b));
        }
    }
}
