package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 03.02.2015 1:14
 * <p>
 *
 * @author OneHalf
 */
public class Solution extends Commentable {

    private static final Logger LOG = LogManager.getLogger(Solution.class);

    @NotNull
    private final SolveTime solveTime;

    @NotNull
    private final ScrambleString scrambleString;

    private List<SolveTime> splits = ImmutableList.of();

    public Solution(@NotNull SolveTime time, @NotNull ScrambleString scrambleString) {
        this.solveTime = Objects.requireNonNull(time);
        this.scrambleString = Objects.requireNonNull(scrambleString);
    }

    public Solution(TimerState time, ScrambleString scrambleString, List<SolveTime> splits) {
        this(time, scrambleString);
        this.splits = splits;
    }

    private Solution(@NotNull TimerState time, @NotNull ScrambleString scrambleString) {
        this.solveTime = new SolveTime(time.getTime());
        this.scrambleString = scrambleString;
    }

    public ScrambleString getScrambleString() {
        return Objects.requireNonNull(scrambleString);
    }

    public String toSplitsString() {
        return Joiner.on(", ").join(splits);
    }

    public List<SolveTime> getSplits() {
        return splits;
    }

    public SolveTime getTime() {
        return solveTime;
    }

    @Override
    public String toString() {
        return "Solution{" + solveTime  + ", " + scrambleString + "}";
    }
}
