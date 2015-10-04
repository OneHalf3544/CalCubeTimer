package net.gnehzr.cct.main;

import com.google.inject.Inject;
import javazoom.jl.decoder.JavaLayerException;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final Duration INSPECTION_TIME = Duration.ofSeconds(15);
    private static final Duration FIRST_WARNING = Duration.ofSeconds(8);
    private static final Duration FINAL_WARNING = Duration.ofSeconds(12);


    private final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    private /*final */ CalCubeTimerGui calCubeTimerGui;
    private final StatisticsTableModel statsModel; //used in ProfileDatabase
    private final Configuration configuration;

    private long previousInpection = -1;

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
    private boolean loading = false;

    SolveType penalty = null;
    Instant inspectionStart = null;
    Timer updateInspectionTimer;
    private long lastSplit;

    private LocaleAndIcon loadedLocale;

    @Inject
    private StackmatState lastAccepted;

    private boolean fullscreen = false;

    @Inject
    public CalCubeTimerModelImpl(CalCubeTimerGui calCubeTimerGui, Configuration configuration, ProfileDao profileDao,
                                 StatisticsTableModel statsModel1, NumberSpeaker numberSpeaker,
                                 ActionMap actionMap, CctModelConfigChangeListener cctModelConfigChangeListener) {
        this.calCubeTimerGui = calCubeTimerGui;
        statsModel = statsModel1;
        this.configuration = configuration;
        this.profileDao = profileDao;
        this.numberSpeaker = numberSpeaker;
        this.actionMap = initializeActionMap(actionMap);
        configuration.addConfigurationChangeListener(cctModelConfigChangeListener);
    }

    @Inject
    void initialize() {
        updateInspectionTimer = new Timer(90, e -> calCubeTimerGui.updateInspection());
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Inject
    void initializeModel() {
        metronome = Metronome.createTickTockTimer(Duration.ofSeconds(1));
    }

    @Override
    public void prepareForProfileSwitch() {
        Profile profile = profileDao.getSelectedProfile();
        profileDao.saveDatabase(profile);
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
    public StatisticsTableModel getStatsModel() {
        return statsModel;
    }

    //if we deleted the current session, should we create a new one, or load the "nearest" session?
    @Override
    public Session getNextSession(CALCubeTimerFrame calCubeTimerFrame) {
        Session nextSesh = getStatsModel().getCurrentSession();
        Profile p = profileDao.getSelectedProfile();
        ScrambleCustomization customization = scramblesList.getCurrentScrambleCustomization();
        SessionsTableModel puzzleDatabase = p.getSessionsDatabase();
        PuzzleStatistics puzzleStatistics = puzzleDatabase.getPuzzleStatistics(customization);
        if (!puzzleStatistics.containsSession(nextSesh)) {
            //failed to find a session to continue, so load newest session
            nextSesh = puzzleDatabase.getSessions().stream()
                    .max(Comparator.comparing(session -> session.getStatistics().getStartDate()))
                    .orElseGet(() -> calCubeTimerFrame.createNewSession(p, customization));
        }
        return nextSesh;
    }

    @Override
    public void sessionSelected(Session s) {
        statsModel.setSession(s);
        scramblesList.clear();
        Statistics stats = s.getStatistics();
        for (int ch = 0; ch < stats.getAttemptCount(); ch++) {
            scramblesList.addScramble(stats.get(ch).getScrambleString());
        }
        scramblesList.setScrambleNumber(scramblesList.size() + 1);

        customizationEditsDisabled = true;
        calCubeTimerGui.getScrambleCustomizationComboBox().setSelectedItem(s.getCustomization()); //this will update the scramble
        customizationEditsDisabled = false;
    }

    @Override
    public void sessionsDeleted() {
        Session s = getNextSession(calCubeTimerGui.getMainFrame());
        statsModel.setSession(s);
        calCubeTimerGui.getScrambleCustomizationComboBox().setSelectedItem(s.getCustomization());
    }

    @Override
    public TimingListener getTimingListener() {
        return timingListener;
    }

    @Override
    public void speakTime(SolveTime latestTime, CALCubeTimerFrame calCubeTimerFrame) {
        if (!configuration.getBoolean(VariableKey.SPEAK_TIMES)) {
            return;
        }
        threadPool.submit(() -> {
            try {
                numberSpeaker.getCurrentSpeaker().speak(latestTime);
            } catch (JavaLayerException e) {
                LOG.info("unexpected exception", e);
            }
        });
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
    public long getInpectionValue() {
        long inspectionDone = Duration.between(inspectionStart, Instant.now()).getSeconds();
        if(inspectionDone != previousInpection && configuration.getBoolean(VariableKey.SPEAK_INSPECTION)) {
            previousInpection = inspectionDone;
            if(inspectionDone == FIRST_WARNING.getSeconds()) {
                sayInspectionWarning(FIRST_WARNING);
            } else if(inspectionDone == FINAL_WARNING.getSeconds()) {
                sayInspectionWarning(FINAL_WARNING);
            }
        }
        return INSPECTION_TIME.getSeconds() - inspectionDone;
    }

    private void sayInspectionWarning(Duration seconds) {
        threadPool.submit(() -> {
            try {
                numberSpeaker.getCurrentSpeaker().speak(false, seconds);
            } catch (Exception e) {
                LOG.info(e);
            }
        });
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
