package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.RandomAccessFile;

public class Profile {

    private static final Logger LOG = Logger.getLogger(Profile.class);
    private final ScramblePluginManager scramblePluginManager;

    private RandomAccessFile statisticsRandomAccessFile = null;

    private String name;
    private File directory;
    private File configurationFile;
    private File statistics;

    private boolean saveable = true;

    private final Configuration configuration;

    //constructors are private because we want only 1 instance of a profile
    //pointing to a given database
    Profile(ProfileDao profileDao, String name, Configuration configuration, StatisticsTableModel statsModel, ScramblePluginManager scramblePluginManager) {
        this.name = name;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        directory = profileDao.getDirectory(name);
        configurationFile = profileDao.getConfiguration(directory, name);
        statistics = profileDao.getStatistics(directory, name);
        puzzleDB = new ProfileDatabase(this.configuration, profileDao, statsModel, this.scramblePluginManager);
    }

    //I assume that this will only get called once for a given directory
    public Profile(String name, File directory, File configurationFile, File statistics,
                   Configuration configuration, ProfileDao profileDao, StatisticsTableModel statsModel,
                   ScramblePluginManager scramblePluginManager) {
        this.name = name;
        this.configuration = configuration;
        this.setDirectory(directory);
        this.scramblePluginManager = scramblePluginManager;
        saveable = false;
        this.configurationFile = configurationFile;
        this.statistics = statistics;
        puzzleDB = new ProfileDatabase(this.configuration, profileDao, statsModel, this.scramblePluginManager);
    }

    public boolean isSaveable() {
        return saveable;
    }

    public String getName() {
        return name;
    }

    public File getConfigurationFile() {
        return configurationFile;
    }

    private String newName;

    public void discardRename() {
        newName = null;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof Profile) {
            return ((Profile) o).directory.equals(directory);
        }
        return this.name.equalsIgnoreCase(o.toString());
    }

    //this is the only indication to the user of whether we successfully loaded the database file
    @Override
    public String toString() {
        return (newName != null ? newName : name);// + (isLoginDisabled() ? StringAccessor.getString("Profile.loggingdisabled") : "");
    }

    public void renameTo(String newName) {
        this.newName = newName;
    }

    @NotNull
    public File getDirectory() {
        return directory;
    }

    public File getStatistics() {
        return statistics;
    }

    public void setName(String newName) {
        this.name = newName;
    }
    //Database stuff
    //this maps from ScrambleVariations to PuzzleStatistics
    ProfileDatabase puzzleDB;

    public ProfileDatabase getPuzzleDatabase() {
        return puzzleDB;
    }

    public void setPuzzleDatabase(ProfileDatabase puzzleDatabase) {
        this.puzzleDB = puzzleDatabase;
    }

    public String getNewName() {
        return newName;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public RandomAccessFile getStatisticsRandomAccessFile() {
        return statisticsRandomAccessFile;
    }

    public void setStatisticsRandomAccessFile(@Nullable RandomAccessFile statisticsRandomAccessFile) {
        this.statisticsRandomAccessFile = statisticsRandomAccessFile;
    }

    public void setStatistics(File statistics) {
        this.statistics = statistics;
    }

    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
    }
}
