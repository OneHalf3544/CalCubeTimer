package net.gnehzr.cct.statistics;

import net.gnehzr.cct.statistics.HibernateProfileDao.ProfileEntity;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.testng.annotations.Test;

import java.util.List;

public class HibernateProfileDaoTest {

    private static final Logger LOG = Logger.getLogger(HibernateProfileDaoTest.class);

    @Test
    public void main() {
        SessionFactory sessionFactory = HibernateProfileDao.configureSessionFactory();
        HibernateProfileDao profileDao = new HibernateProfileDao(sessionFactory);

        profileDao.update(session1 -> {
            // Creating Contact entity that will be save to the sqlite database
            ProfileEntity myProfile = new ProfileEntity("Profile_Name");

            // Saving to the database
            session1.save(myProfile);
        });

        List<ProfileEntity> contactList = profileDao.queryList("from PROFILE");

        for (ProfileEntity contact : contactList) {
            LOG.info("Name: " + contact.getName());
        }
        sessionFactory.close();
    }

}