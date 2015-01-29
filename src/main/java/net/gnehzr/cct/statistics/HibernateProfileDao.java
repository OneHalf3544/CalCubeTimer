package net.gnehzr.cct.statistics;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * <p>
 * <p>
 * Created: 24.11.2014 2:05
 * <p>
 *
 * @author OneHalf
 */
public class HibernateProfileDao extends HibernateDaoSupport {

    private static final Logger LOG = Logger.getLogger(HibernateProfileDao.class);

    public HibernateProfileDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Entity(name = "PROFILE")
    public static class ProfileEntity {

        @Id
        private String name;

        public ProfileEntity() {
        }

        public ProfileEntity(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}