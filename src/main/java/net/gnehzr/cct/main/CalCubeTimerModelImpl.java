package net.gnehzr.cct.main;

import com.google.inject.Inject;
import javazoom.jl.decoder.JavaLayerException;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.umts.ircclient.IRCClient;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.inject.Singleton;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.xml.transform.TransformerConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Logger LOG = Logger.getLogger(CalCubeTimerModelImpl.class);

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
    private IRCClient ircClient;

    @Inject
    private TimingListener timingListener;

    private boolean timing = false;
    private final ProfileDao profileDao;

    final NumberSpeaker numberSpeaker;
    ActionMap actionMap;
    Timer tickTock;

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
        tickTock = createTickTockTimer();
    }

    @Override
    public void prepareForProfileSwitch() {
        Profile profile = profileDao.getSelectedProfile();
        try {
            profileDao.saveDatabase(profile);
        } catch (TransformerConfigurationException | IOException | SAXException e1) {
            LOG.info("unexpected exception", e1);
        }
        calCubeTimerGui.saveToConfiguration();
        try {
            configuration.saveConfigurationToFile(profile);
        } catch (Exception e) {
            LOG.info("unexpected exception", e);
        }
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
                actionMap.getAction("undo", calCubeTimerGui.getMainFrame(), CalCubeTimerModelImpl.this).setEnabled(undoable != 0);
                actionMap.getAction("redo", calCubeTimerGui.getMainFrame(), CalCubeTimerModelImpl.this).setEnabled(redoable != 0);
                actionMap.getAction("undo", calCubeTimerGui.getMainFrame(), CalCubeTimerModelImpl.this).putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.undo") + undoable);
                actionMap.getAction("redo", calCubeTimerGui.getMainFrame(), CalCubeTimerModelImpl.this).putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.redo") + redoable);
            }
        });
        return actionMap;
    }

    private Timer createTickTockTimer() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(configuration.getString(VariableKey.METRONOME_CLICK_FILE, false));
            Objects.requireNonNull(inputStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
            DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
            Timer timer = new Timer(1000, arg0 -> {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
            });
            timer.setInitialDelay(0);
            return timer;

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
            throw new IllegalStateException(e1);
        }
    }

    @Override
    public boolean isFullscreen() {
        return fullscreen;
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
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
        String customization = scramblesList.getCurrentScrambleCustomization().toString();
        ProfileDatabase puzzleDatabase = p.getPuzzleDatabase();
        PuzzleStatistics ps = puzzleDatabase.getPuzzleStatistics(customization);
        if (!ps.containsSession(nextSesh)) {
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
            scramblesList.addScramble(stats.get(ch).getScramble());
        }
        scramblesList.setScrambleNumber(scramblesList.size() + 1);

        customizationEditsDisabled = true;
        calCubeTimerGui.getScrambleCustomizationComboBox().setSelectedItem(s.getCustomization()); //this will update the scramble
        customizationEditsDisabled = false;

        ircClient.sendUserstate();
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
        if (!configuration.getBoolean(VariableKey.SPEAK_TIMES, false)) {
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
        tickTock.setDelay(configuration.getInt(VariableKey.METRONOME_DELAY, false));
        tickTock.start();
    }
    @Override
    public void stopMetronome() {
        tickTock.stop();
    }

    private List<SolveTime> splits = new ArrayList<>();

    @Override
    public void addTime(TimerState addMe) {
        Solution protect = addMe.toSolution(null, splits);
        if(penalty == null) {
            protect.getTime().clearType();
        }
        else {
            protect.getTime().setTypes(Arrays.asList(penalty));
        }
        penalty = null;
        splits = new ArrayList<>();
        boolean sameAsLast = addMe.compareTo(lastAccepted) == 0;
        if(sameAsLast) {
            int choice = Utils.showYesNoDialog(calCubeTimerGui.getMainFrame(), addMe.toString() + "\n" + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
            if(choice != JOptionPane.YES_OPTION)
                return;
        }
        int choice = JOptionPane.YES_OPTION;
        if(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME, false) && !sameAsLast) {
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
                break;
            case JOptionPane.NO_OPTION:
                protect.getTime().setTypes(Arrays.asList(SolveType.PLUS_TWO));
                break;
            case JOptionPane.CANCEL_OPTION:
                protect.getTime().setTypes(Arrays.asList(SolveType.DNF));
                break;
            default:
                return;
        }
        statsModel.getCurrentStatistics().add(protect);
    }

    //this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
    @Override
    public long getInpectionValue() {
        long inspectionDone = Duration.between(inspectionStart, Instant.now()).getSeconds();
        if(inspectionDone != previousInpection && configuration.getBoolean(VariableKey.SPEAK_INSPECTION, false)) {
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
                numberSpeaker.getCurrentSpeaker().speak(false, seconds.getSeconds() * 100);
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
    public Timer getTickTock() {
        return tickTock;
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
