package net.gnehzr.cct.main;

import com.google.common.base.Throwables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScrambleListHolder;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.SolveTime;
import org.hamcrest.number.OrderingComparison;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class SolvingProcessTest {

    private static Instant testCurrentTime = Instant.now();
    private SolvingProcess instance = createProcess();

    @Test
    public void testCanStartProcess() throws Exception {
        assertFalse(instance.canStartProcess());
        shiftCurrentTime(1, ChronoUnit.SECONDS);
        assertTrue(instance.canStartProcess());
    }

    @Test(dependsOnMethods = "testCanStartProcess")
    public void testStartProcess() throws Exception {
        instance.startProcess();
        assertTrue(instance.isRunning());
        assertTrue(instance.isInspecting());
    }

    @Test(dependsOnMethods = "testStartProcess")
    public void testGetInspectionState() throws Exception {
        shiftCurrentTime(1, ChronoUnit.SECONDS);

        InspectionState inspectionState = instance.getInspectionState().get();
        assertThat(inspectionState.getElapsedTime(), lessThanOrEqualTo(Duration.ofSeconds(2)));
        assertThat(inspectionState.getRemainingTime(), lessThanOrEqualTo(Duration.ofSeconds(14)));
        assertFalse(inspectionState.isPenalty());
        assertFalse(inspectionState.isDisqualification());
    }

    private void shiftCurrentTime(int amountToAdd, ChronoUnit unit) {
        testCurrentTime = testCurrentTime.plus(amountToAdd, unit);
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    @Test(dependsOnMethods = "testGetInspectionState")
    public void testGetTimerStateWhenInspecting() throws Exception {
        TimerState timerState = instance.getTimerState();
        assertTrue(timerState.isInspecting());
        assertThat(timerState.getTime(), OrderingComparison.greaterThanOrEqualTo(Duration.ofSeconds(1)));
        assertThat(timerState.getTime(), OrderingComparison.lessThanOrEqualTo(Duration.ofSeconds(2)));
    }

    @Test(dependsOnMethods = "testGetTimerStateWhenInspecting")
    public void testSetInspectionPenalty() throws Exception {
        shiftCurrentTime(14, ChronoUnit.SECONDS);

        InspectionState inspectionState = instance.getInspectionState().get();
        assertTrue(inspectionState.isPenalty());
        assertFalse(inspectionState.isDisqualification());
    }

    @Test(dependsOnMethods = "testSetInspectionPenalty")
    public void testStartSolving() throws Exception {
        instance.startSolving();
        shiftCurrentTime(2, ChronoUnit.SECONDS);

        assertTrue(instance.isRunning());
        assertTrue(instance.isSolving());
        assertFalse(instance.isInspecting());
    }

    @Test(dependsOnMethods = "testStartSolving")
    public void testGetTimerStateWhenSolving() throws Exception {
        TimerState timerState = instance.getTimerState();
        assertFalse(timerState.isInspecting());
        assertEquals(timerState.getTime(), Duration.ofSeconds(2));
    }

    @Test(dependsOnMethods = "testGetTimerStateWhenSolving")
    public void testAddSplit() throws Exception {
        shiftCurrentTime(4, ChronoUnit.SECONDS);
        instance.timeSplit();

        shiftCurrentTime(3, ChronoUnit.SECONDS);
        instance.timeSplit();

        shiftCurrentTime(100, ChronoUnit.MILLIS);
        instance.timeSplit();

        shiftCurrentTime(2, ChronoUnit.SECONDS);
        instance.timeSplit();

        List<SolveTime> splits = instance.getSplits();
        assertThat(splits, hasSize(3));
        assertThat(splits.get(0), is(new SolveTime("6.00")));
        assertThat(splits.get(1), is(new SolveTime("9.00")));
        assertThat(splits.get(2), is(new SolveTime("11.10")));
    }

    @Test(dependsOnMethods = "testAddSplit")
    public void testTimerLabelTextValue() throws Exception {
        shiftCurrentTime(5, ChronoUnit.SECONDS);

        String lastTimerString = ((TimerLabelsHolderMock) instance.getTimerLabelsHolder()).getLastTimerString();
        assertEquals(lastTimerString, "16.10");
    }

    @Test(dependsOnMethods = "testTimerLabelTextValue")
    public void testSolvingFinished() throws Exception {
        assert instance.isRunning();

        TimerState timerState = instance.getTimerState();
        instance.solvingFinished();
        shiftCurrentTime(1, ChronoUnit.SECONDS);

        assertFalse(instance.isRunning());
        assertEquals(timerState, instance.getTimerState());
    }

    @Test(dependsOnMethods = "testSolvingFinished")
    public void testResetProcess() throws Exception {
        instance.resetProcess();

        assertEquals(((TimerLabelsHolderMock) instance.getTimerLabelsHolder()).lastTimerString, "0.00");
    }

    private static SolvingProcess createProcess() {
        ScrambleListHolder scrambleListHolder = mock(ScrambleListHolder.class);
        ScrambleString scramble = new ScrambleString(mock(PuzzleType.class), "R U", false, null, null, "");
        when(scrambleListHolder.getCurrentScramble()).thenReturn(scramble);

        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(VariableKey.COMPETITION_INSPECTION)).thenReturn(Boolean.TRUE);
        when(configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)).thenReturn(1.5);

        SolvingProcess solvingProcess = new SolvingProcess(
                mock(NumberSpeaker.class),
                scrambleListHolder,
                configuration){
            @Override
            Instant now() {
                return testCurrentTime;
            }
        };
        solvingProcess.setSolvingProcessListener(mock(SolvingProcessListener.class));
        solvingProcess.setTimerLabelsHolder(new TimerLabelsHolderMock());
        return solvingProcess;
    }

    private static class TimerLabelsHolderMock extends TimerLabelsHolder {
        private String lastTimerString;

        @Override
        public void refreshTimer(String s) {
            super.refreshTimer(s);
            this.lastTimerString = s;
        }

        public String getLastTimerString() {
            return lastTimerString;
        }

        @Override
        public void refreshDisplay(TimerState newTime) {
            lastTimerString = newTime.toString();
        }
    }
}