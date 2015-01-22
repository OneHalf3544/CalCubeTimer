package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.stackmatInterpreter.TimerState;

import javax.swing.*;

/**
 * <p>
 * <p>
 * Created: 18.01.2015 14:58
 * <p>
 *
 * @author OneHalf
 */
public interface CalCubeTimerGui {

    void updateScramble();

    CALCubeTimerFrame getMainFrame();

    void updateInspection();

    ScrambleChooserComboBox<ScrambleCustomization> getScrambleCustomizationComboBox();

    void loadXMLGUI();

    void loadStringsFromDefaultLocale();

    void repaintTimes();

    void createScrambleAttributesPanel();

    void updateGeneratorField(boolean generatorEnabled, String generator);

    JSpinner getScrambleNumber();

    JSpinner getScrambleLength();

    JComboBox<LocaleAndIcon> getLanguages();

    void saveToConfiguration();

    void setWaiting(boolean wait);

    void addSplit(TimerState newSplit);

    void setFullScreen(boolean b);

    JLabel getOnLabel();
}
