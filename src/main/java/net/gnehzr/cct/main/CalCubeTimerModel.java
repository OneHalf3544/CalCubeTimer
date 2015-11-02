package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SolveType;

/**
 * <p>
 * <p>
 * Created: 19.01.2015 10:08
 * <p>
 *
 * @author OneHalf
 */
public interface CalCubeTimerModel extends CurrentProfileHolder, SolvingProcess {

    String SCRAMBLE_ATTRIBUTE_CHANGED = "Scramble Attribute Changed";

    StackmatInterpreter getStackmatInterpreter();

    Metronome getMetronome();

    void setPenalty(SolveType plusTwo);

    void addTime(TimerState newTime);

    void startUpdateInspectionTimer();

    NumberSpeaker getNumberSpeaker();

    LocaleAndIcon getLoadedLocale();

    void setLoadedLocale(LocaleAndIcon newLocale);

    void saveProfileConfiguration();

    Session getCurrentSession();

    void setSelectedProfile(Profile currentProfile);

    ScrambleList getScramblesList();

    void setScramblesList(ScrambleList scrambleList);
}
