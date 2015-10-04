package net.gnehzr.cct.dao;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.statistics.Session;
import org.hibernate.SessionFactory;

/**
 * <p>
 * <p>
 * Created: 28.01.2015 7:52
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class SolutionDao extends HibernateDaoSupport {

    @Inject
    public SolutionDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void saveSession(SessionEntity session) {
        insertOrUpdate(session);
    }

    public SessionEntity loadSession(Session session) {
        return queryFirst("FROM SessionEntity WHERE sessionId = :id", ImmutableMap.of(
                "id", session.getSessionId()));
    }
}
