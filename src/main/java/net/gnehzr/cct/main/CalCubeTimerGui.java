package net.gnehzr.cct.main;

import net.gnehzr.cct.i18n.LocaleAndIcon;

import javax.swing.*;
import javax.swing.event.TableModelEvent;

/**
 * <p>
 * <p>
 * Created: 18.01.2015 14:58
 * <p>
 *
 * @author OneHalf
 */
public interface CalCubeTimerGui {

    String CCT_VERSION = CALCubeTimerFrame.class.getPackage().getImplementationVersion();
    ImageIcon CUBE_ICON = new ImageIcon(CALCubeTimerFrame.class.getResource("cube.png"));

    String SCRAMBLE_ATTRIBUTE_CHANGED = "Scramble Attribute Changed";

    void updateScramble();

    CALCubeTimerFrame getMainFrame();

    PuzzleTypeComboBox getPuzzleTypeComboBox();

    void loadXMLGUI();

    void newSolutionAdded(TableModelEvent event);

    void loadStringsFromDefaultLocale();

    void repaintTimes();

    void createScrambleAttributesPanel();

    void updateGeneratorField(boolean generatorEnabled, String generator);

    JSpinner getScrambleNumberSpinner();

    JSpinner getScrambleLengthSpinner();

    JComboBox<LocaleAndIcon> getLanguages();

    void saveToConfiguration();

    JLabel getOnLabel();

    void setVisible(boolean visible);

    boolean isFullscreen();

    void setFullscreen(boolean fullscreen);
}
