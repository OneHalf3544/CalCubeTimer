package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleListHolder;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.GeneratedScrambleList;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;

/**
 * <p>
 * <p>
 * Created: 17.01.2015 12:48
 * <p>
 *
 * @author OneHalf
 */
@Service
public class CalCubeTimerModel {

    private static final Logger LOG = LogManager.getLogger(CalCubeTimerModel.class);

    @Autowired
    private Configuration configuration;

    @Autowired
    CurrentProfileHolder currentProfileHolder;

    @Autowired
    private SolutionDao solutionDao;

    @Autowired
    private ProfileDao profileDao;

    private LocaleAndIcon loadedLocale;

    @Autowired
    private SessionsList sessionsList;

    @Autowired
    private ScrambleListHolder scramblesListHolder;

    @Autowired
    private ScramblePluginManager scramblePluginManager;

    private SessionListener sessionListener = new SessionListener() {
        @Override
        public void sessionSelected(Session session) {
            if (scramblesListHolder.isImported()) {
                scramblesListHolder.setScrambleList(new GeneratedScrambleList(sessionsList, configuration));
            }
            scramblesListHolder.asGenerating().generateScrambleForCurrentSession();
            puzzleTypeComboBoxModel.setSelectedItem(session.getPuzzleType()); //this will update the scramble
        }

        @Override
        public void sessionsDeleted() {
            puzzleTypeComboBoxModel.setSelectedItem(sessionsList.getCurrentSession().getPuzzleType());
        }
    };

    private PuzzleTypeComboBoxModel puzzleTypeComboBoxModel;

    @PostConstruct
    void initialize() {
        puzzleTypeComboBoxModel = new PuzzleTypeComboBoxModel(scramblePluginManager);
        scramblesListHolder.setScrambleList(new GeneratedScrambleList(sessionsList, configuration));
        sessionsList.addSessionListener(sessionListener);
        LOG.debug("model initialized");
    }

    public LocaleAndIcon getLoadedLocale() {
        return loadedLocale;
    }

    public void setLoadedLocale(LocaleAndIcon newLocale) {
        this.loadedLocale = newLocale;
    }

    @NotNull
    public PuzzleTypeComboBoxModel getPuzzleTypeComboBoxModel() {
        return puzzleTypeComboBoxModel;
    }
}
