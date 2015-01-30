package net.gnehzr.cct.dao;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.CalCubeTimerGui;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.ProfileSerializer.RandomInputStream;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final Logger LOG = Logger.getLogger(ProfileDao.class);

    private final static Map<String, Profile> profiles = new HashMap<>();
    private final ProfileSerializer profileSerializer;
    private final Configuration configuration;

    public final Profile guestProfile;

    private Session guestSession = null; //need this so we can load the guest's last session, since it doesn't have a file

    private final StatisticsTableModel statsModel;
    private final ScramblePluginManager scramblePluginManager;

    private static final String GUEST_NAME = "Guest";
    private CalCubeTimerGui calCubeTimerFrame;

    @Inject
    public ProfileDao(ProfileSerializer profileSerializer, Configuration configuration, StatisticsTableModel statsModel,
                      ScramblePluginManager scramblePluginManager, CalCubeTimerGui calCubeTimerFrame, SessionFactory sessionFactory) {
        super(sessionFactory);
        this.profileSerializer = profileSerializer;
        this.configuration = configuration;
        this.statsModel = statsModel;
        this.scramblePluginManager = scramblePluginManager;
        guestProfile = createGuestProfile();
        this.calCubeTimerFrame = calCubeTimerFrame;
    }

    public List<ProfileEntity> getAllProfiles() {
        return queryList("from PROFILE order by name");
    }

    public Profile getProfileByName(String name) {
        return profiles.computeIfAbsent(name, (n) -> new Profile(n,
                getDirectory(n),
                getConfiguration(configuration.getProfilesFolder(), n),
                getStatistics(configuration.getProfilesFolder(), n),
                configuration, this, statsModel, scramblePluginManager));
    }

    public Profile loadProfile(File directory) {
        LOG.debug("load profile from " + directory);
        String name = directory.getName();
        File configurationFile = getConfiguration(directory, directory.getName());
        File statistics = getStatistics(directory, directory.getName());

        Profile profile = new Profile(name, directory, configurationFile, statistics, configuration, this, statsModel, scramblePluginManager);
        profiles.put(profile.getName(), profile);
        return profile;
    }

    public void createProfileDirectory(Profile profile) {
        profile.getDirectory().mkdir();
    }

    public void delete(Profile profile) {
        if (profile.getStatisticsRandomAccessFile() != null) {
            closeStatFileAndReleaseLock(profile);
        }
        profile.getConfigurationFile().delete();
        profile.getStatistics().delete();
        profile.getDirectory().delete();
        if (getSelectedProfile() == profile) {
            setSelectedProfile(null);
        }
    }

    // this can only be called once, until after saveDatabase() is called
    public boolean loadDatabase(Profile profile, ScramblePluginManager scramblePluginManager) {
        if (profile == guestProfile) { // disable logging for guest
            // TODO - there is definitely a bug here where guestSession == null when switching profiles
            if (profile.getPuzzleDatabase().getRowCount() > 0) {
                statsModel.setSession(guestSession); //TODO - does this really need to be here?
            }
            return false;
        }

        return Utils.doWithLockedFile(profile.getStatistics(), file -> {
            Utils.doInWaitingState(calCubeTimerFrame, () -> {
                try {
                    ProfileDatabase puzzleDB = new ProfileDatabase(configuration, this, statsModel, scramblePluginManager); //reset the database
                    profile.setPuzzleDatabase(puzzleDB);
                    profile.setStatisticsRandomAccessFile(file);

                    if (file.length() == 0) {
                        LOG.debug("file is empty. skip parsing");
                    } else { // if the file is empty, don't bother to parse it
                        LOG.debug("parse file");
                        DatabaseLoader handler = new DatabaseLoader(profile, configuration, statsModel, scramblePluginManager);
                        profileSerializer.parseBySaxHandler(handler, new RandomInputStream(profile.getStatisticsRandomAccessFile()));
                    }
                    LOG.debug("file loading finished");

                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            });
        });
    }

    public void saveDatabase(Profile profile) throws IOException, TransformerConfigurationException, SAXException {
        LOG.debug("save database");
        profile.getPuzzleDatabase().removeEmptySessions();
        if (profile == guestProfile) {
            guestSession = statsModel.getCurrentSession();
        }

        if (profile.getStatisticsRandomAccessFile() == null) {
            LOG.warn("statisticsRandomAccessFile is null. skip saving");
            return;
        }

        Utils.doInWaitingState(calCubeTimerFrame, () -> {
            try {
                profileSerializer.writeStatisticFile(profile);

            } catch (IOException | SAXException | TransformerConfigurationException e) {
                Throwables.propagate(e);
            }
        });

        closeStatFileAndReleaseLock(profile);
    }

    private void openAndLockStatFile(Profile profile) {
        LOG.debug("openAndLockStatFile + " + profile.getStatistics());
        RandomAccessFile t = null;
        try {
            t = new RandomAccessFile(profile.getStatistics(), "rw");
            t.getChannel().tryLock();
        } catch (IOException e) {
            LOG.info("unexpected exception", e);
        } finally {
            profile.setStatisticsRandomAccessFile(t);
        }
    }

    void closeStatFileAndReleaseLock(Profile profile) {
        LOG.debug("closeStatFileAndReleaseLock");
        try {
            profile.getStatisticsRandomAccessFile().close();
        } catch (IOException e) {
            LOG.info("unexpected exception", e);
        } finally {
            profile.setStatisticsRandomAccessFile(null);
        }
    }

    @NotNull
    public File getDirectory(String name) {
        return new File(configuration.getProfilesFolder(), name + "/");
    }

    @NotNull
    public File getConfiguration(File directory, String name) {
        return new File(directory, name + ".properties");
    }

    @NotNull
    public File getStatistics(File directory, String name) {
        return new File(directory, name + ".xml");
    }

    public void commitRename(Profile profile) {
        String newName = profile.getNewName();
        File newDir = getDirectory(newName);

        File oldConfig = getConfiguration(newDir, profile.getName());
        File oldStats = getStatistics(newDir, profile.getName());

        File configuration = getConfiguration(newDir, newName);
        File statistics = getStatistics(newDir, newName);

        boolean currentProfile = (profile.getStatisticsRandomAccessFile() != null);
        if (currentProfile) {
            closeStatFileAndReleaseLock(profile);
        }
        profile.getDirectory().renameTo(newDir);
        profile.setDirectory(newDir);
        profile.setStatistics(statistics);
        profile.setConfigurationFile(configuration);

        oldConfig.renameTo(configuration);
        oldStats.renameTo(statistics);

        if (currentProfile) {
            openAndLockStatFile(profile);
        }

        profiles.remove(profile.getName());
        profile.setName(newName);
        profiles.put(profile.getName(), profile);
    }

    public Profile createGuestProfile() {
        Profile temp = getProfileByName(GUEST_NAME);
        createProfileDirectory(temp);
        return temp;
    }

    public List<Profile> getProfiles(Configuration configuration) {
        String[] profDirs = configuration.getProfilesFolder().list((f, s) -> {
            File temp = new File(f, s);
            return !temp.isHidden() && temp.isDirectory() && !s.equalsIgnoreCase(GUEST_NAME);
        });
        List<Profile> profs = new ArrayList<>();
        profs.add(guestProfile);
        for(String profDir : profDirs) {
            profs.add(getProfileByName(profDir));
        }
        if(configuration.props != null && configuration.profileOrdering != null) {
            String[] profiles = configuration.profileOrdering.split("\\|");
            for(int ch = profiles.length - 1; ch >= 0; ch--) {
                Profile temp = getProfileByName(profiles[ch]);
                if(profs.contains(temp)) {
                    profs.remove(temp);
                    profs.add(0, temp);
                }
            }
        }
        if(configuration.commandLineProfile != null)
            profs.add(0, configuration.commandLineProfile);
        return profs;
    }

    public Profile getProfile(String profileName, Configuration configuration) {
        return getProfiles(configuration).stream()
                .filter(p -> p.getName().equalsIgnoreCase(profileName))
                .findFirst()
                .orElse(guestProfile);
    }

    private Profile profileCache;

    public void setSelectedProfile(Profile p) {
        profileCache = p;
    }

    //this should always be up to date with the gui
    public Profile getSelectedProfile() {
        if(profileCache == null) {
            String profileName;
            try(BufferedReader in = new BufferedReader(new FileReader(configuration.getStartupProfileFile()))) {
                profileName = in.readLine();
                configuration.profileOrdering = in.readLine();
            } catch (IOException e) {
                LOG.info("exception", e);
                profileName = "";
        }
            profileCache = getProfile(profileName, configuration);
        }
        return profileCache;
    }

}
