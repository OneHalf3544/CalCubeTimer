package net.gnehzr.cct.statistics;

import net.gnehzr.cct.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

/**
 * <p>
 * <p>
 * Created: 11.10.2015 21:38
 * <p>
 *
 * @author OneHalf
 */
public class RollingAverage implements Comparable<RollingAverage> {

    public static final RollingAverage NOT_AVAILABLE = new RollingAverage(
            Collections.<Solution>emptyList(), 0, 0, false);

    private final int toIndex;
    private final int count;
    private final SolveTime worstTime;
    private final SolveTime bestTime;
    private final List<Solution> solutions;
    private final SolveTime average;
    private final boolean trimmed;
    private final SolveTime standartDeviation;

    public RollingAverage(List<Solution> solutions, int toIndex, int count, boolean trimmed) {
        checkArgument(toIndex >= 0);
        checkArgument(count >= 0);

        this.solutions = solutions;
        this.toIndex = toIndex;
        this.count = count;
        this.trimmed = trimmed;
        worstTime = solutions.stream().max(comparing(Solution::getTime)).map(Solution::getTime).orElse(SolveTime.BEST);
        bestTime = solutions.stream().min(comparing(Solution::getTime)).map(Solution::getTime).orElse(SolveTime.WORST);
        average = calculateAverage(solutions, trimmed);
        this.standartDeviation = calculateRSD();
    }

    private SolveTime calculateAverage(List<Solution> solutions, boolean trimmed) {
        if (count == 0 || (trimmed && count <= 2 )) {
            return SolveTime.NA;
        }
        return SolveTime.divide(
                solutions.stream()
                        .map(Solution::getTime)
                        .filter(t -> !this.trimmed || (t != bestTime && t != worstTime))
                        .reduce((a, b) -> SolveTime.sum(a, b))
                        .orElse(SolveTime.NA),
                trimmed ? count - 2 : count);
    }

    public static RollingAverage create(RollingAverageOf rollingAverageOf,
                                        Session session, int toIndex, int count) {
        if (toIndex < count) {
            return NOT_AVAILABLE;
        }
        return new RollingAverage(Seq.iterate(toIndex - count, index -> index + 1)
                .limit(count)
                .filter(i -> i < session.getAttemptsCount())
                .map(session::getSolution)
                .toList(),
                toIndex, count, session.getPuzzleType().isTrimmed(rollingAverageOf));
    }

    public int getToIndex() {
        return toIndex;
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
        if (average.isInfiniteTime()) {
            return SolveTime.NA.toString();
        }
        return solutions.stream()
                .map(Solution::getTime)
                .map(t -> t == getBestTime() || t == getWorstTime() ? "(" + t + ")" : t)
                .map(Object::toString)
                .collect(joining(", "));
    }

    public SolveTime getStandartDeviation() {
        return standartDeviation;
    }

    private SolveTime calculateRSD() {
        if (average == SolveTime.NA) {
            return SolveTime.NA;
        }
        long deviation = 0;
        for (Solution solution : solutions) {
            if(solution.getTime() != bestTime && solution.getTime() != worstTime) {
                SolveTime diff = SolveTime.substruct(solution.getTime(), average);
                deviation += pow2(diff.getTime().toMillis());
            }
        }
        int count2 = trimmed ? count - 2 : count;
        return new SolveTime(Math.sqrt(deviation / (double) count2) / 1000.0);
    }

    private long pow2(long l) {
        return l * l;
    }

    @Override
    public int compareTo(@NotNull RollingAverage other) {
        if (getAverage().better(other.getAverage())) {
            return -1;
        }

        if (!Objects.equals(getAverage(), other.getAverage())) {
            return 1;
        }

        if (getBestTime().better(other.getBestTime())) {
            return -1;
        }

        if (!Objects.equals(getBestTime(), other.getBestTime())) {
            return 1;
        }

        if (getWorstTime().better(other.getWorstTime())) {
            return -1;
        }
        return 0;
    }

    public boolean better(RollingAverage anotherAverage) {
        return Utils.lessThan(this, anotherAverage);
    }

    @Override
    public String toString() {
        return toTerseString();
    }
}
