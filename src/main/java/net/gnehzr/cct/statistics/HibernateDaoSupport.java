package net.gnehzr.cct.statistics;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * <p>
 * Created: 24.11.2014 9:51
 * <p>
 *
 * @author OneHalf
 */
public class HibernateDaoSupport {

    private static final Logger LOG = Logger.getLogger(HibernateProfileDao.class);

    // Configure the session factory
    private final SessionFactory sessionFactory;

    public HibernateDaoSupport(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    static SessionFactory configureSessionFactory() throws HibernateException {
        Configuration configuration = new Configuration();
        configuration.configure();

        Properties properties = configuration.getProperties();

        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(properties).buildServiceRegistry();
        return configuration.buildSessionFactory(serviceRegistry);
    }

    public void update(Consumer<Session> updateFunction) {
        doWithSession(session -> {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                updateFunction.accept(session);

                tx.commit();

                return null;

            } catch (Exception ex) {
                if (tx != null) {
                    tx.rollback();
                }
                throw Throwables.propagate(ex);
            }

        });
    }

    private <T> T doWithSession(Function<Session, T> sessionConsumer) {
        Session session = null;
        try {
            session = sessionFactory.openSession();

            T result = sessionConsumer.apply(session);

            // Committing the change in the database.
            session.flush();

            return result;
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public <T> T query(Function<Session, T> updateFunction) {
        return doWithSession(updateFunction::apply);
    }

}
