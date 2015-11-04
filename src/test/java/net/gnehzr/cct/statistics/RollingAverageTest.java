package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static net.gnehzr.cct.statistics.RollingAverageOf.OF_12;
import static net.gnehzr.cct.statistics.RollingAverageOf.OF_5;
import static net.gnehzr.cct.statistics.SolveType.DNF;
import static net.gnehzr.cct.statistics.SolveType.PLUS_TWO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * <p>
 * <p>
 * Created: 04.11.2015 20:55
 * <p>
 *
 * @author OneHalf
 */
public class RollingAverageTest {



    private PuzzleType puzzleType;
    private ScrambleSettings variation;

    @BeforeClass
    public void setUpClass() {
        Configuration configuration = mock(Configuration.class);
        puzzleType = mock(PuzzleType.class);
        when(puzzleType.isTrimmed(OF_5)).thenReturn(true);
        when(puzzleType.getRASize(OF_5)).thenReturn(5);
        when(puzzleType.isTrimmed(OF_12)).thenReturn(false);
        when(puzzleType.getRASize(OF_12)).thenReturn(12);

        variation = new ScrambleSettings(configuration, mock(ScramblePluginManager.class), "U D", 25);
    }

    @Test
    public void testRollingAverage() {
        RollingAverage average = createSessionWithSolutions(
                new SolveTime("1.00"),
                new SolveTime("2.00").withTypes(DNF),
                new SolveTime("3.00").withTypes(PLUS_TWO),
                new SolveTime("4.00"),
                new SolveTime("5.00"));

        assertEquals(average.getCount(), 5);
        assertEquals(average.getFirst(), new SolveTime("1"));
        assertEquals(average.getLast(), new SolveTime("5.00"));
        assertEquals(average.getBestTime(), new SolveTime("1.00"));
        assertEquals(average.getWorstTime(), new SolveTime("2.00").withTypes(DNF));
        assertEquals(average.getAverage(), new SolveTime("4.666"));
        assertEquals(average.getSolveCounter().getAttemptCount(), 5);
        assertEquals(average.getSolveCounter().getSolveCount(), 4);
        assertEquals(average.getSolveCounter().getSolveTypeCount(PLUS_TWO), 1);
    }

    @Test
    public void testRollingAverageWith2DNF() {
        RollingAverage average = createSessionWithSolutions(
                new SolveTime("1.00"),
                new SolveTime("2.00").withTypes(DNF),
                new SolveTime("3.00").withTypes(PLUS_TWO),
                new SolveTime("4.00").withTypes(DNF),
                new SolveTime("5.00"));

        assertEquals(average.getBestTime(), new SolveTime("1.00"));
        assertEquals(average.getWorstTime(), new SolveTime("2.00").withTypes(DNF));
        assertEquals(average.getAverage(), SolveTime.WORST);
        assertEquals(average.getSolveCounter().getAttemptCount(), 5);
        assertEquals(average.getSolveCounter().getSolveCount(), 3);
    }

    private RollingAverage createSessionWithSolutions(SolveTime... time) {
        List<Solution> session = new ArrayList<>();
        for (SolveTime s : time) {
            session.add(new Solution(s, createScrambleString()));
        }
        return new RollingAverage(session, 4, 5, true, OF_5);
    }

    @NotNull
    private ScrambleString createScrambleString() {
        return new ScrambleString(puzzleType, "R U R' U'", false, variation, mock(ScramblePlugin.class), "Ля-ля-ля");
    }
}