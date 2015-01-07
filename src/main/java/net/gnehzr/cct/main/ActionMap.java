package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.ProfileDao;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.Statistics;
import net.gnehzr.cct.umts.cctbot.CCTUser;
import net.gnehzr.cct.umts.ircclient.IRCClientGUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
* <p>
* <p>
* Created: 09.12.2014 22:42
* <p>
*
* @author OneHalf
*/
class ActionMap {

    private HashMap<String, AbstractAction> actionMap;
	@Inject
	private Configuration configuration;
	@Inject
	private CALCubeTimer calCubeTimer;
	@Inject
	private ProfileDao profileDao;
	@Inject
	private ScramblePlugin scramblePlugin;
	@Inject
	private IRCClientGUI client;

	@Inject
	public ActionMap(CALCubeTimer calCubeTimer){
        this.calCubeTimer = calCubeTimer;
        this.actionMap = new HashMap<>();
    }

    public void put(String s, AbstractAction a){
        actionMap.put(s.toLowerCase(), a);
    }

    public AbstractAction get(String s){
        s = s.toLowerCase();
        AbstractAction a = actionMap.get(s);
        if(a == null){
            a = initialize(s);
            actionMap.put(s, a);
        }
        return a;
    }

    public AbstractAction getRawAction(String s){
        return actionMap.get(s.toLowerCase());
    }

    private AbstractAction initialize(String s){
        AbstractAction a = null;
        switch (s) {
            case "keyboardtiming":
                a = new KeyboardTimingAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
                a.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
                break;
            case "addtime":
                a = new AddTimeAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
                break;
            case "reset":
                a = new ResetAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
                break;
            case "currentaverage0":
                a = new StatisticsAction(calCubeTimer, calCubeTimer.statsModel, Statistics.AverageType.CURRENT, 0, configuration);
                break;
            case "bestaverage0":
                a = new StatisticsAction(calCubeTimer, calCubeTimer.statsModel, Statistics.AverageType.RA, 0, configuration);
                break;
            case "currentaverage1":
                a = new StatisticsAction(calCubeTimer, calCubeTimer.statsModel, Statistics.AverageType.CURRENT, 1, configuration);
                break;
            case "bestaverage1":
                a = new StatisticsAction(calCubeTimer, calCubeTimer.statsModel, Statistics.AverageType.RA, 1, configuration);
                break;
            case "sessionaverage":
                a = new StatisticsAction(calCubeTimer, calCubeTimer.statsModel, Statistics.AverageType.SESSION, 0, configuration);
                break;
            case "togglefullscreen":
                a = new AbstractAction() {
                    {
                        putValue(Action.NAME, "+");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        calCubeTimer.setFullScreen(!calCubeTimer.isFullscreen);
                    }
                };
                break;
            case "importscrambles":
                a = new AbstractAction() {
                    {
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
                        putValue(Action.ACCELERATOR_KEY,
                                KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new ScrambleImportDialog(profileDao, calCubeTimer, calCubeTimer.scramblesList.getScrambleCustomization(), scramblePlugin, configuration);
                    }
                };
                break;
            case "exportscrambles":
                a = new ExportScramblesAction(calCubeTimer, profileDao);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
                break;
            case "connecttoserver":
                a = new AbstractAction() {
                    {
                        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
                        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (e == null) { //this means that the client gui was disposed
                            this.setEnabled(true);
                        } else {
                            if (calCubeTimer.client == null) {
                                calCubeTimer.client = new IRCClientGUI(calCubeTimer, this, scramblePlugin, configuration, profileDao);
                                syncUserStateNOW();
                            }
                            calCubeTimer.client.setVisible(true);
                            this.setEnabled(false);
                        }
                    }
                };
                break;
            case "showconfiguration":
                a = new ShowConfigurationDialogAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
                break;
            case "exit":
                a = new ExitAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
                a.putValue(Action.ACCELERATOR_KEY,
                        KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
                break;
            case "togglestatuslight":
                a = new StatusLightAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
                break;
            case "togglehidescrambles":
                a = new HideScramblesAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
                break;
            case "togglespacebarstartstimer":
                a = new SpacebarOptionAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
                break;
            case "togglefullscreentiming":
                a = new FullScreenTimingAction(configuration);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
                break;
            case "togglescramblepopup":
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, ((AbstractButton) e.getSource()).isSelected());
                        calCubeTimer.scramblePopup.refreshPopup();
                    }
                };
                break;
            case "undo":
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (calCubeTimer.timesTable.isEditing())
                            return;
                        if (calCubeTimer.statsModel.getCurrentStatistics().undo()) { //should decrement 1 from scramblenumber if possible
                            Object prev = calCubeTimer.scrambleNumber.getPreviousValue();
                            if (prev != null) {
                                calCubeTimer.scrambleNumber.setValue(prev);
                            }
                        }
                    }
                };
                a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
                break;
            case "redo":
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (calCubeTimer.timesTable.isEditing())
                            return;
                        calCubeTimer.statsModel.getCurrentStatistics().redo();
                    }
                };
                a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
                break;
            case "submitsundaycontest":
                final SundayContestDialog submitter = new SundayContestDialog(calCubeTimer, configuration);
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        submitter.syncWithStats(calCubeTimer.statsModel.getCurrentStatistics(), Statistics.AverageType.CURRENT, 0);
                        submitter.setVisible(true);
                    }
                };
                break;
            case "newsession":
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        if (calCubeTimer.statsModel.getRowCount() > 0) { //only create a new session if we've added any times to the current one
                            calCubeTimer.statsModel.setSession(calCubeTimer.createNewSession(profileDao.getSelectedProfile(), calCubeTimer.scramblesList.getScrambleCustomization().toString()));
                            calCubeTimer.timeLabel.reset();
                            calCubeTimer.scramblesList.clear();
                            calCubeTimer.updateScramble();
                        }
                    }
                };
                break;
            case "showdocumentation":
                a = new DocumentationAction(calCubeTimer);
                break;
            case "showabout":
                a = new AboutAction();
                break;
            case "requestscramble":
                a = new RequestScrambleAction(calCubeTimer);
                a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
                break;
        }
        return a;
    }


	//this will sync the cct state with the client, but will not transmit the data to other users
	void syncUserStateNOW() {
		CCTUser myself = client.getMyUserstate();
		myself.setCustomization(calCubeTimer.scrambleChooser.getSelectedItem().toString());

		myself.setLatestTime(calCubeTimer.statsModel.getCurrentStatistics().get(-1));

		TimerState state = calCubeTimer.timeLabel.getTimerState();
		if(!calCubeTimer.timing) {
			state = null;
		}
		myself.setTimingState(calCubeTimer.isInspecting(), state);

		Statistics stats = calCubeTimer.statsModel.getCurrentStatistics();
		myself.setCurrentRA(stats.average(Statistics.AverageType.CURRENT, 0), stats.toTerseString(Statistics.AverageType.CURRENT, 0, true));
		myself.setBestRA(stats.average(Statistics.AverageType.RA, 0), stats.toTerseString(Statistics.AverageType.RA, 0, false));
		myself.setSessionAverage(new SolveTime(stats.getSessionAvg(), null, configuration));

		myself.setSolvesAttempts(stats.getSolveCount(), stats.getAttemptCount());

		myself.setRASize(stats.getRASize(0));
	}

}
