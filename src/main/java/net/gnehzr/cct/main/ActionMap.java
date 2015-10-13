package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.AverageType;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOG = LogManager.getLogger(ActionMap.class);

    public static final String NEWSESSION_ACTION = "newsession";
    public static final String TOGGLE_SCRAMBLE_POPUP_ACTION = "togglescramblepopup";
    public static final String KEYBOARDTIMING_ACTION = "keyboardtiming";
    public static final String TOGGLE_STATUS_LIGHT_ACTOIN = "togglestatuslight";
    public static final String TOGGLE_HIDE_SCRAMBLES = "togglehidescrambles";
    public static final String TOGGLE_SPACEBAR_STARTS_TIMER_ACTION = "togglespacebarstartstimer";
    public static final String ADD_TIME_ACTION = "addtime";
    public static final String RESET_ACTION = "reset";
    public static final String SHOW_DOCUMENTATION_ACTION = "showdocumentation";
    public static final String SUBMIT_SUNDAY_CONTEST_ACTION = "submitsundaycontest";
    public static final String SHOW_ABOUT_ACTION = "showabout";
    public static final String SHOW_CONFIGURATION_ACTION = "showconfiguration";

    private Map<String, AbstractAction> actionMap;

    @Inject
	private Configuration configuration;
    @Inject
    private CurrentSessionSolutionsTableModel statsModel;
    @Inject
    private ToggleFullscreenTimingAction toggleFullscreenTimingAction;

    @Inject
	public ActionMap() {
        this.actionMap = new HashMap<>();
    }

    void registerAction(String actionName, AbstractAction abstractAction) {
        LOG.debug("register action " + actionName);
        actionMap.put(actionName.toLowerCase(), abstractAction);
    }

    public AbstractAction getAction(String s, CALCubeTimerFrame calCubeTimerFrame) {
        return actionMap.computeIfAbsent(s.toLowerCase(), (k) -> initializeAction(k, calCubeTimerFrame));
    }

    public Optional<AbstractAction> getActionIfExist(String s){
        return Optional.ofNullable(actionMap.get(s.toLowerCase()));
    }

    private AbstractAction initializeAction(String s, final CALCubeTimerFrame calCubeTimerFrame){
        switch (s) {
            case KEYBOARDTIMING_ACTION: {
                return new KeyboardTimingAction(calCubeTimerFrame);
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
                return new StatisticsAction(
                        calCubeTimerFrame, statsModel, AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_5, configuration);
            case "bestaverage0":
                return new StatisticsAction(
                        calCubeTimerFrame, statsModel, AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_5, configuration);
            case "currentaverage1":
                return new StatisticsAction(
                        calCubeTimerFrame, statsModel, AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_12, configuration);
            case "bestaverage1":
                return new StatisticsAction(
                        calCubeTimerFrame, statsModel, AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_12, configuration);
            case "sessionaverage":
                return new StatisticsAction(
                        calCubeTimerFrame, statsModel, AverageType.SESSION_AVERAGE, null, configuration);
            case ToggleFullscreenTimingAction.TOGGLE_FULLSCREEN:
                // action to stop timing during fullscreen
                return toggleFullscreenTimingAction;

            case SHOW_CONFIGURATION_ACTION: {
                AbstractAction a = new ShowConfigurationDialogAction(calCubeTimerFrame);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
                return a;
            }
            case "exit": {
                return new ExitAction(calCubeTimerFrame);
            }
            case TOGGLE_STATUS_LIGHT_ACTOIN: {
                return new StatusLightAction(calCubeTimerFrame);
            }
            case TOGGLE_HIDE_SCRAMBLES: {
                return new HideScramblesAction(calCubeTimerFrame);
            }
            case TOGGLE_SPACEBAR_STARTS_TIMER_ACTION: {
                return new SpacebarOptionAction(configuration);
            }
            case SUBMIT_SUNDAY_CONTEST_ACTION:
                final SundayContestDialog submitter = new SundayContestDialog(calCubeTimerFrame, configuration);
                return new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        submitter.syncWithStats(statsModel.getCurrentSession().getSessionPuzzleStatistics(), AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_5);
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
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, !configuration.getBoolean(VariableKey.STACKMAT_ENABLED)));
        getActionIfExist(TOGGLE_STATUS_LIGHT_ACTOIN)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY)));
        getActionIfExist(TOGGLE_HIDE_SCRAMBLES)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.HIDE_SCRAMBLES)));
        getActionIfExist(TOGGLE_SPACEBAR_STARTS_TIMER_ACTION)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.SPACEBAR_ONLY)));
        getActionIfExist(FullScreenDuringTimingChangeSettingAction.TOGGLE_FULLSCREEN_TIMING_ACTION)
                .ifPresent(aA -> aA.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.FULLSCREEN_TIMING)));
    }

    @Singleton
    static class ToggleFullscreenTimingAction extends AbstractAction {

        public static final String TOGGLE_FULLSCREEN = "togglefullscreen";

        private final CalCubeTimerModel cubeTimerModel;
        private final CalCubeTimerGui calCubeTimerFrame;

        @Inject
        public ToggleFullscreenTimingAction(CalCubeTimerModel cubeTimerModel, CalCubeTimerGui calCubeTimerFrame) {
            super("+");
            this.cubeTimerModel = cubeTimerModel;
            this.calCubeTimerFrame = calCubeTimerFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            calCubeTimerFrame.setFullScreen(!cubeTimerModel.isFullscreen());
        }
    }
}
