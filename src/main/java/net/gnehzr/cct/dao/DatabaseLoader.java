package net.gnehzr.cct.dao;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.statistics.*;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;

/**
* <p>
* <p>
* Created: 08.11.2014 1:19
* <p>
*
* @author OneHalf
*/
public class DatabaseLoader extends DefaultHandler {

    private static final Logger LOG = Logger.getLogger(DatabaseLoader.class);

    private Profile profile;
    private final Configuration configuration;
    private final StatisticsTableModel statsModel;
    private final ScramblePluginManager scramblePluginManager;

    private int level = 0;
    private String customization;
    private String seshCommentOrSolveTime;
    private Session session;
    private Solution solve;
    private String solveCommentOrScrambleOrSplits;

    public DatabaseLoader(Profile profile, Configuration configuration, StatisticsTableModel statsModel, ScramblePluginManager scramblePluginManager) {
        this.profile = profile;
        this.configuration = configuration;
        this.statsModel = statsModel;
        this.scramblePluginManager = scramblePluginManager;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        return new InputSource(new FileInputStream(configuration.getDatabaseDTD()));
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.equalsIgnoreCase("database")) {
            if (level != 0) {
                throw new SAXException("Root element must be database tag.");
            }
        } else if (name.equalsIgnoreCase("puzzle")) {
            if (level != 1) {
                throw new SAXException("1st level expected for puzzle tag.");
            }
            customization = attributes.getValue("customization");
            if (customization == null) {
                throw new SAXException("Customization attribute needed for puzzle tag.");
            }
        } else if (name.equalsIgnoreCase("session")) {
            if (level != 2)
                throw new SAXException("2nd level expected for session tag.");
            try {
                session = new Session(LocalDateTime.parse(attributes.getValue("date"), configuration.getDateFormat()), configuration, scramblePluginManager, statsModel);
                profile.getPuzzleDatabase().getPuzzleStatistics(customization).addSession(session, profile);
            } catch (DateTimeParseException e) {
                throw new SAXException(e);
            }
            if (Boolean.parseBoolean(attributes.getValue("loadonstartup")))
                statsModel.setSession(session);
        } else if (name.equalsIgnoreCase("solve")) {
            if (level != 3)
                throw new SAXException("3rd level expected for solve tag.");
            solve = new Solution(SolveTime.NULL_TIME, null);
            seshCommentOrSolveTime = "";
        } else if (name.equalsIgnoreCase("comment")) {
            if (level == 3)
                seshCommentOrSolveTime = "";
            else if (level == 4)
                solveCommentOrScrambleOrSplits = "";
            else
                throw new SAXException("3rd or 4th level expected for " + name + " tag.");
        } else if (name.equalsIgnoreCase("scramble")) {
            if (level == 4)
                solveCommentOrScrambleOrSplits = "";
            else
                throw new SAXException("4th level expected for " + name + " tag.");
        } else if (name.equalsIgnoreCase("splits")) {
            if (level == 4)
                solveCommentOrScrambleOrSplits = "";
            else
                throw new SAXException("4th level expected for " + name + " tag.");
        } else {
            throw new SAXException("Unexpected element encountered: " + name);
        }

        level++;
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        level--;

        if (name.equalsIgnoreCase("solve")) {
            try {
                SolveTime solveTime = SolveTime.parseTime(seshCommentOrSolveTime);
                solve.setTime(solveTime);

                session.getStatistics().add(solve);
            } catch (Exception e) {
                throw new SAXException("Unable to parse time: " + seshCommentOrSolveTime + " " + e.toString(), e);
            }
        } else if (name.equalsIgnoreCase("comment")) {
            if (level == 3) {
                session.setComment(seshCommentOrSolveTime);
            } else if (level == 4) {
                solve.getTime().setComment(solveCommentOrScrambleOrSplits);
            }
        } else if (name.equalsIgnoreCase("scramble")) {
            try {
                ScrambleVariation variation = scramblePluginManager.getBestMatchVariation(customization);
                solve.setScramble(variation.getPlugin().importScramble(variation.withoutLength(), solveCommentOrScrambleOrSplits, "", Collections.<String>emptyList()));
            } catch (InvalidScrambleException e) {
                LOG.warn("wrong scramble: " + solveCommentOrScrambleOrSplits, e);
            }
        } else if (name.equalsIgnoreCase("splits"))
            solve.setSplitsFromString(solveCommentOrScrambleOrSplits);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (level) {
            case 4: //solvetime or session comment
                seshCommentOrSolveTime += new String(ch, start, length);
                break;
            case 5: //comment or scramble or splits
                solveCommentOrScrambleOrSplits += new String(ch, start, length);
                break;
        }
    }
}
