package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.SolveType;
import net.gnehzr.cct.statistics.StatisticsTableModel;

import java.time.Instant;
import java.util.List;

/**
 * <p>
 * <p>
 * Created: 19.01.2015 10:08
 * <p>
 *
 * @author OneHalf
 */
public interface CalCubeTimerModel extends SessionListener {

    String SCRAMBLE_ATTRIBUTE_CHANGED = "Scramble Attribute Changed";

    boolean isLoading();

    void setLoading(boolean loading);

    boolean isTiming();

    void setTiming(boolean timing);

    boolean isFullscreen();

    void setFullscreen(boolean fullscreen);

    ScrambleList getScramblesList();

    StatisticsTableModel getStatsModel();

    TimingListener getTimingListener();

    long getLastSplit();

    StackmatInterpreter getStackmatInterpreter();

    void setLastSplit(long lastSplit);

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    long getInpectionValue();

    boolean isInspecting();

    List<SolveTime> getSplits();

    void speakTime(SolveTime latestTime, CALCubeTimerFrame calCubeTimerFrame);

    void stopInspection();

    void setPenalty(SolveType plusTwo);

    Metronome getMetronome();

    NumberSpeaker getNumberSpeaker();

    LocaleAndIcon getLoadedLocale();

    void setLoadedLocale(LocaleAndIcon newLocale);

    boolean getCustomizationEditsDisabled();

    void setCustomizationEditsDisabled(boolean b);

    void startMetronome();

    void addTime(TimerState newTime);

    void stopMetronome();

    void setInspectionStart(Instant now);

    void startUpdateInspectionTimer();

    void prepareForProfileSwitch();

    Session getNextSession(CALCubeTimerFrame calCubeTimerFrame);
}
