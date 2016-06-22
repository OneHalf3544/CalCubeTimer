package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * <p>
 * Created: 01.11.2015 19:21
 * <p>
 *
 * @author OneHalf
 */
public class CurrentProfileHolder {

    private static final Logger log = LogManager.getLogger(CurrentProfileHolder.class);

    @Autowired
    private SolutionDao solutionDao;
    @Autowired
    private Configuration configuration;

    private Profile currentProfile;
    @Autowired
    private CalCubeTimerModel model;
    @Autowired
    private SessionsList sessionsList;
    @Autowired
    private ProfileDao profileDao;
    @Autowired
    private CALCubeTimerFrame calCubeTimerGui;

    public Profile getSelectedProfile() {
        return currentProfile;
    }

    public void setSelectedProfile(Profile newCurrentProfile) {
        if (this.currentProfile != null) {
            model.currentProfileHolder.saveProfileConfiguration(model);
        }

        log.info("setSelectedProfile: {}", newCurrentProfile);

        this.currentProfile = newCurrentProfile;
        configuration.loadConfiguration(newCurrentProfile);
        configuration.apply(newCurrentProfile);

        sessionsList.setSessions(solutionDao.loadSessions(newCurrentProfile));
    }

    public void saveProfileConfiguration(CalCubeTimerModel calCubeTimerModel) {
        log.info("save profile configuration");
        Profile profile = getSelectedProfile();
        profileDao.saveLastSession(profile, sessionsList);
        calCubeTimerGui.saveToConfiguration();
        configuration.saveConfiguration(profile);
    }
}
