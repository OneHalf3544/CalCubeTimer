package net.gnehzr.cct.dao;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

public class ProfileDaoTest {

    private static final Logger LOG = LogManager.getLogger(ProfileDaoTest.class);

    @Test(enabled = false)
    public void main() {
        SessionFactory sessionFactory = ProfileDao.configureSessionFactory();
        ProfileDao profileDao = new ProfileDao(mock(Configuration.class),
                mock(CurrentSessionSolutionsTableModel.class), mock(ScramblePluginManager.class),
                sessionFactory, mock(SolutionDao.class), mock(CalCubeTimerModel.class));

        profileDao.update(session1 -> {
            // Creating Contact entity that will be save to the sqlite database
            ProfileEntity myProfile = new ProfileEntity("Profile_Name");

            // Saving to the database
            session1.save(myProfile);
        });

        Profile contact = profileDao.loadProfile("OneHalf");

        LOG.info("Name: " + contact.getName());
        sessionFactory.close();
    }

}