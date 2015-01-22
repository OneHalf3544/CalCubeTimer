package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.statistics.Statistics;
import net.gnehzr.cct.statistics.StatisticsTableModel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
* <p>
* <p>
* Created: 09.12.2014 22:42
* <p>
*
* @author OneHalf
*/
@Singleton
public class ActionMap {

    private static final Logger LOG = Logger.getLogger(ActionMap.class);

    public static final String NEWSESSION_ACTION = "newsession";
    public static final String CONNECT_TO_SERVER_ACTION = "connecttoserver";
    public static final String TOGGLE_SCRAMBLE_POPUP_ACTION = "togglescramblepopup";

    private Map<String, AbstractAction> actionMap;

    @Inject
	private Configuration configuration;
    @Inject
    private StatisticsTableModel statsModel;

    @Inject
	public ActionMap() {
        this.actionMap = new HashMap<>();
    }

    void registerAction(String actionName, AbstractAction abstractAction) {
        LOG.debug("register action " + actionName);
        actionMap.put(actionName, abstractAction);
    }

    public AbstractAction getAction(String s, CALCubeTimerFrame calCubeTimerFrame, CalCubeTimerModel calCubeTimerModel) {
        return actionMap.computeIfAbsent(s.toLowerCase(), (k) -> initializeAction(k, calCubeTimerFrame, calCubeTimerModel));
    }

    public Optional<AbstractAction> getActionIfExist(String s){
        return Optional.ofNullable(actionMap.get(s.toLowerCase()));
    }

    private AbstractAction initializeAction(String s, final CALCubeTimerFrame calCubeTimerFrame, CalCubeTimerModel cubeTimerModel){
        switch (s) {
            case "keyboardtiming": {
                AbstractAction a = new KeyboardTimingAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
                a.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
                return a;
            }
            case "addtime": {
                AbstractAction a = new AddTimeAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
                return a;
            }
            case "reset": {
                AbstractAction a = new ResetAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
                return a;
            }
            case "currentaverage0":
                return new StatisticsAction(calCubeTimerFrame, statsModel, Statistics.AverageType.CURRENT, 0, configuration);
            case "bestaverage0":
                return new StatisticsAction(calCubeTimerFrame, statsModel, Statistics.AverageType.RA, 0, configuration);
            case "currentaverage1":
                return new StatisticsAction(calCubeTimerFrame, statsModel, Statistics.AverageType.CURRENT, 1, configuration);
            case "bestaverage1":
                return new StatisticsAction(calCubeTimerFrame, statsModel, Statistics.AverageType.RA, 1, configuration);
            case "sessionaverage":
                return new StatisticsAction(calCubeTimerFrame, statsModel, Statistics.AverageType.SESSION, 0, configuration);
            case "togglefullscreen":
                return new AbstractAction() {
                    {
                        putValue(Action.NAME, "+");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        calCubeTimerFrame.setFullScreen(!cubeTimerModel.isFullscreen());
                    }
                };
            case "showconfiguration": {
                AbstractAction a = new ShowConfigurationDialogAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
                return a;
            }
            case "exit": {
                AbstractAction a = new ExitAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
                return a;
            }
            case "togglestatuslight": {
                AbstractAction a = new StatusLightAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
                return a;
            }
            case "togglehidescrambles": {
                AbstractAction a = new HideScramblesAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
                return a;
            }
            case "togglespacebarstartstimer": {
                AbstractAction a = new SpacebarOptionAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
                return a;
            }
            case "togglefullscreentiming": {
                AbstractAction a = new FullScreenTimingAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
                return a;
            }
            case "undo":
                return new AbstractAction() {
                    {
                        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (calCubeTimerFrame.timesTable.isEditing())
                            return;
                        if (statsModel.getCurrentStatistics().undo()) { //should decrement 1 from scramblenumber if possible
                            Object prev = calCubeTimerFrame.scrambleNumber.getPreviousValue();
                            if (prev != null) {
                                calCubeTimerFrame.scrambleNumber.setValue(prev);
                            }
                        }
                    }
                };
            case "redo":
                return new AbstractAction() {
                    {
                        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (calCubeTimerFrame.timesTable.isEditing())
                            return;
                        statsModel.getCurrentStatistics().redo();
                    }
                };
            case "submitsundaycontest":
                final SundayContestDialog submitter = new SundayContestDialog(calCubeTimerFrame, configuration);
                return new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        submitter.syncWithStats(statsModel.getCurrentStatistics(), Statistics.AverageType.CURRENT, 0);
                        submitter.setVisible(true);
                    }
                };
            case "showdocumentation":
                return new DocumentationAction(calCubeTimerFrame);
            case "showabout":
                return new AboutAction();
            case "requestscramble": {
                AbstractAction a = new RequestScrambleAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
                return a;
            }
            default:
                throw new IllegalArgumentException("unknown action: " + s);
        }
    }

    void refreshActions(){
        getActionIfExist("keyboardtiming")
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, !configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)));
        getActionIfExist("togglestatuslight")
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY, false)));
        getActionIfExist("togglehidescrambles")
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.HIDE_SCRAMBLES, false)));
        getActionIfExist("togglespacebarstartstimer")
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false)));
        getActionIfExist("togglefullscreen")
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false)));
    }

}
