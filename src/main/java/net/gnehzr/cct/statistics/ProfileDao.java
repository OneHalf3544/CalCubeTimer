package net.gnehzr.cct.statistics;

import com.google.common.base.Throwables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.CALCubeTimer;
import net.gnehzr.cct.statistics.ProfileSerializer.RandomInputStream;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import java.io.*;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>
 * <p>
 * Created: 08.11.2014 1:47
 * <p>
 *
 * @author OneHalf
 */
public class ProfileDao {

    private static final Logger LOG = Logger.getLogger(ProfileDao.class);

    public static final ProfileDao INSTANCE = new ProfileDao();

    private final static Map<String, Profile> profiles = new HashMap<>();
    private final ProfileSerializer profileSerializer = new ProfileSerializer(this);

    private Session guestSession = null; //need this so we can load the guest's last session, since it doesn't have a file

    private ProfileDao() {
    }

    public static Profile getProfileByName(String name) {
        return profiles.computeIfAbsent(name, Profile::new);
    }

    public static Profile loadProfile(File directory) {
        String name = directory.getAbsolutePath();
        File configuration = getConfiguration(directory, directory.getName());
        File statistics = getStatistics(directory, directory.getName());

        Profile profile = new Profile(name, directory, configuration, statistics);
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
        if (Configuration.getSelectedProfile() == profile) {
            Configuration.setSelectedProfile(null);
        }
    }

    private boolean isLoginDisabled(Profile profile) {
        return profile.getStatisticsRandomAccessFile() == null && profile == Configuration.getSelectedProfile();
    }

    // this can only be called once, until after saveDatabase() is called
    public boolean loadDatabase(Profile profile) {
        if (profile == Configuration.guestProfile) { // disable logging for guest
            // TODO - there is definitely a bug here where guestSession == null when switching profiles
            if (profile.getPuzzleDatabase().getRowCount() > 0) {
                CALCubeTimer.statsModel.setSession(guestSession); //TODO - does this really need to be here?
            }
            return false;
        }

        return doWithLockedFile(profile.getStatistics(), file -> {
            profileSerializer.doInWaitingState(() -> {
                try {
                    ProfileDatabase puzzleDB = new ProfileDatabase(profile); //reset the database
                    profile.setPuzzleDatabase(puzzleDB);

                    if (file.length() != 0) { // if the file is empty, don't bother to parse it
                        profile.setStatisticsRandomAccessFile(file);
                        DatabaseLoader handler = new DatabaseLoader(profile);
                        profileSerializer.parseBySaxHandler(handler, new RandomInputStream(profile.getStatisticsRandomAccessFile()));
                    }

                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            });
        });
    }

    /**
     * @param file
     * @param fileConsumer
     * @return true, if file was processed. false, if file locked by another task
     */
    private boolean doWithLockedFile(@NotNull File file,
                                     @NotNull Consumer<RandomAccessFile> fileConsumer) {
        RandomAccessFile t;
        try {
            t = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        }

        try(FileLock fileLock = t.getChannel().tryLock()) {
            if (fileLock != null) {
                fileConsumer.accept(t);
                return true;
            }
            return false;

        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void saveDatabase(Profile profile) throws IOException, TransformerConfigurationException, SAXException {
        LOG.info("save database");
        profile.getPuzzleDatabase().removeEmptySessions();
        if (profile == Configuration.guestProfile) {
            guestSession = CALCubeTimer.statsModel.getCurrentSession();
        }

        if (profile.getStatisticsRandomAccessFile() == null) {
            LOG.warn("statisticsRandomAccessFile is null. skip saving");
            return;
        }

        profileSerializer.doInWaitingState(() -> {
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
    static File getDirectory(String name) {
        return new File(Configuration.profilesFolder, name + "/");
    }

    @NotNull
    static File getConfiguration(File directory, String name) {
        return new File(directory, name + ".properties");
    }

    @NotNull
    static File getStatistics(File directory, String name) {
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
        profile.setConfiguration(configuration);

        oldConfig.renameTo(configuration);
        oldStats.renameTo(statistics);

        if (currentProfile) {
            openAndLockStatFile(profile);
        }

        profiles.remove(profile.getName());
        profile.setName(newName);
        profiles.put(profile.getName(), profile);
    }//I can't believe I had to create these two silly little classses
}
