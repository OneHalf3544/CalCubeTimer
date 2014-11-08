package net.gnehzr.cct.statistics;

import com.google.common.base.Throwables;
import net.gnehzr.cct.main.CALCubeTimer;
import org.apache.log4j.Logger;
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

public class ProfileSerializer {

    private static final Logger LOG = Logger.getLogger(ProfileSerializer.class);

    final ProfileDao profileDao;

    public ProfileSerializer(ProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    void parseBySaxHandler(DefaultHandler parseLogic, RandomInputStream inputStream) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, parseLogic);

        } catch (IOException e) {
            throw Throwables.propagate(e);

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

    void writeStatisticByPuzzle(TransformerHandler hd, AttributesImpl atts, PuzzleStatistics ps) throws SAXException {
        //TODO - check if there are 0 sessions here and continue? NOTE: this isn't good enough, as there could be a bunch of empty sessions
        atts.clear();
        atts.addAttribute("", "", "customization", "CDATA", ps.getCustomization());
        hd.startElement("", "", "puzzle", atts);
        for (Session s : ps.toSessionIterable()) {
            writeSessionStatistic(hd, atts, s);
        }
        hd.endElement("", "", "puzzle");
    }

    void writeSessionStatistic(TransformerHandler hd, AttributesImpl atts, Session s) throws SAXException {
        Statistics stats = s.getStatistics();
        if (stats.getAttemptCount() == 0) { //this indicates that the session wasn't started
            return;
        }
        atts.clear();
        atts.addAttribute("", "", "date", "CDATA", s.toDateString());
        if (s == CALCubeTimer.statsModel.getCurrentSession()) {
            atts.addAttribute("", "", "loadonstartup", "CDATA", "true");
        }
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

    TransformerHandler createTransformer() throws TransformerConfigurationException {
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        tf.setAttribute("indent-number", Integer.valueOf(4));
        // SAX2.0 ContentHandler.
        TransformerHandler hd = tf.newTransformerHandler();
        Transformer serializer = hd.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "../database.dtd");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        return hd;
    }

    void writeStatisticFile(Profile profile) throws TransformerConfigurationException, IOException, SAXException {
        TransformerHandler hd = createTransformer();

        profile.getStatisticsRandomAccessFile().setLength(0);
        StreamResult streamResult = new StreamResult(new RandomOutputStream(profile.getStatisticsRandomAccessFile()));

        hd.setResult(streamResult);
        hd.startDocument();
        AttributesImpl atts = new AttributesImpl();
        hd.startElement("", "", "database", atts);
        for (PuzzleStatistics ps : profile.getPuzzleDatabase().getPuzzlesStatistics()) {
            writeStatisticByPuzzle(hd, atts, ps);
        }
        hd.endElement("", "", "database");
        hd.endDocument();
    }

    void doInWaitingState(Runnable runnable) {
        try {
            CALCubeTimer.setWaiting(true);
            runnable.run();

        } finally {
            CALCubeTimer.setWaiting(false);
        }
    }

    static class RandomInputStream extends InputStream {
        private RandomAccessFile raf;

        public RandomInputStream(RandomAccessFile raf) {
            this.raf = raf;
        }

        public int read() throws IOException {
            return raf.read();
        }
    }//this is apparently breaking indenting

    static class RandomOutputStream extends OutputStream {
        private RandomAccessFile raf;

        public RandomOutputStream(RandomAccessFile raf) {
            this.raf = raf;
        }

        public void write(int b) throws IOException {
            raf.write(b);
        }
    }
}