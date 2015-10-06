package net.gnehzr.cct.dao;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import net.gnehzr.cct.dao.converters.DurationConverter;
import net.gnehzr.cct.dao.converters.LocalDateTimeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.*;
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

    private static final Logger LOG = LogManager.getLogger(HibernateDaoSupport.class);

    // Configure the session factory
    private final SessionFactory sessionFactory;

    public HibernateDaoSupport(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public static SessionFactory configureSessionFactory() throws HibernateException {
        Configuration configuration = new Configuration();
        configuration.addAttributeConverter(DurationConverter.class);
        configuration.addAttributeConverter(LocalDateTimeConverter.class);
        configuration.configure();

        Properties properties = configuration.getProperties();

        BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().enableAutoClose().build();
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder(bootstrapServiceRegistry)
                .enableAutoClose()
                .applySettings(properties)
                .build();
        return configuration.buildSessionFactory(serviceRegistry);
    }

    protected void update(String updateQuery) {
        update(updateQuery, Collections.emptyMap());
    }

    protected void insert(Object object) {
        update(session ->  {
            session.save(object);
        });
    }

    protected void insertOrUpdate(Object object) {
        update(session ->  {
            session.saveOrUpdate(object);
        });
    }

    protected void update(String updateQuery, Map<String, Object> args) {
        update(s -> {
            createQuery(s, updateQuery, args).executeUpdate();
        });
    }

    protected void update(Consumer<Session> updateFunction) {
        queryWithSession(session -> {
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

    protected void delete(Object o) {
        update(s -> {
            s.delete(o);
        });
    }

    protected void doWithSession(Consumer<Session> sessionConsumer) {
        queryWithSession((session) -> {
            sessionConsumer.accept(session);
            return null;
        });
    }

    protected <T> T queryWithSession(Function<Session, T> sessionConsumer) {
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

    protected  <T> T queryFirst(String query) {
        return queryFirst(query, Collections.emptyMap());
    }

    protected  <T> T queryFirst(String query, Map<String, Object> args) {
        return queryWithSession(session -> Iterables.getFirst(queryListWithLimit(query, args, 1), null));
    }

    protected <T> List<T> queryList(String query) {
        return queryList(query, Collections.emptyMap());
    }

    protected <T> List<T> queryList(String query, Map<String, Object> args) {
        return queryListWithLimit(query, args, null);
    }

    private <T> List<T> queryListWithLimit(final String query, final Map<String, Object> args, Integer limit) {
        return queryWithSession(new Function<Session, List<T>>() {
            @Override
            public List<T> apply(Session session) {
                Query queryObj = createQuery(session, query, args);
                if (limit != null) {
                    queryObj.setMaxResults(limit);
                }
                //noinspection unchecked
                return (List<T>) queryObj.list();
            }
        });
    }

    private Query createQuery(Session session, String query, Map<String, Object> args) {
        Query queryObj = session.createQuery(query);
        for (Map.Entry<String, Object> arg : args.entrySet()) {
            queryObj.setParameter(arg.getKey(), arg.getValue());
        }
        return queryObj;
    }

}
