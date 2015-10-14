package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static net.gnehzr.cct.statistics.RollingAverageOf.OF_12;
import static net.gnehzr.cct.statistics.RollingAverageOf.OF_5;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * <p>
 * <p>
 * Created: 14.10.2015 0:34
 * <p>
 *
 * @author OneHalf
 */
public class SessionTest {

    private PuzzleType puzzleType;
    private Configuration configuration;
    private ScrambleSettings variation;

    @BeforeClass
    public void setUpClass() {
        configuration = mock(Configuration.class);
        puzzleType = mock(PuzzleType.class);
        when(puzzleType.isTrimmed(OF_5)).thenReturn(true);
        when(puzzleType.getRASize(OF_5)).thenReturn(5);
        when(puzzleType.isTrimmed(OF_12)).thenReturn(false);
        when(puzzleType.getRASize(OF_12)).thenReturn(12);

        variation = new ScrambleSettings(configuration, mock(ScramblePluginManager.class), "U D", 25, null);
    }

    @Test
    public void testSessionAverages() {
        Session session = createSessionWithSolutions("1.23", "3.21", "3.21", "5.74", "123.21", "121.29");

        assertEquals(session.getAttemptsCount(), 6);
        assertEquals(session.getSolution(0).getTime(), new SolveTime("1.23"));
        assertEquals(session.getSolution(5).getTime(), new SolveTime("2:01.29"));
    }

    @Test
    public void testSessionSolutions() {
        Session session = createSessionWithSolutions("1.23", "3.21", "3.21", "5.74", "123.21", "121.29");

        assertEquals(session.getAttemptsCount(), 6);
        assertEquals(session.getRollingAverageForWholeSession().getBestTime(), new SolveTime("1.23"));
        assertEquals(session.getRollingAverage(OF_5, 5, 0), RollingAverage.NOT_AVAILABLE);
        assertEquals(session.getRollingAverage(OF_5, 5, 5).getBestTime(), new SolveTime("1.23"));
        assertEquals(session.getRollingAverage(OF_5, 5, 5).getAverage(), new SolveTime("4.05"));
        assertEquals(session.getRollingAverage(OF_12, 5, 5).getAverage(), new SolveTime("27.32"));
        assertEquals(session.getRollingAverage(OF_5, 5, 6).toTerseString(), "(3.21), 3.21, 5.74, (2:03.21), 2:01.29");
    }

    @Test
    public void testGetValueAt() {
        Session session = createSessionWithSolutions("1.23", "3.21", "3.21", "5.74", "123.21", "121.29");

        assertEquals(session.getStatistics().getRA(0, OF_5), RollingAverage.NOT_AVAILABLE);

        RollingAverage lastRollingAverage = session.getStatistics().getRA(5, OF_5);

        assertEquals(lastRollingAverage, session.getStatistics().getCurrentRollingAverage(OF_5));
        assertEquals(lastRollingAverage.getAverage(), new SolveTime("4.05"));
    }

    private Session createSessionWithSolutions(String... time) {
        Session session = new Session(LocalDateTime.now(), configuration, puzzleType);
        for (String s : time) {
            session.addSolution(new Solution(new SolveTime(s), createScrambleString()));
        }
        assertEquals(session.getAttemptsCount(), time.length);
        return session;
    }

    @NotNull
    private ScrambleString createScrambleString() {
        return new ScrambleString(puzzleType, "R U R' U'", false, variation, mock(ScramblePlugin.class), "Ля-ля-ля");
    }
}