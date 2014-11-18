package net.gnehzr.cct.statistics;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.RandomAccessFile;

public class Profile {

    private static final Logger LOG = Logger.getLogger(Profile.class);

    private RandomAccessFile statisticsRandomAccessFile = null;

    private String name;
    private File directory;
    private File configuration;
    private File statistics;

    //constructors are private because we want only 1 instance of a profile
    //pointing to a given database
    Profile(String name) {
        this.name = name;
        directory = ProfileDao.getDirectory(name);
        configuration = ProfileDao.getConfiguration(directory, name);
        statistics = ProfileDao.getStatistics(directory, name);
    }

    private boolean saveable = true;

    //I assume that this will only get called once for a given directory
    public Profile(String name, File directory, File configuration, File statistics) {
        saveable = false;
        this.directory = directory;
        this.configuration = configuration;
        this.statistics = statistics;
    }

    public boolean isSaveable() {
        return saveable;
    }

    public String getName() {
        return name;
    }

    public File getConfigurationFile() {
        return configuration;
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

    File getDirectory() {
        return directory;
    }

    File getStatistics() {
        return statistics;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    //Database stuff
    //this maps from ScrambleVariations to PuzzleStatistics
    ProfileDatabase puzzleDB = new ProfileDatabase(this);

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

    public void setConfiguration(File configuration) {
        this.configuration = configuration;
    }
}
