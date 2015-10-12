package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;

import java.util.*;

public class SessionPuzzleStatistics {

	public enum RollingAverageOf {
		OF_5("0"),
		OF_12("1");

		private final String code;

		RollingAverageOf(String code) {
			this.code = code;
		}

		public static RollingAverageOf byCode(String arg) {
			switch (arg) {
				case "0":
					return OF_5;
				case "1":
					return OF_12;
				default:
					throw new IllegalArgumentException(arg);
			}
		}

		public String getCode() {
			return code;
		}
	}

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

	private Map<RollingAverageOf, List<SolveTime>> averages;
	private Map<RollingAverageOf, List<SolveTime>> sds;

	private List<SolveTime> sessionAvgs;
	private List<SolveTime> sessionSds;

	private Map<RollingAverageOf, Integer> indexOfBestRA;

	private Map<RollingAverageOf, List<Integer>> sortAverages;
	private Map<RollingAverageOf, List<SolveTime>> sortSds;

	private SolveTime runningTotal;
	private SolveTime curSessionAvg;
	private double runningTotalSquareSeconds;
	private	SolveTime curSessionSD;

	private SolveCounter solveCounter;

	private List<StatisticsUpdateListener> statisticsUpdateListeners;
	public DraggableJTableModel tableListener;


	public SessionPuzzleStatistics(Session session, Configuration configuration) {
		this.session = session;
		configuration.addConfigurationChangeListener(this::onConfigurationChange);

		averages = new EnumMap<>(RollingAverageOf.class);
		sds = new EnumMap<>(RollingAverageOf.class);
		sortAverages = new EnumMap<>(RollingAverageOf.class);
		sortSds = new EnumMap<>(RollingAverageOf.class);
		indexOfBestRA = new EnumMap<>(RollingAverageOf.class);

		sessionAvgs = new ArrayList<>();
		sessionSds = new ArrayList<>();

		for (RollingAverageOf i : RollingAverageOf.values()){
			averages.put(i, new ArrayList<>());
			sds.put(i, new ArrayList<>());
			sortAverages.put(i, new ArrayList<>());
			sortSds.put(i, new ArrayList<>());
		}

		solveCounter = new SolveCounter();

		initialize();

		onConfigurationChange(null);
	}

	private void onConfigurationChange(Profile profile) {
		refresh();
	}

	private void initialize() {
		sessionAvgs.clear();
		sessionSds.clear();

		for (RollingAverageOf i : RollingAverageOf.values()){
			averages.get(i).clear();
			sds.get(i).clear();
			sortAverages.get(i).clear();
			sortSds.get(i).clear();
			//indexOfBestRA[i] = -1;
		}

		runningTotal = SolveTime.ZERO_TIME;
		runningTotalSquareSeconds = 0;
		curSessionAvg = SolveTime.ZERO_TIME;
		curSessionSD = SolveTime.NA;
		
		//zero out solvetype counter
		solveCounter.clear();
	}

	public Session getSession() {
		return session;
	}

	public void clear() {
		int[] indices = new int[session.getAttemptsCount()];
		for(int ch = 0; ch < indices.length; ch++) {
			indices[ch] = ch;
		}
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
		if(statisticsUpdateListeners != null) {
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
		SolveTime avg = getSublist(k, session.getAttemptsCount() - getRASize(k), getRASize(k)).getAverage();

		if(!avg.isInfiniteTime()) {

			averages.get(k).add(avg);

			if(avg == SolveTime.NA) {
				SolveTime s = SolveTime.NA;
				sds.get(k).add(s);
				sortSds.get(k).add(s);
			} else {
				SolveTime s = calculateRSD(session.getAttemptsCount() - getRASize(k), k);
				sds.get(k).add(s);

				int i;
				for (i = 0; i < sortSds.get(k).size() && sortSds.get(k).get(i).compareTo(s) <= 0; i++) {
					;
				}
				sortSds.get(k).add(i, s);
			}

			int i;
			for (i = 0; i < sortAverages.get(k).size() && averages.get(k).get(sortAverages.get(k).get(i)).compareTo(avg) < 0; i++) ;
			sortAverages.get(k).add(i, averages.get(k).size() - 1);
			if(i == 0){
				int newbest = averages.get(k).size() - 1;
				if(indexOfBestRA.get(k) < 0 || !Objects.equals(averages.get(k).get(indexOfBestRA.get(k)), averages.get(k).get(newbest))){
					indexOfBestRA.put(k, newbest);
				}
				else{
					//in the event of a tie, we compare the 2 untrimmed averages
					SolveTime newAverage = getSublist(k, session.getAttemptsCount() - getRASize(k), getRASize(k)).getAverage();
					SolveTime oldAverage = getSublist(k, indexOfBestRA.get(k), getRASize(k)).getAverage();
					if(Objects.equals(newAverage, oldAverage)) {
						if (bestTimeOfAverage(indexOfBestRA.get(k), k).compareTo(bestTimeOfAverage(newbest, k)) > 0) {
							indexOfBestRA.put(k, newbest);
						}
						else if (bestTimeOfAverage(indexOfBestRA.get(k), k).equals(bestTimeOfAverage(newbest, k))){
							if (worstTimeOfAverage(indexOfBestRA.get(k), k).compareTo(worstTimeOfAverage(newbest, k)) > 0) {
								indexOfBestRA.put(k, newbest);
							}
						}
					} else if (Utils.lessThan(newAverage, oldAverage)) {
						indexOfBestRA.put(k, newbest);
					}
				}
			}
		}
	}

	private SolveTime calculateRSD(int start, RollingAverageOf num) {
		SolveTime average = averages.get(num).get(start);
		if(average == SolveTime.NA) {
			return SolveTime.NA;
		}
		RollingAverage best_worst = getSublist(num, start, getRASize(num));
		long deviation = 0;
		for(int i = start; i < start + getRASize(num); i++) {
			if(session.getSolution(i).getTime() != best_worst.getBestTime() && session.getSolution(i).getTime() != best_worst.getWorstTime()) {
				SolveTime diff = SolveTime.substruct(session.getSolution(i).getTime(), average);
				deviation += pow2(diff.getTime().toMillis());
			}
		}
		return new SolveTime(Math.sqrt(deviation / (double)getRASize(num)) / 1000.0);
	}

	void refresh() {
		initialize();

		solveCounter = SolveCounter.fromSolutions(session.getSolutionList());
		int numDNFs = solveCounter.getSolveTypeCount(SolveType.DNF);

		for (Solution solution : session.getSolutionList()) {
			for (RollingAverageOf averageOf : RollingAverageOf.values()){
				if (solveCounter.getAttemptCount() >= getRASize(averageOf)) {
                    calculateCurrentAverage(averageOf);
                }
            }

			if (!solution.getTime().isInfiniteTime()) {
                runningTotal = SolveTime.sum(runningTotal, solution.getTime());
                curSessionAvg = SolveTime.divide(runningTotal, solveCounter.getSolveCount());
                sessionAvgs.add(curSessionAvg);
                runningTotalSquareSeconds += pow2(solution.getTime().getTime().toMillis());
                curSessionSD = new SolveTime(Math.sqrt(
                        runningTotalSquareSeconds
								/ (session.getAttemptsCount() - numDNFs) - pow2(curSessionAvg.getTime().toMillis()))
						/ 1000.0);
                sessionSds.add(curSessionSD);
            }
		}
		notifyListeners();
	}

	public int getRASize(RollingAverageOf num) {
		return session.getPuzzleType().getRASize(num);
	}
	
	public RollingAverageTime getRA(int toIndex, RollingAverageOf whichRA) {
		int RAnum = 1 + toIndex - getRASize(whichRA);
		SolveTime seconds = averages.get(whichRA).get(RAnum);
		return new RollingAverageTime(seconds, whichRA);
	}

	@Deprecated
	public SolveTime average(AverageType type, RollingAverageOf num) {
		SolveTime average;
		switch (type) {
            case SESSION_AVERAGE:
                average = curSessionAvg;
                break;
            case BEST_ROLLING_AVERAGE:
                average = averages.get(num).get(indexOfBestRA.get(num));
                break;
            case CURRENT_ROLLING_AVERAGE:
                average = averages.get(num).get(averages.get(num).size() - 1);
                break;
            default:
                return SolveTime.NULL_TIME;
        }

		if(average.isZero() || average == SolveTime.NA) {
			return SolveTime.NULL_TIME;
		}

		return average;
	}

	public boolean isValid(AverageType type, RollingAverageOf num) {
		switch (type) {
            case SESSION_AVERAGE:
                return curSessionAvg.isDefined();

			case BEST_ROLLING_AVERAGE:
                return averages.get(num).get(sortAverages.get(num).get(0)).isDefined();

            case CURRENT_ROLLING_AVERAGE:
                return averages.get(num).get(averages.get(num).size() - 1).isDefined();

            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
	}

	private RollingAverage getSublist(RollingAverageOf ra, int fromIndex, int count) {
		return session.getRollingAverage(ra, fromIndex,  count);
	}

	private RollingAverage getSublist(AverageType type, RollingAverageOf ra) {
		if(type == AverageType.SESSION_AVERAGE) {
			return session.getRollingAverageForWholeSession();
		}

		int lower = type == AverageType.CURRENT_ROLLING_AVERAGE ? averages.get(ra).size() - 1 : indexOfBestRA.get(ra);
		return 	session.getRollingAverage(ra, lower, getRASize(ra));
	}

	public boolean containsTime(int indexOfSolve, AverageType type, RollingAverageOf num) {
		int lower = type == AverageType.CURRENT_ROLLING_AVERAGE ? averages.get(num).size() - 1 : indexOfBestRA.get(num);
		int upper = lower + getRASize(num);

		return indexOfSolve >= lower && indexOfSolve < upper;
	}

	public String toStatsString(AverageType type, boolean showSplits, RollingAverageOf num) {
		RollingAverage times = getSublist(type, num);
		StringBuilder ret = new StringBuilder();
		int i = 0;
		for(Solution next : times.getSolutions()){
			String comment = next.getComment();
			if(!comment.isEmpty())
				comment = "\t" + comment;
			boolean parens = next.getTime() == times.getBestTime() || next.getTime() == times.getWorstTime();

			ret.append(++i).append(".\t");
			if(parens) ret.append("(");
			ret.append(next.getTime().toString());
			if(parens) ret.append(")\t");
			else ret.append("\t");
			ret.append(next.getScrambleString());
			if(showSplits) ret.append(StringAccessor.getString("Statistics.splits")).append(next.toSplitsString());
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

	public String toTerseString(AverageType type, RollingAverageOf num, boolean showincomplete) {
		RollingAverage list = getSublist(type, num);
		if(list.getSolutions().size() != getRASize(num) && !showincomplete) {
			return "N/A";
		}
		return list.toTerseString();
	}

	public SolveTime standardDeviation(AverageType type, RollingAverageOf num) {
		switch (type) {
			case SESSION_AVERAGE:
				return getSessionSD();
			case BEST_ROLLING_AVERAGE:
				return sds.get(num).get(indexOfBestRA.get(num));
			case CURRENT_ROLLING_AVERAGE:
				return sds.get(num).get(sds.get(num).size() - 1);
			default:
				return SolveTime.NA;
		}
	}

	private SolveTime bestTimeOfAverage(int n, RollingAverageOf num) {
		return getSublist(num, n, getRASize(num)).getBestTime();
	}

	private SolveTime worstTimeOfAverage(int n, RollingAverageOf num) {
		return getSublist(num, n, getRASize(num)).getWorstTime();
	}

	public int getIndexOfBestRA(RollingAverageOf num){
		return indexOfBestRA.get(num);
	}

	public SolveCounter getSolveCounter() {
		return solveCounter;
	}

	// access methods
	public SolveTime getSessionAvg() {
		return curSessionAvg;
	}

	public SolveTime getSessionSD() {
		return curSessionSD;
	}

	public SolveTime getTime(int n) {
		if (session.getAttemptsCount() == 0)
			return SolveTime.NA;

		return session.getSolution(n).getTime();
	}

	public SolveTime getAverage(int n, RollingAverageOf num) {
		if(averages.get(num).isEmpty()) {
			return SolveTime.NA;
		}
		return averages.get(num).get(n);
	}

	public SolveTime getSD(int n, RollingAverageOf num) {
		if (sds.get(num).isEmpty()) {
			return SolveTime.NA;
		}
		
		return sds.get(num).get(n);
	}

	public SolveTime getSortAverage(int n, RollingAverageOf num) {
		if(n < 0) {
			n = sortAverages.get(num).size() + n;
		}

		if(sortAverages.get(num).size() == 0 || n < 0 || n >= sortAverages.get(num).size())
			return SolveTime.NA;
		
		return averages.get(num).get(sortAverages.get(num).get(n));
	}

	public SolveTime getSortSD(int n, RollingAverageOf num) {
		if(n < 0) {
			n = sortSds.get(num).size() + n;
		}

		if(sortSds.get(num).size() == 0 || n < 0 || n >= sortSds.get(num).size()) {
			return SolveTime.NA;
		}
		
		return sortSds.get(num).get(n);
	}

	public SolveTime getSortAverageSD(int n, RollingAverageOf num) {
		if(n < 0)
			n = sortAverages.get(num).size() + n;

		if(sortAverages.get(num).size() == 0 || n < 0 || n >= sortAverages.get(num).size()) {
			return SolveTime.NA;
		}
		return sds.get(num).get(sortAverages.get(num).get(n));
	}

	public SolveTime getBestTimeOfAverage(int n, RollingAverageOf num) {
		if(n < 0) {
			n = averages.get(num).size() + n;
		}

		if(averages.get(num).size() == 0 || n < 0 || n >= averages.get(num).size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(n, num);
	}

	public SolveTime getWorstTimeOfAverage(int n, RollingAverageOf num) {
		if(n < 0) {
			n = averages.get(num).size() + n;
		}

		if(averages.get(num).size() == 0 || n < 0 || n >= averages.get(num).size())
			return SolveTime.NULL_TIME;
		return worstTimeOfAverage(n, num);
	}

	public SolveTime getBestTimeOfSortAverage(int n, RollingAverageOf num) {
		if(n < 0) {
			n = sortAverages.get(num).size() + n;
		}

		if(sortAverages.get(num).size() == 0 || n < 0 || n >= sortAverages.get(num).size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(sortAverages.get(num).get(n), num);
	}

	public SolveTime getWorstTimeOfSortAverage(int n, RollingAverageOf num) {
		if(n < 0) {
			n = sortAverages.get(num).size() + n;
		}

		if(sortAverages.get(num).size() == 0 || n < 0 || n >= sortAverages.get(num).size()) {
			return SolveTime.NULL_TIME;
		}
		return worstTimeOfAverage(sortAverages.get(num).get(n), num);
	}

	public SolveTime getSessionAverage(int n) {
		if(n < 0) {
			n = sessionAvgs.size() + n;
		}

		if(sessionAvgs.size() == 0 || n < 0 || n >= sessionAvgs.size()) {
			return SolveTime.NA;
		}
		return sessionAvgs.get(n);
	}

	public SolveTime getSessionSD(int n) {
		if(n < 0) {
			n = sessionSds.size() + n;
		}

		if(sessionSds.size() == 0 || n < 0 || n >= sessionSds.size()) {
			return SolveTime.NA;
		}
		return sessionSds.get(n);
	}

	public SolveTime getProgressTime() {
		if(session.getAttemptsCount() < 2) {
			return SolveTime.NA;
		}
		
		SolveTime t1 = getTime(-1);
		if(t1 == SolveTime.NA) {
			return SolveTime.NA;
		}
		SolveTime t2 = getTime(-2);
		if(t2 == SolveTime.NA) {
			return SolveTime.NA;
		}
		return SolveTime.substruct(t1, t2);
	}

	public SolveTime getProgressAverage(RollingAverageOf num) {
		if(averages.get(num).size() == 0) {
			return SolveTime.NA;
		} else if(averages.get(num).size() == 1) {
			return SolveTime.NA;
		} else {
			SolveTime t1 = getAverage(-1, num);
			if(t1 == SolveTime.NA)
				return SolveTime.NA;
			SolveTime t2 = getAverage(-2, num);
			if(t2 == SolveTime.NA)
				return SolveTime.NA;
			return SolveTime.substruct(t1, t2);
		}
	}

	public SolveTime getProgressSessionAverage() {
		if(sessionAvgs.size() == 0) {
			return SolveTime.NA;
		} else if(sessionAvgs.size() == 1) {
			return SolveTime.NA;
		} else {
			SolveTime t1 = getSessionAverage(-1);
			if(t1 == SolveTime.NA)
				return SolveTime.NA;
			SolveTime t2 = getSessionAverage(-2);
			if(t2 == SolveTime.NA)
				return SolveTime.NA;
			return SolveTime.substruct(t1, t2);
		}
	}

	public SolveTime getProgressSessionSD() {
		if(sessionSds.size() < 2)
			return SolveTime.NA;
		
		SolveTime t1 = getSessionSD(-1);
		if(t1 == SolveTime.NA)
			return SolveTime.NA;
		SolveTime t2 = getSessionSD(-2);
		if(t2 == SolveTime.NA)
			return SolveTime.NA;
		return SolveTime.substruct(t1, t2);
	}

	public SolveTime getBestTime() {
		return session.getRollingAverageForWholeSession().getBestTime();
	}

	public SolveTime getBestAverage(RollingAverageOf num) {
		return getSortAverage(0, num);
	}

	public SolveTime getBestSD(RollingAverageOf num) {
		return getSortSD(0, num);
	}

	public SolveTime getBestAverageSD(RollingAverageOf num) {
		return getSortAverageSD(0, num);
	}

	public SolveTime getWorstTime() {
		return session.getRollingAverageForWholeSession().getWorstTime();
	}

	public SolveTime getWorstAverage(RollingAverageOf num) {
		return getSortAverage(-1, num);
	}

	public SolveTime getWorstSD(RollingAverageOf num) {
		return getSortSD(-1, num);
	}

	public SolveTime getWorstAverageSD(RollingAverageOf num) {
		return getSortAverageSD(-1, num);
	}

	public SolveTime getCurrentTime() {
		return getTime(-1);
	}

	public SolveTime getCurrentAverage(RollingAverageOf num) {
		return getAverage(-1, num);
	}

	public SolveTime getCurrentSD(RollingAverageOf num) {
		return getSD(-1, num);
	}

	public SolveTime getLastTime() {
		return getTime(-2);
	}

	public SolveTime getLastAverage(RollingAverageOf num) {
		return getAverage(-2, num);
	}

	public SolveTime getLastSD(RollingAverageOf num) {
		return getSD(-2, num);
	}

	public SolveTime getBestTimeOfCurrentAverage(RollingAverageOf num) {
		return getBestTimeOfAverage(-1, num);
	}

	public SolveTime getWorstTimeOfCurrentAverage(RollingAverageOf num) {
		return getWorstTimeOfAverage(-1, num);
	}

	public SolveTime getBestTimeOfLastAverage(RollingAverageOf num) {
		return getBestTimeOfAverage(-2, num);
	}

	public SolveTime getWorstTimeOfLastAverage(RollingAverageOf num) {
		return getWorstTimeOfAverage(-2, num);
	}

	public SolveTime getBestTimeOfBestAverage(RollingAverageOf num) {
		return getBestTimeOfSortAverage(0, num);
	}

	public SolveTime getWorstTimeOfBestAverage(RollingAverageOf num) {
		return getWorstTimeOfSortAverage(0, num);
	}

	public SolveTime getBestTimeOfWorstAverage(RollingAverageOf num) {
		return getBestTimeOfSortAverage(-1, num);
	}

	public SolveTime getWorstTimeOfWorstAverage(RollingAverageOf num) {
		return getWorstTimeOfSortAverage(-1, num);
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

	public String getWorstAverageList(RollingAverageOf num) {
		if(sortAverages.get(num).size() >= 1)
			return toTerseString(sortAverages.get(num).get(sortAverages.get(num).size() - 1), num);
		
		return toTerseString(AverageType.BEST_ROLLING_AVERAGE, num, false);
	}

	public String getLastAverageList(RollingAverageOf num) {
		if(sortAverages.get(num).size() > 1)
			return toTerseString(averages.get(num).size() - 2, num);
		
		return "N/A";
	}
}
