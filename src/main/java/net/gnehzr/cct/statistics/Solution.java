package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.dao.SolutionEntity;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
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
public class Solution implements Commentable {

    private static final Logger LOG = LogManager.getLogger(Solution.class);

    private Long solutionId;

    @NotNull
    private SolveTime solveTime;

    @NotNull
    private ScrambleString scrambleString;

    private List<SolveTime> splits = ImmutableList.of();

    private String comment = "";

    public Solution(@NotNull SolveTime time, @NotNull ScrambleString scrambleString) {
        this.solveTime = Objects.requireNonNull(time);
        this.scrambleString = Objects.requireNonNull(scrambleString);
    }

    public Solution(@NotNull TimerState time, @NotNull ScrambleString scrambleString, List<SolveTime> splits) {
        this.solveTime = new SolveTime(time.getTime());
        this.scrambleString = scrambleString;
        this.splits = splits;
    }

    public Long getSolutionId() {
        return solutionId;
    }

    public void setSolutionId(Long solutionId) {
        this.solutionId = solutionId;
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

    public void setSolveTime(@NotNull SolveTime solveTime) {
        this.solveTime = solveTime;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "Solution{" + solveTime  + ", " + scrambleString + "}";
    }

    public SolutionEntity toEntity(SessionEntity session) {
        return new SolutionEntity()
                .withId(solutionId)
                .withSession(session)
                .withComment(getComment())
                .withScramble(getScrambleString().getScramble())
                .withSolveStart(LocalDateTime.now())
                .withSolveTime(getTime().getTime())
                // todo .withSplits(getSplits())
                ;
    }

    public void setScrambleString(@NotNull ScrambleString scrambleString) {
        this.scrambleString = scrambleString;
    }
}
