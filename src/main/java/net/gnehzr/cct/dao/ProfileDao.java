package net.gnehzr.cct.dao;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsTableModel;
import net.gnehzr.cct.statistics.StatisticsTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>
 * <p>
 * Created: 08.11.2014 1:47
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class ProfileDao extends HibernateDaoSupport {

    private static final Logger LOG = LogManager.getLogger(ProfileDao.class);

    private static final String GUEST_NAME = "Guest";

    private final static Map<String, Profile> profiles = new HashMap<>();

    private final Configuration configuration;

    public final Profile guestProfile;

    private final ConfigurationDao configurationDao;
    private final StatisticsTableModel statisticsTableModel;
    private final ScramblePluginManager scramblePluginManager;

    @NotNull
    private Profile currentProfile;
    private SolutionDao solutionsDao;

    @Inject
    public ProfileDao(Configuration configuration, ConfigurationDao configurationDao, StatisticsTableModel statisticsTableModel,
                      ScramblePluginManager scramblePluginManager, SessionFactory sessionFactory, SolutionDao solutionsDao) {
        super(sessionFactory);
        this.configuration = configuration;
        this.configurationDao = configurationDao;
        this.statisticsTableModel = statisticsTableModel;
        this.scramblePluginManager = scramblePluginManager;
        this.solutionsDao = solutionsDao;
        guestProfile = getOrCreateGuestProfile();
        currentProfile = loadCurrentProfile();
    }

    public List<ProfileEntity> getAllProfiles() {
        return queryList("from PROFILE order by name");
    }

    public Profile getProfileByName(String name) {
        return profiles.computeIfAbsent(name, this::loadProfile);
    }

    public Profile loadProfile(@NotNull String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        LOG.debug("load profile " + name);

        ProfileEntity profileEntity = queryFirst("from PROFILE where name = :name",
                Collections.singletonMap("name", name));

        SessionsTableModel sessionsTableModel = new SessionsTableModel(configuration, this, statisticsTableModel, scramblePluginManager);
        Profile profile;
        if (profileEntity != null) {
            profile = new Profile(profileEntity.getProfileId(), name, sessionsTableModel);
            //statisticsTableModel.getCurrentSession().setCustomization();
        } else {
            profile = new Profile(null, name, sessionsTableModel);
            saveProfile(profile);
            return profile;
        }
        profiles.put(profile.getName(), profile);
        return profile;
    }

    public void saveProfile(Profile profile) {
        if (profile.getId() == null) {
            saveProfileWithoutSession(profile);
        }
        solutionsDao.saveSession(statisticsTableModel.getCurrentSession().toSessionEntity());
    }

    private void saveProfileWithoutSession(Profile profile) {
        ProfileEntity entity = profile.toEntity(statisticsTableModel.getCurrentSession().getSessionId());
        insert(entity);
        profile.setId(entity.getProfileId());
    }

    public void delete(Profile profile) {
        super.delete(profile);

        if (getSelectedProfile() == profile) {
            setSelectedProfile(null);
        }
    }

    public void loadDatabase(@NotNull Profile profile, ScramblePluginManager scramblePluginManager) {
        profile.setPuzzleDatabase(new SessionsTableModel(configuration, this, statisticsTableModel, scramblePluginManager));
    }

    public void saveDatabase(Profile profile) {
        LOG.debug("save database");
        profile.getSessionsDatabase().removeEmptySessions();
    }

    public void commitRename(Profile profile) {
        String newName = profile.getNewName();
        profiles.remove(profile.getName());
        profile.setName(newName);
        profiles.put(profile.getName(), profile);
    }

    public Profile getOrCreateGuestProfile() {
        Profile temp = getProfileByName(GUEST_NAME);
        saveProfileWithoutSession(temp);
        return temp;
    }

    public List<Profile> getProfiles(Configuration configuration) {
        List<ProfileEntity> profilesExceptGuest = queryList("from PROFILE where name != '" + GUEST_NAME + "'");

        List<Profile> profs = profilesExceptGuest.stream()
                .map(profDir -> getProfileByName(profDir.getName()))
                .collect(Collectors.toList());
        profs.add(0, guestProfile);

        if(configuration.isPropertiesLoaded() && configuration.profileOrdering != null) {
            String[] profiles = configuration.profileOrdering.split("\\|");
            for(int ch = profiles.length - 1; ch >= 0; ch--) {
                if (Strings.isNullOrEmpty(profiles[ch])) {
                    continue;
                }
                Profile temp = getProfileByName(profiles[ch]);
                if(profs.contains(temp)) {
                    profs.remove(temp);
                    profs.add(0, temp);
                }
            }
        }

        return profs;
    }

    public Profile getProfile(@NotNull ProfileEntity profile, Configuration configuration) {
        return getProfiles(configuration).stream()
                .filter(p -> p.getName().equalsIgnoreCase(profile.getName()))
                .findFirst()
                .orElse(guestProfile);
    }

    public void setSelectedProfile(Profile profile) {
        currentProfile = profile;
    }

    //this should always be up to date with the gui
    public Profile getSelectedProfile() {
        return currentProfile;
    }

    @NotNull
    private Profile loadCurrentProfile() {
        SystemSettingsEntity systemSettings = configurationDao.getSystemSettingsEntity();
        configuration.profileOrdering = systemSettings.getProfileOrdering();
        ProfileEntity startupProfileEntity = systemSettings.getStartupProfile();

        return startupProfileEntity == null ? guestProfile : getProfile(startupProfileEntity, configuration);
    }

}
