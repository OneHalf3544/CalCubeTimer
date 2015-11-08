package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.GeneratedScrambleList;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

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

    private final CalCubeTimerGui calCubeTimerGui;
    private final Configuration configuration;


    private ScrambleList scramblesList;

    @Inject
    private StackmatInterpreter stackmatInterpreter;

    @Inject
    private SolutionDao solutionDao;

    private final ProfileDao profileDao;

    private LocaleAndIcon loadedLocale;
    private Profile currentProfile;

    @Inject
    private SolvingProcess solvingProcess;

    @Inject
    private SessionsList sessionsList;

    SessionListener sessionListener = new SessionListener() {
        @Override
        public void sessionSelected(Session session) {
            if (scramblesList.isImported()) {
                setScramblesList(new GeneratedScrambleList(sessionsList, configuration));
            }
            scramblesList.asGenerating().generateScrambleForCurrentSession();
            calCubeTimerGui.getPuzzleTypeComboBox().setSelectedItem(session.getPuzzleType()); //this will update the scramble
        }

        @Override
        public void sessionsDeleted() {
            PuzzleType currentPuzzleType = sessionsList.getCurrentSession().getPuzzleType();
            calCubeTimerGui.getPuzzleTypeComboBox().setSelectedItem(currentPuzzleType);
        }
    };

    @Inject
    public CalCubeTimerModelImpl(CalCubeTimerGui calCubeTimerGui, Configuration configuration, ProfileDao profileDao,
                                 CctModelConfigChangeListener cctModelConfigChangeListener) {
        this.calCubeTimerGui = calCubeTimerGui;
        this.configuration = configuration;
        this.profileDao = profileDao;
        configuration.addConfigurationChangeListener(cctModelConfigChangeListener);
        LOG.debug("model created");
    }

    @Inject
    void initialize() {
        scramblesList = new GeneratedScrambleList(sessionsList, configuration);
        sessionsList.addSessionListener(sessionListener);
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
    public SolvingProcess getSolvingProcess() {
        return solvingProcess;
    }

    @Override
    public ScrambleList getScramblesList() {
        return scramblesList;
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

        sessionsList.setSessions(solutionDao.loadSessions(newCurrentProfile));
    }

    @Override
    public Profile getSelectedProfile() {
        return currentProfile;
    }

    @Override
    public StackmatInterpreter getStackmatInterpreter() {
        return stackmatInterpreter;
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
    public void setScramblesList(@NotNull ScrambleList scrambleList) {
        this.scramblesList = scrambleList;
    }

}
