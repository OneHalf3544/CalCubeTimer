package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.GeneratedScrambleList;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private final Configuration configuration;

    private InspectionState previousInpectionState = new InspectionState(Instant.now(), Instant.now());

    private ScrambleList scramblesList;

    @Inject
    private StackmatInterpreter stackmatInterpreter;

    @Inject
    private TimingListener timingListener;

    @Inject
    private ScramblePluginManager scramblePluginManager;

    @Inject
    private SolutionDao solutionDao;

    private boolean timing = false;
    private final ProfileDao profileDao;

    final NumberSpeaker numberSpeaker;

    Metronome metronome;

    SolveType penalty = null;

    Timer updateInspectionTimer;
    private long lastSplit;

    private LocaleAndIcon loadedLocale;

    private StackmatState lastAccepted = new StackmatState(null, Collections.emptyList());

    private boolean fullscreen = false;
    private Instant inspectionStart = null;
    private Profile currentProfile;
    @Inject
    private SessionsList sessionsList;

    SessionListener sessionListener = new SessionListener() {
        @Override
        public void sessionSelected(Session session) {
            scramblesList.asGenerating().generateScrambleForCurrentSession();
            calCubeTimerGui.getPuzzleTypeComboBox().setSelectedItem(session.getPuzzleType()); //this will update the scramble
        }

        @Override
        public void sessionsDeleted() {
            Session session = sessionsList.getCurrentSession();
            sessionsList.removeSession(session);
            calCubeTimerGui.getPuzzleTypeComboBox().setSelectedItem(session.getPuzzleType());
        }
    };

    @Inject
    public CalCubeTimerModelImpl(CalCubeTimerGui calCubeTimerGui, Configuration configuration, ProfileDao profileDao,
                                 NumberSpeaker numberSpeaker, CctModelConfigChangeListener cctModelConfigChangeListener) {
        this.calCubeTimerGui = calCubeTimerGui;
        this.configuration = configuration;
        this.profileDao = profileDao;
        this.numberSpeaker = numberSpeaker;
        configuration.addConfigurationChangeListener(cctModelConfigChangeListener);
        LOG.debug("model created");
    }

    @Inject
    void initialize() {
        scramblesList = new GeneratedScrambleList(sessionsList, configuration);
        sessionsList.addSessionListener(sessionListener);
        updateInspectionTimer = new Timer(90, e -> calCubeTimerGui.updateInspection());
        metronome = Metronome.createTickTockTimer(Duration.ofSeconds(1));
        LOG.debug("model initialized");
    }

    @Override
    public void saveProfileConfiguration() {
        LOG.info("save profile configuration");
        Profile profile = getSelectedProfile();
        profileDao.saveLastSession(profile, sessionsList);
        calCubeTimerGui.saveToConfiguration();
        configuration.saveConfiguration(profile);
    }

    @Override
    public boolean isTiming() {
        return timing;
    }

    @Override
    public void setTiming(boolean timing) {
        if (timing) {
            Objects.requireNonNull(scramblesList.getCurrentScramble());
        }
        this.timing = timing;
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

    //if we deleted the current session, should we create a new one, or load the "nearest" session?
    @Override
    @Deprecated
    public Session getCurrentSession() {
        LOG.debug("getNextSession");
        return sessionsList.getCurrentSession();
    }

    @Override
    public void setSelectedProfile(Profile newCurrentProfile) {
        if (this.currentProfile != null) {
            saveProfileConfiguration();
        }

        LOG.info("setSelectedProfile: {}", newCurrentProfile);

        this.currentProfile = newCurrentProfile;
        configuration.loadConfiguration(newCurrentProfile);
        configuration.apply(newCurrentProfile);

        sessionsList.setSessions(solutionDao.loadSessions(newCurrentProfile, scramblePluginManager));
    }

    @Override
    public Profile getSelectedProfile() {
        return currentProfile;
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
    public void addTime(TimerState timerState) {
        Solution solution = timerState.toSolution(Objects.requireNonNull(scramblesList.getCurrentScramble()), splits);
        if(penalty == null) {
            solution.getTime().clearType();
        }
        else {
            solution.getTime().setTypes(Collections.singletonList(penalty));
        }
        penalty = null;
        splits = new ArrayList<>();
        boolean sameAsLast = timerState.compareTo(lastAccepted) == 0;
        if(sameAsLast) {
            int choice = Utils.showYesNoDialog(calCubeTimerGui.getMainFrame(),
                    timerState.toString() + "\n"
                            + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
            if(choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (promptNewTime(solution, sameAsLast)) {
            timingListener.timerAccepted(solution);
        }
        getScramblesList().generateNext();
        calCubeTimerGui.updateScramble();
    }

    private boolean promptNewTime(Solution protect, boolean sameAsLast) {
        int choice = JOptionPane.YES_OPTION;
        if(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME) && !sameAsLast) {
            String[] OPTIONS = { StringAccessor.getString("CALCubeTimer.accept"), SolveType.PLUS_TWO.toString(), SolveType.DNF.toString() };
            //This leaves +2 and DNF enabled, even if the user just got a +2 or DNF from inspection.
            //I don't really care however, since I doubt that anyone even uses this feature. --Jeremy
            choice = JOptionPane.showOptionDialog(null,
                    StringAccessor.getString("CALCubeTimer.yourtime")
                            + protect.getTime().toString()
                            + StringAccessor.getString("CALCubeTimer.newtimedialog"),
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
    public void setInspectionStart(Instant inspectionStart) {
        Objects.requireNonNull(scramblesList.getCurrentScramble());
        this.inspectionStart = inspectionStart;
    }

    @Override
    public void startUpdateInspectionTimer() {
        updateInspectionTimer.start();
    }

    @Override
    public void setScramblesList(@NotNull ScrambleList scrambleList) {
        this.scramblesList = scrambleList;
    }
}
