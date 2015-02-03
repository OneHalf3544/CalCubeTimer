package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * <p>
 * Created: 03.02.2015 1:14
 * <p>
 *
 * @author OneHalf
 */
public class Solution extends Commentable {

    private static final Logger LOG = Logger.getLogger(Solution.class);

    private SolveTime solveTime = null;
    private ScrambleString scramble = null;
    //this constructor exists to allow the jtable of times to contain the averages also
    //we need to know the index so we can syntax highlight it
    private int whichRA = -1;
    private List<SolveTime> splits = ImmutableList.of();

    public Solution(SolveTime time, ScrambleString scramble) {
        this.solveTime = time;
        this.scramble = scramble;
    }

    public Solution(double seconds, int whichRA) {
        this.whichRA = whichRA;
        this.solveTime = new SolveTime(seconds);
    }

    public Solution(TimerState time, ScrambleString scramble, List<SolveTime> splits) {
        this(time, scramble);
        this.splits = splits;
    }


    private Solution(TimerState time, ScrambleString scramble) {
        this.solveTime = new SolveTime(time.getTime());
        setScramble(scramble);
    }

    public int getWhichRA() {
        return whichRA;
    }

    public void setScramble(ScrambleString scramble) {
        this.scramble = scramble;
    }

    public ScrambleString getScramble() {
        return scramble == null ? ScramblePluginManager.NULL_CREATED_SCRAMBLE : scramble;
    }

    public String toSplitsString() {
        return Joiner.on(", ").join(splits);
    }

    //this follows the same formatting as the above method spits out
    public void setSplitsFromString(String splitsString) {
        this.splits = new ArrayList<>();
        for(String s : splitsString.split(", *")) {
            try {
                this.splits.add(new SolveTime(s));
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public List<SolveTime> getSplits() {
        return splits;
    }

    public void setTime(SolveTime solveTime) {
        this.solveTime = solveTime;
    }

    public SolveTime getTime() {
        return solveTime;
    }

    @Override
    public String toString() {
        return "Solution{" + solveTime  + ", " + scramble + "}";
    }
}
