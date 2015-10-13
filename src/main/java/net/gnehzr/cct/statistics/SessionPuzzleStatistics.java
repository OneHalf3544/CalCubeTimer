package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;

import java.util.*;

public class SessionPuzzleStatistics {

    public enum AverageType {
        CURRENT_ROLLING_AVERAGE {
            public String toString() {
                return StringAccessor.getString("Statistics.currentaverage");
            }
        },
        BEST_ROLLING_AVERAGE {
            public String toString() {
                return StringAccessor.getString("Statistics.bestRA");
            }
        },
        SESSION_AVERAGE {
            public String toString() {
                return StringAccessor.getString("Statistics.sessionAverage");
            }
        }
    }

    private final Session session;

    private Map<RollingAverageOf, List<RollingAverage>> averages;

    private Map<RollingAverageOf, RollingAverage> bestRollingAverages = new EnumMap<>(RollingAverageOf.class);
    private Map<RollingAverageOf, RollingAverage> worstRollingAverages = new EnumMap<>(RollingAverageOf.class);

    private RollingAverage wholeSessionAverage = RollingAverage.NOT_AVAILABLE;
    private RollingAverage previousWholeSessionAverage = RollingAverage.NOT_AVAILABLE;

    private SolveCounter solveCounter;

    private List<StatisticsUpdateListener> statisticsUpdateListeners;
    public DraggableJTableModel tableListener;


    public SessionPuzzleStatistics(Session session, Configuration configuration) {
        this.session = session;
        configuration.addConfigurationChangeListener(this::onConfigurationChange);

        averages = new EnumMap<>(RollingAverageOf.class);
        bestRollingAverages = new EnumMap<>(RollingAverageOf.class);

        for (RollingAverageOf i : RollingAverageOf.values()) {
            averages.put(i, new ArrayList<>());
        }

        solveCounter = new SolveCounter();

        initialize();

        onConfigurationChange(null);
    }

    private void onConfigurationChange(Profile profile) {
        refresh();
    }

    private void initialize() {
        wholeSessionAverage = RollingAverage.NOT_AVAILABLE;
        previousWholeSessionAverage = RollingAverage.NOT_AVAILABLE;

        this.averages.values().forEach(List<RollingAverage>::clear);

        //zero out solvetype counter
        solveCounter.clear();
    }

    public Session getSession() {
        return session;
    }

    public void clear() {
        initialize();
        notifyListeners();
    }

    public void setStatisticsUpdateListeners(List<StatisticsUpdateListener> listener) {
        statisticsUpdateListeners = listener;
    }


    public void setTableListener(DraggableJTableModel tableListener) {
        this.tableListener = tableListener;
    }

    public void notifyListeners() {
        if (tableListener != null) {
            tableListener.fireTableDataChanged();
        }
        if (statisticsUpdateListeners != null) {
            statisticsUpdateListeners.forEach(e -> e.update(session.getSessionsList()));
        }
    }

    public void setSolveTypes(int row, List<SolveType> newTypes) {
        SolveTime selectedSolve = session.getSolution(row).getTime();
        selectedSolve.setTypes(newTypes);
        refresh();
    }

    private long pow2(long millis) {
        return millis * millis;
    }

    private void calculateCurrentAverage(RollingAverageOf k) {
        RollingAverage currentRollingAverage = getSublist(k, AverageType.CURRENT_ROLLING_AVERAGE);
        RollingAverage bestRollingAverage = bestRollingAverages.get(k);

        averages.get(k).add(currentRollingAverage);

        if (currentRollingAverage.getAverage().isInfiniteTime()) {
            return;
        }

        if (bestRollingAverage == null) {
            bestRollingAverages.put(k, currentRollingAverage);
            return;
        }

        if (currentRollingAverage.better(bestRollingAverage)) {
            bestRollingAverages.put(k, currentRollingAverage);
            return;
        }
    }

    void refresh() {
        initialize();

        solveCounter = SolveCounter.fromSolutions(session.getSolutionList());

        for (Solution solution : session.getSolutionList()) {
            for (RollingAverageOf averageOf : RollingAverageOf.values()) {
                if (solveCounter.getAttemptCount() >= getRASize(averageOf)) {
                    calculateCurrentAverage(averageOf);
                }
            }

            if (!solution.getTime().isInfiniteTime()) {
                previousWholeSessionAverage = wholeSessionAverage;
                wholeSessionAverage = session.getRollingAverageForWholeSession();
            }
        }
        notifyListeners();
    }

    public int getRASize(RollingAverageOf num) {
        return session.getPuzzleType().getRASize(num);
    }

    public RollingAverageTime getRA(int toIndex, RollingAverageOf whichRA) {
        int RAnum = 1 + toIndex - getRASize(whichRA);
        RollingAverage seconds = averages.get(whichRA).get(RAnum);
        return new RollingAverageTime(seconds.getAverage(), whichRA);
    }

    @Deprecated
    public SolveTime average(AverageType type, RollingAverageOf num) {
        SolveTime average;
        switch (type) {
            case SESSION_AVERAGE:
                average = wholeSessionAverage.getAverage();
                break;
            case BEST_ROLLING_AVERAGE:
                average = bestRollingAverages.get(num).getAverage();
                break;
            case CURRENT_ROLLING_AVERAGE:
                average = averages.get(num).get(averages.get(num).size() - 1).getAverage();
                break;
            default:
                return SolveTime.NULL_TIME;
        }

        if (average.isZero() || average == SolveTime.NA) {
            return SolveTime.NULL_TIME;
        }

        return average;
    }

    public boolean isValid(AverageType type, RollingAverageOf num) {
        switch (type) {
            case BEST_ROLLING_AVERAGE:
                return getBestAverage(num).getAverage().isDefined();

            case CURRENT_ROLLING_AVERAGE:
                return getCurrentRollingAverage(num).getAverage().isDefined();

            case SESSION_AVERAGE:
                return getCurrentRollingAverage(num).getAverage().isDefined();

            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    private RollingAverage getSublist(RollingAverageOf ra, int fromIndex) {
        return session.getRollingAverage(ra, fromIndex, getRASize(ra));
    }

    private RollingAverage getSublist(RollingAverageOf ra, AverageType type) {
        if (type == AverageType.SESSION_AVERAGE) {
            return session.getRollingAverageForWholeSession();
        }

        return type == AverageType.CURRENT_ROLLING_AVERAGE
                ? getCurrentRollingAverage(ra)
                : getBestAverage(ra);
    }

    public boolean containsTime(int indexOfSolve, AverageType type, RollingAverageOf num) {
        int averagesLastIndex = averages.get(num).size() - 1;
        RollingAverage rollingAverage = type == AverageType.CURRENT_ROLLING_AVERAGE ? averages.get(num).get(averagesLastIndex) : bestRollingAverages.get(num);

        return indexOfSolve >= rollingAverage.getFromIndex() && indexOfSolve < rollingAverage.getFromIndex() + rollingAverage.getCount();
    }

    public String toStatsString(AverageType type, boolean showSplits, RollingAverageOf num) {
        RollingAverage times = getSublist(num, type);
        StringBuilder ret = new StringBuilder();
        int i = 0;
        for (Solution next : times.getSolutions()) {
            String comment = next.getComment();
            if (!comment.isEmpty())
                comment = "\t" + comment;
            boolean parens = next.getTime() == times.getBestTime() || next.getTime() == times.getWorstTime();

            ret.append(++i).append(".\t");
            if (parens) ret.append("(");
            ret.append(next.getTime().toString());
            if (parens) ret.append(")\t");
            else ret.append("\t");
            ret.append(next.getScrambleString());
            if (showSplits) ret.append(StringAccessor.getString("Statistics.splits")).append(next.toSplitsString());
            ret.append(comment);
            ret.append("\n");
        }
        return ret.toString();
    }

    public String toTerseString(int n, RollingAverageOf num) {
        RollingAverage list = session.getRollingAverage(num, n, getRASize(num));
        if (list.getSolutions().isEmpty()) {
            return "N/A";
        }
        return list.toTerseString();
    }

    public String toTerseString(AverageType type, RollingAverageOf num, boolean showIncomplete) {
        if (type == AverageType.SESSION_AVERAGE) {
            return wholeSessionAverage.toTerseString();
        }
        RollingAverage list = getSublist(num, type);
        if (list.getSolutions().size() != getRASize(num) && !showIncomplete) {
            return "N/A";
        }
        return list.toTerseString();
    }

    private SolveTime bestTimeOfAverage(RollingAverageOf num, int n) {
        return getSublist(num, n).getBestTime();
    }

    private SolveTime worstTimeOfAverage(RollingAverageOf num, int n) {
        return getSublist(num, n).getWorstTime();
    }

    public int getIndexOfBestRA(RollingAverageOf num) {
        return bestRollingAverages.get(num).getFromIndex();
    }

    public SolveCounter getSolveCounter() {
        return solveCounter;
    }

    // access methods
    public SolveTime getSessionAvg() {
        return wholeSessionAverage.getAverage();
    }

    public SolveTime getTime(int n) {
        if (session.getAttemptsCount() == 0)
            return SolveTime.NA;

        return session.getSolution(n).getTime();
    }

    public SolveTime getAverage(int n, RollingAverageOf num) {
        if (averages.get(num).isEmpty()) {
            return SolveTime.NA;
        }
        return averages.get(num).get(n).getAverage();
    }

    public SolveTime getSD(int n, RollingAverageOf num) {
        if (averages.get(num).isEmpty()) {
            return SolveTime.NA;
        }

        return averages.get(num).get(n).getStandartDeviation();
    }

    public SolveTime getWorstTimeOfAverage(int n, RollingAverageOf num) {
        if (averages.get(num).isEmpty()) {
            return SolveTime.NULL_TIME;
        }
        return worstTimeOfAverage(num, n);
    }

    public SolveTime getSessionSD(int n) {
        return getWholeSessionAverage().getStandartDeviation();
    }

    public SolveTime getProgressTime() {
        return SolveTime.substruct(getCurrentTime(), getPreviousTime());
    }

    public SolveTime getProgressAverage(RollingAverageOf num) {
        if (averages.get(num).size() == 0) {
            return SolveTime.NA;
        } else if (averages.get(num).size() == 1) {
            return SolveTime.NA;
        } else {
            SolveTime t1 = getAverage(-1, num);
            if (t1 == SolveTime.NA)
                return SolveTime.NA;
            SolveTime t2 = getAverage(-2, num);
            if (t2 == SolveTime.NA)
                return SolveTime.NA;
            return SolveTime.substruct(t1, t2);
        }
    }

    public SolveTime getProgressSessionAverage() {
        return SolveTime.substruct(wholeSessionAverage.getAverage(), previousWholeSessionAverage.getAverage());
    }

    public SolveTime getProgressSessionSD() {
        // todo
        return SolveTime.NA;
/*		return averages.get(num).stream()
                .min(Comparator.comparing(RollingAverage::getStandartDeviation))
				.orElse(RollingAverage.NOT_AVAILABLE);
		if(sessionSds.size() < 2)
			return SolveTime.NA;

		SolveTime t1 = getSessionSD(-1);
		if(t1 == SolveTime.NA)
			return SolveTime.NA;
		SolveTime t2 = getSessionSD(-2);
		if(t2 == SolveTime.NA)
			return SolveTime.NA;

		return SolveTime.substruct(t1, t2);
		*/
    }

    public RollingAverage getBestAverage(RollingAverageOf num) {
        return bestRollingAverages.getOrDefault(num, RollingAverage.NOT_AVAILABLE);
    }

    public RollingAverage getByBestStandardDeviation(RollingAverageOf num) {
        return averages.get(num).stream()
                .min(Comparator.comparing(RollingAverage::getStandartDeviation))
                .orElse(RollingAverage.NOT_AVAILABLE);
    }

    public SolveTime getBestAverageSD(RollingAverageOf num) {
        // todo average
        return getByBestStandardDeviation(num).getStandartDeviation();
    }

    public SolveTime getWorstTime() {
        return session.getRollingAverageForWholeSession().getWorstTime();
    }

    public RollingAverage getWorstAverage(RollingAverageOf num) {
        return getWorstRollingAverage(num);
    }

    public SolveTime getByWorstStandartDeviation(RollingAverageOf num) {
        return averages.get(num).stream()
                .max(Comparator.comparing(RollingAverage::getStandartDeviation))
                .orElse(RollingAverage.NOT_AVAILABLE)
                .getStandartDeviation();
    }

    public SolveTime getWorstAverageSD(RollingAverageOf num) {
        // todo average
        return getByWorstStandartDeviation(num);
    }

    public SolveTime getCurrentTime() {
        return getTime(-1);
    }

    public RollingAverage getCurrentRollingAverage(RollingAverageOf num) {
        if (averages.getOrDefault(num, Collections.emptyList()).isEmpty()) {
            return RollingAverage.NOT_AVAILABLE;
        }
        return averages.get(num).get(averages.size() - 1);
    }

    public SolveTime getCurrentSD(RollingAverageOf num) {
        return getSD(-1, num);
    }

    public SolveTime getPreviousTime() {
        return getTime(-2);
    }

    public RollingAverage getPreviousAverage(RollingAverageOf num) {
        return getPreviousRollingAverage(num);
    }

    public SolveTime getLastSD(RollingAverageOf num) {
        return getSD(-2, num);
    }

    public SolveTime getBestTimeOfPreviousAverage(RollingAverageOf num) {
        return getPreviousAverage(num).getBestTime();
    }

    public SolveTime getWorstTimeOfPreviousAverage(RollingAverageOf num) {
        return getWorstTimeOfAverage(-2, num);
    }

    public SolveTime getBestTimeOfWorstAverage(RollingAverageOf num) {
        return getWorstRollingAverage(num).getBestTime();
    }

    public SolveTime getWorstTimeOfWorstAverage(RollingAverageOf num) {
        return getWorstRollingAverage(num).getWorstTime();
    }

    private RollingAverage getWorstRollingAverage(RollingAverageOf num) {
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
        if (averages.size() <= 2) {
            return RollingAverage.NOT_AVAILABLE;
        }
        return averages.get(averageOf).get(averages.size() - 2);
    }

    public RollingAverage getWholeSessionAverage() {
        return wholeSessionAverage;
    }
}
