package net.gnehzr.cct.dao;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsListTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;

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

    public static final String GUEST_NAME = "Guest";

    private final Configuration configuration;

    private final CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

    private SolutionDao solutionsDao;
    private final CalCubeTimerModel cubeTimerModel;

    @Inject
    public ProfileDao(Configuration configuration, CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel,
                      ScramblePluginManager scramblePluginManager, SessionFactory sessionFactory,
                      SolutionDao solutionsDao, CalCubeTimerModel model) {
        super(sessionFactory);
        this.configuration = configuration;
        this.currentSessionSolutionsTableModel = currentSessionSolutionsTableModel;
        this.solutionsDao = solutionsDao;
        cubeTimerModel = model;
    }

    public Profile loadProfile(@NotNull String name) {
        ProfileEntity profileEntity = loadProfileEntity(name);

        return mapEntityToProfile(name, profileEntity);
    }

    public ProfileEntity loadProfileEntity(@NotNull String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        LOG.info("load profile entity {}", name);

        return queryFirst("from PROFILE where name = :name",
                Collections.singletonMap("name", name));
    }

    @NotNull
    private Profile mapEntityToProfile(@NotNull String name, ProfileEntity profileEntity) {
        SessionsListTableModel sessionsListTableModel = new SessionsListTableModel(configuration, this, cubeTimerModel,
                currentSessionSolutionsTableModel);
        Profile profile;
        if (profileEntity != null) {
            profile = new Profile(profileEntity.getProfileId(), name, sessionsListTableModel);
            return profile;
        } else {
            profile = new Profile(null, name, sessionsListTableModel);
            saveProfile(profile);
            return profile;
        }
    }

    public void saveProfile(Profile profile) {
        if (profile.getId() == null) {
            LOG.info("save profile for {}", profile);
            saveProfileWithoutSession(profile);
        }
        LOG.info("save current session for {}", profile);
        solutionsDao.saveSession(currentSessionSolutionsTableModel.getCurrentSession().toSessionEntity(profile.getId()));
    }

    private void saveProfileWithoutSession(Profile profile) {
        ProfileEntity entity = profile.toEntity(currentSessionSolutionsTableModel.getCurrentSession().getSessionId());
        insertOrUpdate(entity);
        profile.setId(entity.getProfileId());
    }

    public void delete(Profile profile) {
        super.delete(profile);
    }

    public void loadDatabase(@NotNull Profile profile, ScramblePluginManager scramblePluginManager) {
        doWithSession((session) -> {
            List<SessionEntity> entity = queryList("from SessionEntity where profile.profileId = :profileId",
                    Collections.singletonMap("profileId", profile.getId()));

            SessionsListTableModel sessionsListTableModel = new SessionsListTableModel(
                    configuration, this, cubeTimerModel, currentSessionSolutionsTableModel);

            sessionsListTableModel.getSessionsList().setSessions(entity.stream()
                    .map(s -> s.toSession(configuration, scramblePluginManager))
                    .collect(toList()));
            profile.setSessionsListTableModel(sessionsListTableModel);
            sessionsListTableModel.fireTableDataChanged();
        });
    }

    public void saveDatabase(Profile profile) {
        LOG.debug("save database for profile {}", profile);
        profile.getSessionsListTableModel().getSessionsList().removeEmptySessions();
        profile.getSessionsListTableModel().getSessionsList().getSessions().stream()
                .map(s -> s.toSessionEntity(profile.getId()))
                .forEach(this::insertOrUpdate);
    }

    public void commitRename(Profile profile) {
        LOG.info("rename profile from {} to {}", profile.getName(), profile.getNewName());
        profile.setName(profile.getNewName());
        insertOrUpdate(profile);
    }

    public Profile getOrCreateGuestProfile() {
        Profile temp = loadProfile(GUEST_NAME);
        saveProfileWithoutSession(temp);
        return temp;
    }

    public List<Profile> getProfiles() {
        List<ProfileEntity> profilesExceptGuest = getProfileEntitiesExcept(GUEST_NAME);

        List<Profile> profs = profilesExceptGuest.stream()
                .map(profDir -> loadProfile(profDir.getName()))
                .collect(toList());
        profs.add(0, getOrCreateGuestProfile());

        return profs;
    }

    public List<ProfileEntity> getProfileEntitiesExcept(String exceptedProfileName) {
        return queryList(
                    "from PROFILE where name != '" + exceptedProfileName + "' order by lastSessionId desc ");
    }

    @NotNull
    public Profile loadLastProfile() {
        LOG.info("load last used profile");
        ProfileEntity lastUsedProfile = queryFirst("FROM PROFILE ORDER BY lastSessionId DESC");
        return lastUsedProfile == null
                ? getOrCreateGuestProfile()
                : mapEntityToProfile(lastUsedProfile.getName(), lastUsedProfile);
    }

    public void moveSessionsTo(Session[] sessions, ProfileEntity anotherProfile) {
        for (Session session : sessions) {
            insertOrUpdate(session.toSessionEntity(anotherProfile.getProfileId()));
        }
    }
}
