package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleListHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.gnehzr.cct.statistics.RollingAverageOf.OF_12;
import static net.gnehzr.cct.statistics.RollingAverageOf.OF_5;
import static net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType.BEST_ROLLING_AVERAGE;
import static net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType.CURRENT_ROLLING_AVERAGE;

/**
* <p>
* <p>
* Created: 09.12.2014 22:42
* <p>
*
* @author OneHalf
*/
@Service
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
    public static final String SHOW_ABOUT_ACTION = "showabout";
    public static final String SHOW_CONFIGURATION_ACTION = "showconfiguration";

    private final Map<String, AbstractAction> actionMap;

    @Autowired
	private Configuration configuration;
    @Autowired
    private SessionsList sessionsList;
    @Autowired
    private ScrambleListHolder scrambleListHolder;

	public ActionMap() {
        this.actionMap = new HashMap<>();
    }

    void registerAction(String actionName, AbstractAction abstractAction) {
        LOG.debug("register action {}", actionName);
        actionMap.put(actionName.toLowerCase(), abstractAction);
    }

    public AbstractAction getAction(String s, CALCubeTimerFrame calCubeTimerFrame) {
        return actionMap.computeIfAbsent(s.toLowerCase(), (k) -> initializeAction(k, calCubeTimerFrame));
    }

    public Optional<AbstractAction> getActionIfExist(String s){
        return Optional.ofNullable(actionMap.get(s.toLowerCase()));
    }

    private AbstractAction initializeAction(String actionName, final CALCubeTimerFrame calCubeTimerFrame) {
        LOG.debug("register action {}", actionName);
        switch (actionName) {
            case KEYBOARDTIMING_ACTION: {
                return new KeyboardTimingAction(calCubeTimerFrame, configuration);
            }
            case RESET_ACTION: {
                return new ResetAction(calCubeTimerFrame, sessionsList);
            }
            case "currentaverage0":
                return new ShowDetailedStatisticsAction(
                        calCubeTimerFrame, CURRENT_ROLLING_AVERAGE, OF_5, configuration, sessionsList);
            case "bestaverage0":
                return new ShowDetailedStatisticsAction(
                        calCubeTimerFrame, BEST_ROLLING_AVERAGE, OF_5, configuration, sessionsList);
            case "currentaverage1":
                return new ShowDetailedStatisticsAction(
                        calCubeTimerFrame, CURRENT_ROLLING_AVERAGE, OF_12, configuration, sessionsList);
            case "bestaverage1":
                return new ShowDetailedStatisticsAction(
                        calCubeTimerFrame, BEST_ROLLING_AVERAGE, OF_12, configuration, sessionsList);
            case "sessionaverage":
                return new ShowDetailedStatisticsAction(
                        calCubeTimerFrame, AverageType.SESSION_AVERAGE, null, configuration, sessionsList);

            case SHOW_CONFIGURATION_ACTION: {
                return new ShowConfigurationDialogAction(calCubeTimerFrame);
            }
            case "exit": {
                return new ExitAction(calCubeTimerFrame);
            }
            case TOGGLE_STATUS_LIGHT_ACTOIN: {
                return new ToggleStatusLightAction(calCubeTimerFrame, configuration);
            }
            case TOGGLE_HIDE_SCRAMBLES: {
                return new HideScramblesAction(calCubeTimerFrame, configuration, this);
            }
            case TOGGLE_SPACEBAR_STARTS_TIMER_ACTION: {
                return new SpacebarOptionAction(configuration);
            }
            case SHOW_DOCUMENTATION_ACTION:
                return new DocumentationAction(calCubeTimerFrame, configuration);

            case SHOW_ABOUT_ACTION:
                return new AboutAction();
            case "requestscramble": {
                return new RequestScrambleAction(calCubeTimerFrame, scrambleListHolder);
            }
            default:
                throw new IllegalArgumentException("unknown action: " + actionName);
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

    @Service
    static class ToggleFullscreenTimingAction extends AbstractAction {

        public static final String TOGGLE_FULLSCREEN = "togglefullscreen";

        private final CALCubeTimerFrame calCubeTimerFrame;

        @Autowired
        public ToggleFullscreenTimingAction(CALCubeTimerFrame calCubeTimerFrame) {
            super("+");
            this.calCubeTimerFrame = calCubeTimerFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            calCubeTimerFrame.setFullscreen(!calCubeTimerFrame.isFullscreen());
        }

        @Autowired
        public void registerAction(ActionMap actionMap) {
            actionMap.registerAction(ToggleFullscreenTimingAction.TOGGLE_FULLSCREEN, this);
        }
    }
}
