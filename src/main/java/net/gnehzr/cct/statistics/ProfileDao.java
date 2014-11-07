package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.CALCubeTimer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
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

    protected RandomAccessFile statisticsRandomAccessFile = null;
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
        if (statisticsRandomAccessFile != null) {
            closeStatFileAndReleaseLock();
        }
        profile.getConfigurationFile().delete();
        profile.getStatistics().delete();
        profile.getDirectory().delete();
        if (Configuration.getSelectedProfile() == profile) {
            Configuration.setSelectedProfile(null);
        }
    }

    private boolean isLoginDisabled(Profile profile) {
        return statisticsRandomAccessFile == null && profile == Configuration.getSelectedProfile();
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
            try {
                CALCubeTimer.setWaiting(true);

                ProfileDatabase puzzleDB = new ProfileDatabase(profile); //reset the database
                profile.setPuzzleDatabase(puzzleDB);

                if (file.length() != 0) { // if the file is empty, don't bother to parse it
                    statisticsRandomAccessFile = file;
                    DatabaseLoader handler = new DatabaseLoader(profile);
                    parseBySaxHandler(handler, new RandomInputStream(statisticsRandomAccessFile));
                }

            } catch (IOException e) {
                LOG.error("i/o error", e);

            } finally {
                CALCubeTimer.setWaiting(false);
            }
        });
    }

    private void parseBySaxHandler(DefaultHandler parseLogic, RandomInputStream inputStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, parseLogic);

        } catch (IOException e) {
            LOG.error("i/o error", e);

        } catch (SAXParseException spe) {
            LOG.error(spe.getSystemId() + ":" + spe.getLineNumber() + ": parse error: " + spe.getMessage());

            Exception x = spe;
            if (spe.getException() != null)
                x = spe.getException();
            LOG.info("unexpected exception", x);

        } catch (SAXException se) {
            Exception x = se;
            if (se.getException() != null)
                x = se.getException();
            LOG.error("exception", x);

        } catch (ParserConfigurationException pce) {
            LOG.error("exception", pce);
        }
    }

    /**
     * @param file
     * @param fileConsumer
     * @return true, if file was processed. false, if file locked by another task
     */
    private boolean doWithLockedFile(File file, Consumer<RandomAccessFile> fileConsumer) {

        FileLock fileLock = null;
        try {
            RandomAccessFile t = new RandomAccessFile(file, "rw");
            fileLock = t.getChannel().tryLock();
            if (fileLock != null) {
                fileConsumer.accept(t);

                return true;
            }
        } catch (IOException e) {
            LOG.error("i/o error", e);

        } finally {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException e) {
                    LOG.error("exception", e);
                }
            }
        }
        return false;
    }

    public void saveDatabase(Profile profile) throws IOException, TransformerConfigurationException, SAXException {
        LOG.info("save database");
        profile.getPuzzleDatabase().removeEmptySessions();
        if (profile == Configuration.guestProfile) {
            guestSession = CALCubeTimer.statsModel.getCurrentSession();
        }
        if (statisticsRandomAccessFile == null) {
            LOG.warn("statisticsRandomAccessFile is null");
            return;
        }
        try {
            CALCubeTimer.setWaiting(true);
            statisticsRandomAccessFile.setLength(0);
            StreamResult streamResult = new StreamResult(new RandomOutputStream(statisticsRandomAccessFile));
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            tf.setAttribute("indent-number", Integer.valueOf(4));
            // SAX2.0 ContentHandler.
            TransformerHandler hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "../database.dtd");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            hd.setResult(streamResult);
            hd.startDocument();
            AttributesImpl atts = new AttributesImpl();
            hd.startElement("", "", "database", atts);
            for (PuzzleStatistics ps : profile.getPuzzleDatabase().getPuzzlesStatistics()) {
                //TODO - check if there are 0 sessions here and continue? NOTE: this isn't good enough, as there could be a bunch of empty sessions
                atts.clear();
                atts.addAttribute("", "", "customization", "CDATA", ps.getCustomization());
                hd.startElement("", "", "puzzle", atts);
                for (Session s : ps.toSessionIterable()) {
                    Statistics stats = s.getStatistics();
                    if (stats.getAttemptCount() == 0) //this indicates that the session wasn't started
                        continue;
                    atts.clear();
                    atts.addAttribute("", "", "date", "CDATA", s.toDateString());
                    if (s == CALCubeTimer.statsModel.getCurrentSession())
                        atts.addAttribute("", "", "loadonstartup", "CDATA", "true");
                    hd.startElement("", "", "session", atts);
                    atts.clear();
                    String temp = s.getComment();
                    if (!temp.isEmpty()) {
                        hd.startElement("", "", "comment", atts);
                        char[] chs = temp.toCharArray();
                        hd.characters(chs, 0, chs.length);
                        hd.endElement("", "", "comment");
                    }
                    for (int ch = 0; ch < stats.getAttemptCount(); ch++) {
                        SolveTime st = stats.get(ch);
                        atts.clear();
                        hd.startElement("", "", "solve", atts);
                        char[] chs = st.toExternalizableString().toCharArray();
                        hd.characters(chs, 0, chs.length);
                        temp = st.getComment();
                        if (!temp.isEmpty()) {
                            atts.clear();
                            hd.startElement("", "", "comment", atts);
                            chs = temp.toCharArray();
                            hd.characters(chs, 0, chs.length);
                            hd.endElement("", "", "comment");
                        }
                        temp = st.toSplitsString();
                        if (!temp.isEmpty()) {
                            atts.clear();
                            hd.startElement("", "", "splits", atts);
                            chs = temp.toCharArray();
                            hd.characters(chs, 0, chs.length);
                            hd.endElement("", "", "splits");
                        }
                        temp = st.getScramble();
                        if (!temp.isEmpty()) {
                            atts.clear();
                            hd.startElement("", "", "scramble", atts);
                            chs = temp.toCharArray();
                            hd.characters(chs, 0, chs.length);
                            hd.endElement("", "", "scramble");
                        }

                        hd.endElement("", "", "solve");
                    }
                    hd.endElement("", "", "session");
                }
                hd.endElement("", "", "puzzle");
            }
            hd.endElement("", "", "database");
            hd.endDocument();
        } finally {
            CALCubeTimer.setWaiting(false);
        }
        closeStatFileAndReleaseLock();
    }

    public void commitRename(Profile profile) {
        String newName = profile.getNewName();
        File newDir = getDirectory(newName);

        File oldConfig = getConfiguration(newDir, profile.getName());
        File oldStats = getStatistics(newDir, profile.getName());

        File configuration = getConfiguration(newDir, newName);
        File statistics = getStatistics(newDir, newName);

        boolean currentProfile = (statisticsRandomAccessFile != null);
        if (currentProfile) {
            closeStatFileAndReleaseLock();
        }
        profile.getDirectory().renameTo(newDir);
        profile.setDirectory(newDir);

        oldConfig.renameTo(configuration);
        oldStats.renameTo(statistics);

        if (currentProfile) {
            openAndLockStatFile(statistics);
        }

        profiles.remove(profile.getName());
        profile.setName(newName);
        profiles.put(profile.getName(), profile);
    }

    private void openAndLockStatFile(File statistics) {
        RandomAccessFile t = null;
        try {
            t = new RandomAccessFile(statistics, "rw");
            t.getChannel().tryLock();
        } catch (IOException e) {
            LOG.info("unexpected exception", e);
        } finally {
            statisticsRandomAccessFile = t;
        }
    }

    private void closeStatFileAndReleaseLock() {
        try {
            statisticsRandomAccessFile.close();
        } catch (IOException e) {
            LOG.info("unexpected exception", e);
        } finally {
            statisticsRandomAccessFile = null;
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

    //I can't believe I had to create these two silly little classses
    private static class RandomInputStream extends InputStream {
        private RandomAccessFile raf;

        public RandomInputStream(RandomAccessFile raf) {
            this.raf = raf;
        }

        public int read() throws IOException {
            return raf.read();
        }
    }

    //this is apparently breaking indenting
    private static class RandomOutputStream extends OutputStream {
        private RandomAccessFile raf;

        public RandomOutputStream(RandomAccessFile raf) {
            this.raf = raf;
        }

        public void write(int b) throws IOException {
            raf.write(b);
        }
    }

}
