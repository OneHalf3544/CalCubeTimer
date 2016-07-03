package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.statistics.RollingAverageOf;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics;
import net.gnehzr.cct.statistics.SessionsList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
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
@Service
public class ActionMap {

    private static final Logger LOG = LogManager.getLogger(ActionMap.class);

    public static final String KEYBOARDTIMING_ACTION = "keyboardtiming";
    public static final String TOGGLE_STATUS_LIGHT_ACTOIN = "togglestatuslight";
    public static final String TOGGLE_HIDE_SCRAMBLES = "togglehidescrambles";
    public static final String TOGGLE_SPACEBAR_STARTS_TIMER_ACTION = "togglespacebarstartstimer";

    private final Map<String, AbstractAction> actionMap;

    @Autowired
	private Configuration configuration;
    @Autowired
    private SessionsList sessionsList;

    public ActionMap() {
        this.actionMap = new HashMap<>();
    }

    @Autowired
    void setActions(AbstractNamedAction[] abstractActions) {
        for (AbstractNamedAction abstractNamedAction : abstractActions) {
            registerAction(abstractNamedAction.getActionCode(), abstractNamedAction);
        }
    }

    void registerAction(String actionName, AbstractAction abstractAction) {
        LOG.debug("register action {}", actionName);
        actionMap.put(actionName.toLowerCase(), abstractAction);
    }

    public AbstractAction getAction(String s) {
        return actionMap.get(s.toLowerCase());
    }

    public Optional<AbstractAction> getActionIfExist(String s){
        return Optional.ofNullable(actionMap.get(s.toLowerCase()));
    }

    /**
     * Обновыление статистики
     */
    public void updateStatisticActionsStatuses() {
        SessionSolutionsStatistics stats = sessionsList.getCurrentSession().getStatistics();

        updateActionStatus(stats, "currentaverage0", SessionSolutionsStatistics.AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_5);
        updateActionStatus(stats, "currentaverage1", SessionSolutionsStatistics.AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_12);
        updateActionStatus(stats, "bestaverage0", SessionSolutionsStatistics.AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_5);
        updateActionStatus(stats, "bestaverage1", SessionSolutionsStatistics.AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_12);
        updateActionStatus(stats, "sessionaverage", SessionSolutionsStatistics.AverageType.SESSION_AVERAGE, null);
    }

    private void updateActionStatus(SessionSolutionsStatistics sessionStatistics, String actionName,
                                    SessionSolutionsStatistics.AverageType statType, RollingAverageOf i) {
        getActionIfExist(actionName).ifPresent(action -> action.setEnabled(sessionStatistics.isValid(statType, i)));
    }


    public void refreshActions(){
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

}
