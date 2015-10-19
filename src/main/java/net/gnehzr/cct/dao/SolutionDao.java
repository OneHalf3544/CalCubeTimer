package net.gnehzr.cct.dao;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.Solution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

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

    private static final Logger LOG = LogManager.getLogger(SolutionDao.class);

    @Inject
    private Configuration configuration;

    @Inject
    public SolutionDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public void saveSession(Profile profile, Session session) {
        LOG.info("save current session for {}", profile);
        SessionEntity sessionEntity = session.toSessionEntity(profile.getId());
        insertOrUpdate(sessionEntity);
        session.setSessionId(sessionEntity.getSessionId());
    }

    public void deleteSession(Session session) {
        LOG.info("remove session {}", session);
        doWithSession(s -> s.delete(session));
    }

    public SessionEntity loadSession(Session session) {
        return queryFirst("FROM SessionEntity WHERE sessionId = :id", ImmutableMap.of(
                "id", session.getSessionId()));
    }

    public List<String> getUsedPuzzleTypes(Profile profile) {
        return queryList("select distinct variationName from SessionEntity where profile.profileId = :id", ImmutableMap.of(
                "id", profile.getId()
        ));
    }

    @Deprecated // todo load sessions on demand
    public List<Session> loadDatabase(@NotNull Profile profile, ScramblePluginManager scramblePluginManager) {
        List<SessionEntity> sessionEntities = queryList("from SessionEntity where profile.profileId = :profileId",
                Collections.singletonMap("profileId", profile.getId()));

        return sessionEntities.stream()
                .map(s -> s.toSession(configuration, scramblePluginManager))
                .collect(toList());
    }

    @Deprecated // todo save solutions after each attempt
    public void saveDatabase(Profile profile, SessionsList sessionsList) {
        LOG.debug("save database for profile {}", profile);
        sessionsList.removeEmptySessions();
        sessionsList.getSessions().stream()
                .map(s -> s.toSessionEntity(profile.getId()))
                .forEach(this::insertOrUpdate);
    }

    public void insertSolution(Solution solution) {
        SolutionEntity entity = solution.toEntity();
        doWithSession(session -> {
            insert(entity);
        });
        solution.setSolutionId(entity.getId());
    }
}
