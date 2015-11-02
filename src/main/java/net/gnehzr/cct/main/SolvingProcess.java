package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.statistics.SolveTime;

import java.time.Instant;
import java.util.List;

/**
 * <p>
 * <p>
 * Created: 02.11.2015 7:50
 * <p>
 *
 * @author OneHalf
 */
public interface SolvingProcess {

    boolean isTiming();

    void setTiming(boolean timing);

    TimingListener getTimingListener();

    long getLastSplit();

    void setLastSplit(long lastSplit);

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    InspectionState getInspectionValue();

    boolean isInspecting();

    List<SolveTime> getSplits();

    void stopInspection();

    void startMetronome();

    void stopMetronome();

    void setInspectionStart(Instant now);

}
