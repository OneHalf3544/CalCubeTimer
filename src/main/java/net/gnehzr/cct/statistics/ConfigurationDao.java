package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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

    @Inject
    public ConfigurationDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    void tmp() {
        org.hibernate.Session session = null;
        //session.createQuery("from ConfigEntity where profileName = :profileName" );
    }

    public Map<String, String> getParametersForProfile(Profile profileName) {
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
        List<ConfigEntity> oldParameters = getConfigEntities(profile);
        MapDifference<String, String> difference = Maps.difference(toEntitiesMap(oldParameters), properties);
        if (difference.areEqual()) {
            return;
        }
        update(session -> {
            oldParameters.stream()
                    .filter(c -> difference.entriesOnlyOnLeft().containsKey(c.getKey()))
                    .forEach(session::delete);

            difference.entriesOnlyOnRight().entrySet().stream()
                    .map(e -> new ConfigEntity(profile.getName(), e.getKey(), e.getValue()))
                    .forEach(session::save);

            difference.entriesOnlyOnRight().entrySet().stream()
                    .filter(c -> difference.entriesDiffering().containsKey(c.getKey()))
                    .forEach(session::update);
        });
    }

}
