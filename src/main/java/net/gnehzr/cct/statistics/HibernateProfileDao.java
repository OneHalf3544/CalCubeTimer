package net.gnehzr.cct.statistics;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * <p>
 * Created: 24.11.2014 2:05
 * <p>
 *
 * @author OneHalf
 */
public class HibernateProfileDao extends HibernateDaoSupport {

    public HibernateProfileDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Entity(name = "Profile")
    public static class ProfileEntity {

        private String name;

        public ProfileEntity() {

        }

        public ProfileEntity(String name) {
            this.name = name;
        }

        @Id
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static final Logger LOG = Logger.getLogger(HibernateProfileDao.class);

    public static void main(String[] args) {
        SessionFactory sessionFactory = configureSessionFactory();
        HibernateProfileDao profileDao = new HibernateProfileDao(sessionFactory);

        profileDao.update(session1 -> {
            // Creating Contact entity that will be save to the sqlite database
            ProfileEntity myProfile = new ProfileEntity("Profile Name");

            // Saving to the database
            session1.save(myProfile);
        });

        List<ProfileEntity> contactList = profileDao.query(new Function<Session, List<ProfileEntity>>() {
            @Override
            public List<ProfileEntity> apply(Session session) {
                // Fetching saved data
                return session.createQuery("from Profile").list();
            }
        });

        for (ProfileEntity contact : contactList) {
            LOG.info("Name: " + contact.getName() + " | PuzzleDatabase: ");// + contact.getPuzzleDatabase());
        }
        sessionFactory.close();
    }
}