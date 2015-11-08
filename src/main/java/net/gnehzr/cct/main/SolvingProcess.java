package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.SolveType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * <p>
 * Created: 02.11.2015 7:50
 * <p>
 *
 * @author OneHalf
 */
public interface SolvingProcess {

    /**
     * Reset time labels and cleanup internal state for new solve
     */
    void resetProcess();

    boolean canStartProcess();

    boolean isRunning();

    void startProcess();

    /**
     * start inspection countdown
     */
    void startInspection();

    /**
     *
     * @return true, if inspection run
     */
    boolean isInspecting();

    /**
     * @return amount of inspection remaining (in seconds)
     */
    Optional<InspectionState> getInspectionState();

    void setInspectionPenalty(SolveType penalty);

    /**
     * run timer
     */
    void startSolving();

    /**
     *
     * @return true, if timer is running
     */
    boolean isSolving();

    /**
     * Add solving time of a puzzle part
     * @param state partial solving time
     */
    void addSplit(TimerState state);

    List<SolveTime> getSplits();
    /**
     * Add solution after solving finish
     * @param newTime result of solve
     */
    void solvingFinished(TimerState newTime);

    Duration getElapsedTime();

    TimingListener getTimingListener();

    TimerState getTimerState();
}
