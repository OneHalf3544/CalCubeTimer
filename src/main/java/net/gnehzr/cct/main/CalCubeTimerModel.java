package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;

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

    boolean isTiming();

    void setTiming(boolean timing);

    boolean isFullscreen();

    void setFullscreen(boolean fullscreen);

    ScrambleList getScramblesList();

    TimingListener getTimingListener();

    long getLastSplit();

    StackmatInterpreter getStackmatInterpreter();

    void setLastSplit(long lastSplit);

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    InspectionState getInspectionValue();

    boolean isInspecting();

    List<SolveTime> getSplits();

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

    void setSelectedProfile(Profile currentProfile);

    Profile getSelectedProfile();

    SessionsList getSessionsList();

    void setScramblesList(ScrambleList scrambleList);
}
