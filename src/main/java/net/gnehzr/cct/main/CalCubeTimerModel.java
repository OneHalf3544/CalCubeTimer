package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.statistics.Profile;

/**
 * <p>
 * <p>
 * Created: 19.01.2015 10:08
 * <p>
 *
 * @author OneHalf
 */
public interface CalCubeTimerModel extends CurrentProfileHolder {

    SolvingProcess getSolvingProcess();

    StackmatInterpreter getStackmatInterpreter();

    LocaleAndIcon getLoadedLocale();

    void setLoadedLocale(LocaleAndIcon newLocale);

    void saveProfileConfiguration();

    void setSelectedProfile(Profile currentProfile);

    ScrambleList getScramblesList();

    void setScramblesList(ScrambleList scrambleList);
}
