package net.gnehzr.cct.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
public class ConfigurationDao extends HibernateDaoSupport {

    private static final Logger LOGGER = LogManager.getLogger(ConfigurationDao.class);

    @Autowired
    public ConfigurationDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Map<String, String> getParametersForProfile(@NotNull Profile profileName) {
        LOGGER.info("get parameters for " + profileName);
        List<ConfigEntity> objects = getConfigEntities(profileName);
        return toEntitiesMap(objects);
    }

    private Map<String, String> toEntitiesMap(List<ConfigEntity> objects) {
        return objects.stream()
                .collect(Collectors.toMap(ConfigEntity::getKey, ConfigEntity::getValue));
    }

    private List<ConfigEntity> getConfigEntities(@NotNull Profile profile) {
        return queryList("from ConfigEntity where profileId = :profileId", ImmutableMap.of(
                "profileId", profile.getId()
        ));
    }

    public void storeParameters(@NotNull Profile profile, @NotNull Map<String, String> properties) {
        LOGGER.info("store properties for " + profile);

        List<ConfigEntity> oldParameters = getConfigEntities(profile);
        MapDifference<String, String> difference = Maps.difference(toEntitiesMap(oldParameters), properties);
        if (difference.areEqual()) {
            LOGGER.debug("no changes");
            return;
        }
        update(session -> {
            if (!difference.entriesOnlyOnLeft().isEmpty()) {
                LOGGER.debug("remove " + difference.entriesOnlyOnLeft());
                oldParameters.stream()
                        .filter(c -> difference.entriesOnlyOnLeft().containsKey(c.getKey()))
                        .forEach(session::delete);
            }
            if (!difference.entriesOnlyOnRight().isEmpty()) {
                LOGGER.debug("add " + difference.entriesOnlyOnRight());
                difference.entriesOnlyOnRight().entrySet().stream()
                        .map(e -> new ConfigEntity(profile.getId(), e.getKey(), e.getValue()))
                        .forEach(session::save);
            }

            Map<String, MapDifference.ValueDifference<String>> differenceMap = difference.entriesDiffering();
            if (!differenceMap.isEmpty()) {
                LOGGER.debug("change " + differenceMap);
                oldParameters.stream()
                        .filter(c -> differenceMap.containsKey(c.getKey()))
                        .peek(c -> c.setValue(differenceMap.get(c.getKey()).rightValue()))
                        .forEach(session::update);
            }
        });
    }

}
