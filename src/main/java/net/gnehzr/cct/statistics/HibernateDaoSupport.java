package net.gnehzr.cct.statistics;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.Session;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public static SessionFactory configureSessionFactory() throws HibernateException {
        Configuration configuration = new Configuration();
        configuration.configure();

        Properties properties = configuration.getProperties();

        BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().enableAutoClose().build();
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(bootstrapServiceRegistry).applySettings(properties).build();
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

    protected  <T> T queryFirst(String query, Map<String, Object> args) {
        return doWithSession(session -> Iterables.getFirst(queryList(query, args), null));
    }

    protected <T> List<T> queryList(String query) {
        return queryList(query, Collections.emptyMap());
    }

    protected <T> List<T> queryList(String query, Map<String, Object> args) {
        return doWithSession(new Function<Session, List<T>>() {
            @Override
            public List<T> apply(Session session) {
                Query queryObj = session.createQuery(query);
                for (Map.Entry<String, Object> arg : args.entrySet()) {
                    queryObj.setParameter(arg.getKey(), arg.getValue());
                }
                //noinspection unchecked
                return (List<T>) queryObj.list();
            }
        });
    }

}
