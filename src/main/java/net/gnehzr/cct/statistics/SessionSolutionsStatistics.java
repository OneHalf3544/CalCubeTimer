package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.gnehzr.cct.statistics.SolveTime.*;

public class SessionSolutionsStatistics {

    public enum AverageType {
        CURRENT_ROLLING_AVERAGE("Statistics.currentaverage", VariableKey.CURRENT_AVERAGE_STATISTICS),
        BEST_ROLLING_AVERAGE("Statistics.bestRA", VariableKey.BEST_RA_STATISTICS),
        SESSION_AVERAGE("Statistics.sessionAverage", VariableKey.SESSION_STATISTICS),
        ;


        private final String stringCode;
        private final VariableKey<String> configurationKey;

        AverageType(String stringCode, VariableKey<String> configurationKey) {
            this.stringCode = stringCode;
            this.configurationKey = configurationKey;
        }

        @Override
        public String toString() {
            return StringAccessor.getString(stringCode);
        }

        public VariableKey<String> getConfKey() {
            return configurationKey;
        }
    }

    private final Session session;

    private final Map<RollingAverageOf, List<RollingAverage>> averages;

    private final Map<RollingAverageOf, RollingAverage> bestRollingAverages = new EnumMap<>(RollingAverageOf.class);
    private final Map<RollingAverageOf, RollingAverage> worstRollingAverages = new EnumMap<>(RollingAverageOf.class);

    private RollingAverage wholeSessionAverage = RollingAverage.NOT_AVAILABLE;
    private RollingAverage previousWholeSessionAverage = RollingAverage.NOT_AVAILABLE;

    private SolveCounter solveCounter;

    public SessionSolutionsStatistics(Session session) {
        this.session = session;

        averages = new EnumMap<>(RollingAverageOf.class);

        for (RollingAverageOf i : RollingAverageOf.values()) {
            averages.put(i, new ArrayList<>());
        }

        solveCounter = new SolveCounter();
    }

    public Session getSession() {
        return session;
    }

    public void setSolveTypes(int row, List<SolveType> newTypes, Runnable notifier) {
        SolveTime selectedSolve = session.getSolution(row).getTime();
        selectedSolve.setTypes(newTypes);
        refresh(notifier);
    }

    private void calculateCurrentAverage(@NotNull RollingAverageOf k) {
        RollingAverage currentRollingAverage = session.getRollingAverage(k, getRASize(k), getRollingAverageList(k).size());

        RollingAverage bestRollingAverage = getBestAverage(k);
        getRollingAverageList(k).add(currentRollingAverage);

        if (currentRollingAverage.getAverage().isInfiniteTime()) {
            return;
        }

        if (bestRollingAverage == RollingAverage.NOT_AVAILABLE) {
            bestRollingAverages.put(k, currentRollingAverage);
            return;
        }

        if (currentRollingAverage.better(bestRollingAverage)) {
            bestRollingAverages.put(k, currentRollingAverage);
        }

        // ---------------
        RollingAverage worstRollingAverage = getBestAverage(k);
        if (worstRollingAverage == null) {
            worstRollingAverages.put(k, currentRollingAverage);
            return;
        }

        if (currentRollingAverage.getAverage().isInfiniteTime()) {
            return;
        }

        if (worstRollingAverage.better(currentRollingAverage)) {
            worstRollingAverages.put(k, currentRollingAverage);
        }
    }

    void refresh(Runnable notifier) {
        wholeSessionAverage = RollingAverage.NOT_AVAILABLE;
        previousWholeSessionAverage = RollingAverage.NOT_AVAILABLE;

        averages.values().forEach(List<RollingAverage>::clear);

        for (Solution solution : session.getSolutionList()) {
            for (RollingAverageOf averageOf : RollingAverageOf.values()) {
                if (getRollingAverageList(averageOf).size() + 1 >= getRASize(averageOf)) {
                    calculateCurrentAverage(averageOf);
                } else {
                    getRollingAverageList(averageOf).add(RollingAverage.NOT_AVAILABLE);
                }
            }

            if (!solution.getTime().isInfiniteTime()) {
                previousWholeSessionAverage = wholeSessionAverage;
                wholeSessionAverage = session.getRollingAverageForWholeSession();
            }
        }

        solveCounter = SolveCounter.fromSolutions(session.getSolutionList());

        notifier.run();
    }

    public int getRASize(RollingAverageOf num) {
        return session.getPuzzleType().getRASize(num);
    }

    public RollingAverage getRA(int toIndex, RollingAverageOf whichRA) {
        return getRollingAverageList(whichRA).get(toIndex);
    }

    public boolean isValid(AverageType type, RollingAverageOf num) {
        switch (type) {
            case BEST_ROLLING_AVERAGE:
                return getBestAverage(num).getAverage().isDefined();

            case CURRENT_ROLLING_AVERAGE:
                return getCurrentRollingAverage(num).getAverage().isDefined();

            case SESSION_AVERAGE:
                return getWholeSessionAverage().getAverage().isDefined();

            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    private RollingAverage getRollingAverage(RollingAverageOf ra, AverageType type) {
        switch (type) {
            case SESSION_AVERAGE:
                return session.getRollingAverageForWholeSession();
            case CURRENT_ROLLING_AVERAGE:
                return getCurrentRollingAverage(ra);
            case BEST_ROLLING_AVERAGE:
                return getBestAverage(ra);
            default:
                throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    public boolean containsTime(int indexOfSolve, AverageType type, RollingAverageOf num) {
        RollingAverage rollingAverage = type == AverageType.CURRENT_ROLLING_AVERAGE ? getCurrentRollingAverage(num) : getBestAverage(num);
        return indexOfSolve >= rollingAverage.getToIndex() - rollingAverage.getCount() && indexOfSolve < rollingAverage.getToIndex();
    }

    public String toStatsString(AverageType type, boolean showSplits, RollingAverageOf num) {
        RollingAverage times = getRollingAverage(num, type);
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (Solution solution : times.getSolutions()) {
            appendSolutionToStatString(showSplits, times, result, ++i, solution);
        }
        return result.toString();
    }

    private void appendSolutionToStatString(boolean showSplits, RollingAverage times, StringBuilder result, int i, Solution solution) {
        boolean parenthesis = solution.getTime() == times.getBestTime() || solution.getTime() == times.getWorstTime();

        result.append(i).append(".\t");

        if (parenthesis) {
            result.append(solution.getTime().toStringWithParenthesis());
        } else {
            result.append(solution.getTime().toString());
        }
        result.append("\t");

        result.append(solution.getScrambleString());
        if (showSplits) {
            result.append(StringAccessor.getString("Statistics.splits")).append(solution.toSplitsString());
        }

        if (!solution.getComment().isEmpty()) {
            result.append('\t').append(solution.getComment());
        }
        result.append("\n");
    }

    public String toTerseString(AverageType type, RollingAverageOf num, boolean showIncomplete) {
        if (type == AverageType.SESSION_AVERAGE) {
            return wholeSessionAverage.toTerseString();
        }
        RollingAverage list = getRollingAverage(num, type);
        if (list.getSolutions().size() != getRASize(num) && !showIncomplete) {
            return "N/A";
        }
        return list.toTerseString();
    }

    public int getIndexOfBestRA(RollingAverageOf num) {
        return bestRollingAverages.getOrDefault(num, RollingAverage.NOT_AVAILABLE).getToIndex();
    }

    public SolveCounter getSolveCounter() {
        return solveCounter;
    }

    public SolveTime getSessionAvg() {
        return wholeSessionAverage.getAverage();
    }

    public SolveTime getTime(int n) {
        if (session.getAttemptsCount() == 0)
            return NOT_AVAILABLE;

        return Utils.getByCircularIndex(n, session.getSolutionList()).getTime();
    }

    public SolveTime getAverage(int n, RollingAverageOf num) {
        if (getRollingAverageList(num).isEmpty()) {
            return NOT_AVAILABLE;
        }
        return Utils.getByCircularIndex(n, getRollingAverageList(num)).getAverage();
    }


    public SolveTime getStandardDeviation(int n, RollingAverageOf num) {
        if (getRollingAverageList(num).isEmpty()) {
            return NOT_AVAILABLE;
        }
        return Utils.getByCircularIndex(n, getRollingAverageList(num)).getStandartDeviation();
    }

    public SolveTime getSessionStandardDeviation(int n) {
        // TODO n
        return getWholeSessionAverage().getStandartDeviation();
    }

    public SolveTime getProgressTime() {
        return substruct(getCurrentTime(), getPreviousTime());
    }

    public SolveTime getProgressAverage(RollingAverageOf num) {
        if(getSolveCounter().getAttemptCount() < 2) {
            return NOT_AVAILABLE;
        }

        SolveTime t1 = getAverage(-1, num);
        SolveTime t2 = getAverage(-2, num);
        if (t1 == NOT_AVAILABLE || t2 == NOT_AVAILABLE) {
            return NOT_AVAILABLE;
        }
        return substruct(t1, t2);
    }

    public SolveTime getProgressSessionAverage() {
        return substruct(wholeSessionAverage.getAverage(), previousWholeSessionAverage.getAverage());
    }

    public SolveTime getProgressSessionStandardDeviation() {
		/*
		return averages.get(num).stream()
                .min(Comparator.comparing(RollingAverage::getStandartDeviation))
				.orElse(RollingAverage.NOT_AVAILABLE);
         */
		if(getSolveCounter().getAttemptCount() < 2) {
            return NOT_AVAILABLE;
        }

		SolveTime t1 = getSessionStandardDeviation(-1);
		SolveTime t2 = getSessionStandardDeviation(-2);
        if (t1 == NOT_AVAILABLE || t2 == NOT_AVAILABLE) {
            return NOT_AVAILABLE;
        }
        return substruct(t1, t2);
    }

    public RollingAverage getBestAverage(RollingAverageOf num) {
        return bestRollingAverages.getOrDefault(num, RollingAverage.NOT_AVAILABLE);
    }

    public RollingAverage getByBestStandardDeviation(RollingAverageOf num) {
        return getRollingAverageList(num).stream()
                .min(Comparator.comparing(RollingAverage::getStandartDeviation))
                .orElse(RollingAverage.NOT_AVAILABLE);
    }

    public SolveTime getBestAverageStandardDeviation(RollingAverageOf num) {
        // todo average
        return getByBestStandardDeviation(num).getStandartDeviation();
    }

    public SolveTime getByWorstStandardDeviation(RollingAverageOf num) {
        return getRollingAverageList(num).stream()
                .max(Comparator.comparing(RollingAverage::getStandartDeviation))
                .orElse(RollingAverage.NOT_AVAILABLE)
                .getStandartDeviation();
    }

    public SolveTime getWorstAverageStandardDeviation(RollingAverageOf num) {
        // todo average
        return getByWorstStandardDeviation(num);
    }

    public SolveTime getCurrentTime() {
        return getTime(-1);
    }

    public RollingAverage getCurrentRollingAverage(RollingAverageOf num) {
        return Utils.getByCircularIndex(-1, getRollingAverageList(num), RollingAverage.NOT_AVAILABLE);
    }

    private List<RollingAverage> getRollingAverageList(RollingAverageOf num) {
        return averages.computeIfAbsent(num, raOf -> new ArrayList<>());
    }

    public SolveTime getCurrentStandardDeviation(RollingAverageOf num) {
        return getStandardDeviation(-1, num);
    }

    public SolveTime getLastStandardDeviation(RollingAverageOf num) {
        return getStandardDeviation(-2, num);
    }

    public SolveTime getPreviousTime() {
        return getTime(-2);
    }

    public RollingAverage getWorstRollingAverage(RollingAverageOf num) {
        return worstRollingAverages.getOrDefault(num, RollingAverage.NOT_AVAILABLE);
    }

    public String getBestAverageList(RollingAverageOf num) {
        return toTerseString(AverageType.BEST_ROLLING_AVERAGE, num, false);
    }

    public String getCurrentAverageList(RollingAverageOf num) {
        return toTerseString(AverageType.CURRENT_ROLLING_AVERAGE, num, false);
    }

    public String getSessionAverageList() {
        return toTerseString(AverageType.SESSION_AVERAGE, null, true);
    }

    public RollingAverage getPreviousRollingAverage(RollingAverageOf averageOf) {
        if (averages.size() < 2) {
            return RollingAverage.NOT_AVAILABLE;
        }
        return Utils.getByCircularIndex(-2, getRollingAverageList(averageOf));
    }

    public RollingAverage getWholeSessionAverage() {
        return wholeSessionAverage;
    }
}
