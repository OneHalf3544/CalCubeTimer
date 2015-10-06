package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * <p>
 * <p>
 * Created: 17.01.2015 12:48
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class CalCubeTimerModelImpl implements CalCubeTimerModel {

    private static final Logger LOG = LogManager.getLogger(CalCubeTimerModelImpl.class);

    private /*final */ CalCubeTimerGui calCubeTimerGui;
    private final CurrentSessionSolutionsTableModel statsModel; //used in ProfileDatabase
    private final Configuration configuration;

    private InspectionState previousInpectionState = new InspectionState(Instant.now(), Instant.now());

    @Inject
    private ScrambleList scramblesList;

    @Inject
    private StackmatInterpreter stackmatInterpreter;

    @Inject
    private TimingListener timingListener;

    private boolean timing = false;
    private final ProfileDao profileDao;

    final NumberSpeaker numberSpeaker;
    ActionMap actionMap;
    Metronome metronome;

    boolean customizationEditsDisabled = false;

    SolveType penalty = null;

    Timer updateInspectionTimer;
    private long lastSplit;

    private LocaleAndIcon loadedLocale;

    private StackmatState lastAccepted = new StackmatState(null, Collections.emptyList());

    private boolean fullscreen = false;
    private Instant inspectionStart = null;
    private Profile currentProfile;
    @Inject
    private CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

    @Inject
    public CalCubeTimerModelImpl(CalCubeTimerGui calCubeTimerGui, Configuration configuration, ProfileDao profileDao,
                                 CurrentSessionSolutionsTableModel statsModel1, NumberSpeaker numberSpeaker,
                                 ActionMap actionMap, CctModelConfigChangeListener cctModelConfigChangeListener) {
        this.calCubeTimerGui = calCubeTimerGui;
        statsModel = statsModel1;
        this.configuration = configuration;
        this.profileDao = profileDao;
        this.numberSpeaker = numberSpeaker;
        this.actionMap = initializeActionMap(actionMap);
        configuration.addConfigurationChangeListener(cctModelConfigChangeListener);
        LOG.debug("model created");
    }

    @Inject
    void initialize() {
        updateInspectionTimer = new Timer(90, e -> calCubeTimerGui.updateInspection());
        metronome = Metronome.createTickTockTimer(Duration.ofSeconds(1));
        LOG.debug("model initialized");
    }

    @Override
    public void prepareForProfileSwitch() {
        Profile profile = getSelectedProfile();
        profileDao.saveDatabase(profile);
        profileDao.saveProfile(profile);
        calCubeTimerGui.saveToConfiguration();
        configuration.saveConfiguration(profile);
    }

    @Override
    public boolean isTiming() {
        return timing;
    }

    @Override
    public void setTiming(boolean timing) {
        this.timing = timing;
    }


    private ActionMap initializeActionMap(ActionMap actionMap) {
        statsModel.setUndoRedoListener(new UndoRedoListener() {
            private int undoable;
            private int redoable;

            @Override
            public void undoRedoChange(int undoable, int redoable) {
                this.undoable = undoable;
                this.redoable = redoable;
                refresh();
            }

            @Override
            public void refresh() {
                actionMap.getAction("undo", calCubeTimerGui.getMainFrame()).setEnabled(undoable != 0);
                actionMap.getAction("redo", calCubeTimerGui.getMainFrame()).setEnabled(redoable != 0);
                actionMap.getAction("undo", calCubeTimerGui.getMainFrame()).putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.undo") + undoable);
                actionMap.getAction("redo", calCubeTimerGui.getMainFrame()).putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.redo") + redoable);
            }
        });
        return actionMap;
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        LOG.trace("toggle fullscreen. was {}, new: {}", this.fullscreen, fullscreen);
        this.fullscreen = fullscreen;
    }

    @Override
    public ScrambleList getScramblesList() {
        return scramblesList;
    }

    @Override
    public CurrentSessionSolutionsTableModel getStatsModel() {
        return statsModel;
    }

    //if we deleted the current session, should we create a new one, or load the "nearest" session?
    @Override
    public Session getNextSession(CALCubeTimerFrame calCubeTimerFrame) {
        Session nextSession = getStatsModel().getCurrentSession();
        PuzzleType customization = scramblesList.getCurrentScrambleCustomization();
        SessionsListTableModel puzzleDatabase = getSelectedProfile().getSessionsDatabase();
        SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics = puzzleDatabase.getPuzzleStatisticsForType(customization);
        if (!sessionsListAndPuzzleStatistics.containsSession(nextSession)) {
            //failed to find a session to continue, so load newest session
            Optional<Session> nextSessionOptional = puzzleDatabase.getSessions().stream()
                    .max(Comparator.comparing(session -> session.getStatistics().getStartTime()));

            if (nextSessionOptional.isPresent()) {
                nextSession = nextSessionOptional.get();
            } else {
                nextSession = new Session(LocalDateTime.now(), configuration, customization);
                currentSessionSolutionsTableModel.setCurrentSession(getSelectedProfile(), nextSession);
            }
        }
        return nextSession;
    }

    @Override
    public void setSelectedProfile(Profile currentProfile) {
        this.currentProfile = currentProfile;
    }

    @Override
    public Profile getSelectedProfile() {
        return currentProfile;
    }

    @Override
    public void sessionSelected(Session session) {
        statsModel.setCurrentSession(getSelectedProfile(), session);
        scramblesList.clear();
        Statistics stats = session.getStatistics();
        for (int ch = 0; ch < stats.getAttemptCount(); ch++) {
            scramblesList.addScramble(stats.get(ch).getScrambleString());
        }
        scramblesList.setScrambleNumber(scramblesList.size() + 1);

        customizationEditsDisabled = true;
        calCubeTimerGui.getScrambleCustomizationComboBox().setSelectedItem(session.getCustomization()); //this will update the scramble
        customizationEditsDisabled = false;
    }

    @Override
    public void sessionsDeleted() {
        Session session = getNextSession(calCubeTimerGui.getMainFrame());
        statsModel.setCurrentSession(getSelectedProfile(), session);
        calCubeTimerGui.getScrambleCustomizationComboBox().setSelectedItem(session.getCustomization());
    }

    @Override
    public TimingListener getTimingListener() {
        return timingListener;
    }

    @Override
    public long getLastSplit() {
        return lastSplit;
    }

    @Override
    public StackmatInterpreter getStackmatInterpreter() {
        return stackmatInterpreter;
    }

    @Override
    public void setLastSplit(long lastSplit) {
        this.lastSplit = lastSplit;
    }

    @Override
    public void startMetronome() {
        metronome.setDelay(configuration.getInt(VariableKey.METRONOME_DELAY));
        metronome.startMetronome();
    }
    @Override
    public void stopMetronome() {
        metronome.stopMetronome();
    }

    private List<SolveTime> splits = new ArrayList<>();

    @Override
    public void addTime(TimerState addMe) {
        Solution protect = addMe.toSolution(scramblesList.getCurrent(), splits);
        if(penalty == null) {
            protect.getTime().clearType();
        }
        else {
            protect.getTime().setTypes(Collections.singletonList(penalty));
        }
        penalty = null;
        splits = new ArrayList<>();
        boolean sameAsLast = addMe.compareTo(lastAccepted) == 0;
        if(sameAsLast) {
            int choice = Utils.showYesNoDialog(calCubeTimerGui.getMainFrame(), addMe.toString() + "\n" + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
            if(choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (promptNewTime(protect, sameAsLast)) {
            statsModel.getCurrentSession().getStatistics().add(protect);
        }
    }

    private boolean promptNewTime(Solution protect, boolean sameAsLast) {
        int choice = JOptionPane.YES_OPTION;
        if(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME) && !sameAsLast) {
            String[] OPTIONS = { StringAccessor.getString("CALCubeTimer.accept"), SolveType.PLUS_TWO.toString(), SolveType.DNF.toString() };
            //This leaves +2 and DNF enabled, even if the user just got a +2 or DNF from inspection.
            //I don't really care however, since I doubt that anyone even uses this feature. --Jeremy
            choice = JOptionPane.showOptionDialog(null,
                    StringAccessor.getString("CALCubeTimer.yourtime") + protect.getTime().toString() + StringAccessor.getString("CALCubeTimer.newtimedialog"),
                    StringAccessor.getString("CALCubeTimer.confirm"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    OPTIONS,
                    OPTIONS[0]);
        }

        switch (choice) {
            case JOptionPane.YES_OPTION:
                return true;
            case JOptionPane.NO_OPTION:
                protect.getTime().setTypes(Collections.singletonList(SolveType.PLUS_TWO));
                return true;
            case JOptionPane.CANCEL_OPTION:
                protect.getTime().setTypes(Collections.singletonList(SolveType.DNF));
                return true;
            default:
                return false;
        }
    }

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    @Override
    public InspectionState getInspectionValue() {
        InspectionState inspectionState = new InspectionState(inspectionStart, Instant.now());

        if(configuration.getBoolean(VariableKey.SPEAK_INSPECTION) && !Objects.equals(inspectionState, previousInpectionState)) {
            previousInpectionState = inspectionState;
            if(inspectionState.getElapsedTime().getSeconds() == InspectionState.FIRST_WARNING.getSeconds()) {
                numberSpeaker.sayInspectionWarning(InspectionState.FIRST_WARNING);
            } else if(inspectionState.getElapsedTime().getSeconds() == InspectionState.FINAL_WARNING.getSeconds()) {
                numberSpeaker.sayInspectionWarning(InspectionState.FINAL_WARNING);
            }
        }
        return inspectionState;
    }

    @Override
    public void stopInspection() {
        inspectionStart = null;
        updateInspectionTimer.stop();
    }

    @Override
    public boolean isInspecting() {
        return inspectionStart != null;
    }

    @Override
    public List<SolveTime> getSplits() {
        return splits;
    }

    @Override
    public void setPenalty(SolveType penalty) {
        this.penalty = penalty;
    }

    @Override
    public Metronome getMetronome() {
        return metronome;
    }

    @Override
    public NumberSpeaker getNumberSpeaker() {
        return numberSpeaker;
    }

    @Override
    public LocaleAndIcon getLoadedLocale() {
        return loadedLocale;
    }

    @Override
    public void setLoadedLocale(LocaleAndIcon newLocale) {
        this.loadedLocale = newLocale;
    }

    @Override
    public boolean getCustomizationEditsDisabled() {
        return customizationEditsDisabled;
    }

    @Override
    public void setCustomizationEditsDisabled(boolean b) {
        this.customizationEditsDisabled = b;
    }

    @Override
    public void setInspectionStart(Instant inspectionStart) {
        this.inspectionStart = inspectionStart;
    }

    @Override
    public void startUpdateInspectionTimer() {
        updateInspectionTimer.start();
    }
}
