package net.gnehzr.cct.statistics;

import org.jooq.lambda.Seq;

import java.util.List;

import static java.util.Comparator.comparing;

/**
 * <p>
 * <p>
 * Created: 11.10.2015 21:38
 * <p>
 *
 * @author OneHalf
 */
public class RollingAverage {

    private final int fromIndex;
    private final int count;
    private final SolveTime worstTime;
    private final SolveTime bestTime;
    private final List<Solution> solutions;
    private final SolveTime average;
    private final boolean trimmed;

    public RollingAverage(List<Solution> solutions, int fromIndex, int count, boolean trimmed) {
        this.solutions = solutions;
        this.fromIndex = fromIndex;
        this.count = count;
        this.trimmed = trimmed;
        worstTime = solutions.stream().max(comparing(Solution::getTime)).map(Solution::getTime).orElse(SolveTime.BEST);
        bestTime = solutions.stream().min(comparing(Solution::getTime)).map(Solution::getTime).orElse(SolveTime.WORST);
        average = calculateAverage(solutions);
    }

    private SolveTime calculateAverage(List<Solution> solutions) {
        return SolveTime.divide(
                solutions.stream()
                        .map(Solution::getTime)
                        .filter(t -> !trimmed || (t != bestTime && t != worstTime))
                        .reduce((a, b) -> SolveTime.sum(a, b))
                        .orElse(SolveTime.NA),
                isTrimmed() ? count - 2 : count);
    }

    public static RollingAverage create(SessionPuzzleStatistics.RollingAverageOf rollingAverageOf,
                                        Session session, int fromIndex, int count) {
        return new RollingAverage(Seq.iterate(fromIndex, index -> index + 1)
                .limit(count)
                .filter(i -> i < session.getAttemptsCount())
                .map(session::getSolution)
                .toList(),
                fromIndex, count, session.getPuzzleType().isTrimmed(rollingAverageOf));
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getCount() {
        return count;
    }

    public SolveTime getWorstTime() {
        return worstTime;
    }

    public SolveTime getBestTime() {
        return bestTime;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    public SolveTime getAverage() {
        return average;
    }

    public boolean isTrimmed() {
        return trimmed;
    }

    public SolveCounter getSolveCounter() {
        return SolveCounter.fromSolutions(solutions);
    }

    public String toTerseString() {
        StringBuilder ret = new StringBuilder();
        String nextAppend = "";
        for(Solution next : this.getSolutions()){
            ret.append(nextAppend);
            boolean parens = next.getTime() == this.getBestTime() || next.getTime() == this.getWorstTime();
            if(parens) {
                ret.append("(");
            }
            ret.append(next.getTime().toString());
            if(parens) {
                ret.append(")");
            }
            nextAppend = ", ";
        }
        return ret.toString();
    }
}
