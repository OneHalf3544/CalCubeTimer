package net.gnehzr.cct.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.statistics.Profile;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * <p>
 * Created: 28.01.2015 8:02
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class ConfigurationDao extends HibernateDaoSupport {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationDao.class);

    @Inject
    public ConfigurationDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Map<String, String> getParametersForProfile(Profile profileName) {
        LOGGER.info("get parameters for " + profileName);
        List<ConfigEntity> objects = getConfigEntities(profileName);
        return toEntitiesMap(objects);
    }

    private Map<String, String> toEntitiesMap(List<ConfigEntity> objects) {
        return objects.stream()
                .collect(Collectors.toMap(ConfigEntity::getKey, ConfigEntity::getValue));
    }

    private List<ConfigEntity> getConfigEntities(Profile profileName) {
        if (profileName == null) {
            return Collections.emptyList();
        }
        return queryList("from ConfigEntity where profileName = :profileName", ImmutableMap.of(
                "profileName", profileName.getName()
        ));
    }

    public void storeParameters(Profile profile, Map<String, String> properties) {
        LOGGER.info("store properties for " + profile);

        List<ConfigEntity> oldParameters = getConfigEntities(profile);
        MapDifference<String, String> difference = Maps.difference(toEntitiesMap(oldParameters), properties);
        if (difference.areEqual()) {
            LOGGER.debug("no changes");
            return;
        }
        update(session -> {
            LOGGER.debug("remove " + difference.entriesOnlyOnLeft());
            oldParameters.stream()
                    .filter(c -> difference.entriesOnlyOnLeft().containsKey(c.getKey()))
                    .forEach(session::delete);

            LOGGER.debug("add " + difference.entriesOnlyOnRight());
            difference.entriesOnlyOnRight().entrySet().stream()
                    .map(e -> new ConfigEntity(profile.getName(), e.getKey(), e.getValue()))
                    .forEach(session::save);

            Map<String, MapDifference.ValueDifference<String>> differenceMap = difference.entriesDiffering();
            LOGGER.debug("change " + differenceMap);
            oldParameters.stream()
                    .filter(c -> differenceMap.containsKey(c.getKey()))
                    .peek(c -> c.setValue(differenceMap.get(c.getKey()).rightValue()))
                    .forEach(session::update);
        });
    }

}
