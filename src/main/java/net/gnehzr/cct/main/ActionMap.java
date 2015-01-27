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
    public static final String KEYBOARDTIMING_ACTION = "keyboardtiming";
    public static final String TOGGLE_STATUS_LIGHT_ACTOIN = "togglestatuslight";
    public static final String TOGGLE_HIDE_SCRAMBLES = "togglehidescrambles";
    public static final String TOGGLE_SPACEBAR_STARTS_TIMER_ACTION = "togglespacebarstartstimer";
    public static final String TOGGLE_FULLSCREEN = "togglefullscreen";
    public static final String ADD_TIME_ACTION = "addtime";
    public static final String RESET_ACTION = "reset";
    public static final String SHOW_DOCUMENTATION_ACTION = "showdocumentation";
    public static final String SUBMIT_SUNDAY_CONTEST_ACTION = "submitsundaycontest";
    public static final String SHOW_ABOUT_ACTION = "showabout";
    public static final String UNDO_ACTION = "undo";
    public static final String REDO_ACTION = "redo";
    public static final String TOGGLE_FULLSCREEN_TIMING_ACTION = "togglefullscreentiming";
    public static final String SHOW_CONFIGURATION_ACTION = "showconfiguration";

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
            case KEYBOARDTIMING_ACTION: {
                AbstractAction a = new KeyboardTimingAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
                a.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
                return a;
            }
            case ADD_TIME_ACTION: {
                AbstractAction a = new AddTimeAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
                return a;
            }
            case RESET_ACTION: {
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
            case TOGGLE_FULLSCREEN:
                return new AbstractAction() {
                    {
                        putValue(Action.NAME, "+");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        calCubeTimerFrame.setFullScreen(!cubeTimerModel.isFullscreen());
                    }
                };
            case SHOW_CONFIGURATION_ACTION: {
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
            case TOGGLE_STATUS_LIGHT_ACTOIN: {
                AbstractAction a = new StatusLightAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
                return a;
            }
            case TOGGLE_HIDE_SCRAMBLES: {
                AbstractAction a = new HideScramblesAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
                return a;
            }
            case TOGGLE_SPACEBAR_STARTS_TIMER_ACTION: {
                AbstractAction a = new SpacebarOptionAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
                return a;
            }
            case TOGGLE_FULLSCREEN_TIMING_ACTION: {
                AbstractAction a = new FullScreenTimingAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
                return a;
            }
            case UNDO_ACTION:
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
            case REDO_ACTION:
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
            case SUBMIT_SUNDAY_CONTEST_ACTION:
                final SundayContestDialog submitter = new SundayContestDialog(calCubeTimerFrame, configuration);
                return new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        submitter.syncWithStats(statsModel.getCurrentStatistics(), Statistics.AverageType.CURRENT, 0);
                        submitter.setVisible(true);
                    }
                };
            case SHOW_DOCUMENTATION_ACTION:
                return new DocumentationAction(calCubeTimerFrame);
            case SHOW_ABOUT_ACTION:
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
        getActionIfExist(KEYBOARDTIMING_ACTION)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, !configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)));
        getActionIfExist(TOGGLE_STATUS_LIGHT_ACTOIN)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY, false)));
        getActionIfExist(TOGGLE_HIDE_SCRAMBLES)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.HIDE_SCRAMBLES, false)));
        getActionIfExist(TOGGLE_SPACEBAR_STARTS_TIMER_ACTION)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false)));
        getActionIfExist(TOGGLE_FULLSCREEN)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false)));
    }

}
