package net.gnehzr.cct.statistics;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.PuzzleType;
import org.jooq.lambda.tuple.Tuple2;

import java.util.*;

public class Statistics implements SolveCounter {

	public enum AverageType {
		CURRENT_AVERAGE {
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

	public void redo() {
		editActions.getNext().doEdit();
	}

	/**
	 * @return  true if the caller should decrement the scramble #
	 */
	public boolean undo() {
		CCTUndoableEdit t = editActions.getPrevious();
		t.undoEdit();
		if(t instanceof StatisticsEdit) {
			StatisticsEdit se = (StatisticsEdit) t;
			return se.row == -1 && se.oldTimes == null;
		}
		return false;
	}

	public void setUndoRedoListener(UndoRedoListener url) {
		editActions.setUndoRedoListener(url);
	}

	public UndoRedoList<CCTUndoableEdit> editActions = new UndoRedoList<>();

	List<Solution> times;
	private List<List<SolveTime>> averages;
	private List<List<SolveTime>> sds;
	private List<SolveTime> sessionavgs;
	private List<SolveTime> sessionsds;

	private int[] indexOfBestRA;

	private SortedSet<SolveTime> sortedTimes;
	private List<List<Integer>> sortaverages;
	private List<List<SolveTime>> sortsds;

	private SolveTime runningTotal;
	private SolveTime curSessionAvg;
	private double runningTotalSquareSeconds;
	private	SolveTime curSessionSD;
	
	private Map<SolveType, Integer> solveCounter;

	private int[] curRASize;
	private boolean[] curRATrimmed;

	public static final int RA_SIZES_COUNT = 2;

	public Statistics(Configuration configuration, PuzzleType puzzleType) {
		configuration.addConfigurationChangeListener(this::onConfigurationChange);

		//we'll initialized these arrays when our scramble customization is set
		curRASize = new int[RA_SIZES_COUNT];
		curRATrimmed = new boolean[RA_SIZES_COUNT];

		averages = new ArrayList<>(RA_SIZES_COUNT);
		sds = new ArrayList<>(RA_SIZES_COUNT);
		sortaverages = new ArrayList<>(RA_SIZES_COUNT);
		sortsds = new ArrayList<>(RA_SIZES_COUNT);
		indexOfBestRA = new int[RA_SIZES_COUNT];

		sessionavgs = new ArrayList<>();
		sessionsds = new ArrayList<>();

		for (int i = 0; i < RA_SIZES_COUNT; i++){
			averages.add(i, new ArrayList<>());
			sds.add(i, new ArrayList<>());
			sortaverages.add(i, new ArrayList<>());
			sortsds.add(i, new ArrayList<>());
		}

		solveCounter = new HashMap<>();

		times = new ArrayList<>();
		sortedTimes = new TreeSet<>();
		customization = puzzleType;

		initialize();

		onConfigurationChange(null);
	}

	private void onConfigurationChange(Profile profile) {
		if (loadRollingAverages()) {
			refresh();
		}
	}

	private PuzzleType customization;

	private void initialize() {
		times.clear();
		sortedTimes.clear();
		sessionavgs.clear();
		sessionsds.clear();

		for(int i = 0; i < RA_SIZES_COUNT; i++){
			averages.get(i).clear();
			sds.get(i).clear();
			sortaverages.get(i).clear();
			sortsds.get(i).clear();
			indexOfBestRA[i] = -1;
		}

		runningTotal = SolveTime.ZERO_TIME;
		runningTotalSquareSeconds = 0;
		curSessionAvg = SolveTime.ZERO_TIME;
		curSessionSD = SolveTime.NA;
		
		//zero out solvetype counter
		solveCounter.clear();
	}

	
	public void clear() {
		int[] indices = new int[times.size()];
		for(int ch = 0; ch < indices.length; ch++) {
			indices[ch] = ch;
		}
		editActions.add(new StatisticsEdit(this, indices, Iterables.toArray(times, Solution.class), null));
		initialize();
		notifyListeners(false);
	}

	private List<StatisticsUpdateListener> statisticsUpdateListeners;

	public void setStatisticsUpdateListeners(List<StatisticsUpdateListener> listener) {
		statisticsUpdateListeners = listener;
	}
	
	public DraggableJTableModel tableListener;

	public void setTableListener(DraggableJTableModel tableListener) {
		this.tableListener = tableListener;
	}
	
	//TODO - this could probably be cleaned up, as it is currently
	//hacked together from the ashes of the old system (see StatisticsTableModel for how it's done)
	public void notifyListeners(boolean newTime) {
		if (tableListener != null) {
			if (newTime) {
				int row = times.size() - 1;
				tableListener.fireTableRowsInserted(row, row);
			} else {
				tableListener.fireTableDataChanged();
			}
		}
		editActions.notifyListener();
		if(statisticsUpdateListeners != null) {
			statisticsUpdateListeners.forEach(e -> e.update(null));
		}
	}
	
	public void add(int pos, Solution solution) {
		if(pos == times.size()) {
			add(solution);
		} else {
			editActions.add(new StatisticsEdit(this, new int[]{pos}, null, solution));
			times.add(pos, solution);
			refresh();
		}
	}
	
	public void set(int pos, Solution solution) {
		if(pos == times.size()) {
			addHelper(solution);
			editActions.add(new StatisticsEdit(this, new int[]{pos}, null, solution));
			notifyListeners(true);
		} else {
			editActions.add(new StatisticsEdit(this, new int[]{pos}, new Solution[]{times.get(pos)}, solution));
			times.set(pos, solution);
			refresh(); //this will fire table data changed
		}
	}
	
	//returns an array of the times removed
	//index array must be sorted!
	public void remove(int[] indices) {
		Solution[] t = new Solution[indices.length];
		for(int ch = indices.length - 1; ch >= 0; ch--) {
			int i = indices[ch];
			if(i >= 0 && i < times.size()) {
				t[ch] = times.get(i);
				times.remove(i);
			} else {
				t[ch] = null;
				indices[ch] = -1;
			}
		}
		editActions.add(new StatisticsEdit(this, indices, t, null));
		refresh();
	}
	
	public void setSolveTypes(int row, List<SolveType> newTypes) {
		SolveTime selectedSolve = times.get(row).getTime();
		List<SolveType> oldTypes = selectedSolve.getTypes();
		selectedSolve.setTypes(newTypes);
		editActions.add(new StatisticsEdit(this, row, oldTypes, newTypes));
		refresh();
	}
	
	//this method will not cause CALCubeTimer to increment the scramble number
	//nasty fix for undo-redo
	void addSilently(int pos, Solution solution) {
		editActions.add(new StatisticsEdit(this, new int[]{pos}, null, solution));
		times.add(pos, solution);
		refresh();
	}

	public void add(Solution s) {
		addHelper(s);
		int newRow = times.size() - 1;
		editActions.add(new StatisticsEdit(this, new int[]{newRow}, null, s));
		notifyListeners(true);
	}

	private void addHelper(Solution solution) {
		times.add(solution);
		sortedTimes.add(solution.getTime());

		for(int k = 0; k < RA_SIZES_COUNT; k++)
			if(times.size() >= curRASize[k])
				calculateCurrentAverage(k);

		for(SolveType t : solution.getTime().getTypes()) {
			Integer count = solveCounter.get(t);
			if(count == null)
				count = 0;
			count++;
			solveCounter.put(t, count);
		}
		Integer numDNFs = getSolveTypeCount(SolveType.DNF);
		if(!solution.getTime().isInfiniteTime()) {
			runningTotal = SolveTime.sum(runningTotal, solution.getTime());
			curSessionAvg = SolveTime.divide(runningTotal, getSolveCount());
			sessionavgs.add(curSessionAvg);
			runningTotalSquareSeconds += pow2(solution.getTime().getTime().toMillis());
			curSessionSD = new SolveTime(Math.sqrt(
					runningTotalSquareSeconds / (times.size() - numDNFs)
					- pow2(curSessionAvg.getTime().toMillis())) / 1000.0);
			sessionsds.add(curSessionSD);
		}
	}

	private long pow2(long millis) {
		return millis * millis;
	}

	private void calculateCurrentAverage(int k) {
		SolveTime avg = calculateRA(times.size() - curRASize[k], times.size(), k, curRATrimmed[k]);
		if(!avg.isInfiniteTime()) {
			int i;

			averages.get(k).add(avg);

			if(avg == SolveTime.NA) {
				SolveTime s = SolveTime.NA;
				sds.get(k).add(s);
				sortsds.get(k).add(s);
			} else {
				SolveTime s = calculateRSD(times.size() - curRASize[k], k);
				sds.get(k).add(s);

				for (i = 0; i < sortsds.get(k).size() && sortsds.get(k).get(i).compareTo(s) <= 0; i++) {
					;
				}
				sortsds.get(k).add(i, s);
			}

			for (i = 0; i < sortaverages.get(k).size() && averages.get(k).get(sortaverages.get(k).get(i)).compareTo(avg) < 0; i++) ;
			sortaverages.get(k).add(i, averages.get(k).size() - 1);
			if(i == 0){
				int newbest = averages.get(k).size() - 1;
				if(indexOfBestRA[k] < 0 || !Objects.equals(averages.get(k).get(indexOfBestRA[k]), averages.get(k).get(newbest))){
					indexOfBestRA[k] = newbest;
				}
				else{
					//in the event of a tie, we compare the 2 untrimmed averages
					SolveTime newAverage = calculateRA(times.size() - curRASize[k], times.size(), k, false);
					SolveTime oldAverage = calculateRA(indexOfBestRA[k], indexOfBestRA[k] + curRASize[k], k, false);
					if(Objects.equals(newAverage, oldAverage)) {
						if(bestTimeOfAverage(indexOfBestRA[k], k).compareTo(bestTimeOfAverage(newbest, k)) > 0)
							indexOfBestRA[k] = newbest;
						else if(bestTimeOfAverage(indexOfBestRA[k], k).equals(bestTimeOfAverage(newbest, k))){
							if(worstTimeOfAverage(indexOfBestRA[k], k).compareTo(worstTimeOfAverage(newbest, k)) > 0)
								indexOfBestRA[k] = newbest;
						}
					} else if(Utils.lessThan(newAverage, oldAverage)) {
						indexOfBestRA[k] = newbest;
					}
				}
			}
		}
	}

	private SolveTime calculateRA(int a, int b, int num, boolean trimmed) {
		if(a < 0) {
			return SolveTime.NA;
		}
		SolveTime best = null, worst = null;
		int ignoredSolves = 0;
		if(trimmed) {
			Tuple2<SolveTime, SolveTime> bestWorst = getBestAndWorstTimes(a, b);
			best = bestWorst.v1;
			worst = bestWorst.v2;
			ignoredSolves = 2;
		}
		SolveTime total = SolveTime.ZERO_TIME;
		int multiplier = 1;
		for(int i = a; i < b; i++) {
			SolveTime time = times.get(i).getTime();
			if(time != best && time != worst) {
				if(time.isInfiniteTime()) {
					if(trimmed)
						return SolveTime.NA;
					
					multiplier = -1;
				} else
					total = SolveTime.sum(total, time);
			}
		}
		//if we're calling this method with trimmed == false, we know the RA is valid, and we will return the negative of the true average if there was one infinite time
		return SolveTime.divide(SolveTime.multiply(total, multiplier),  curRASize[num] - ignoredSolves);
	}

	private SolveTime calculateRSD(int start, int num) {
		if(start < 0) {
			return SolveTime.NA;
		}
		int end = start + getRASize(num);
		SolveTime average = averages.get(num).get(start);
		if(average == SolveTime.NA) {
			return SolveTime.NA;
		}
		Tuple2<SolveTime, SolveTime> best_worst = getBestAndWorstTimes(start, end);
		long deviation = 0;
		for(int i = start; i < end; i++) {
			if(times.get(i).getTime() != best_worst.v1 && times.get(i).getTime() != best_worst.v2) {
				SolveTime diff = SolveTime.substruct(times.get(i).getTime(), average);
				deviation += pow2(diff.getTime().toMillis());
			}
		}
		return new SolveTime(Math.sqrt(deviation / (double)getRASize(num)) / 1000.0);
	}

	void refresh() {
		List<Solution> temp = new ArrayList<>(times);
		initialize();
		for(Solution solution : temp) {
			addHelper(solution);
		}
		notifyListeners(false);
	}

	public Solution get(int n) {
		if(n < 0) {
			n = times.size() + n;
		}

		if(times.size() == 0 || n < 0 || n >= times.size()) {
			return null;
		}
		
		return times.get(n);
	}

	public int getRASize(int num) {
		return curRASize[num];
	}
	
	public RollingAverageTime getRA(int num, int whichRA) {
		int RAnum = 1 + num - curRASize[whichRA];
		SolveTime seconds;
		if(RAnum < 0) {
			seconds = SolveTime.NA;
		}
		else {
			seconds = averages.get(whichRA).get(RAnum);
		}
		return new RollingAverageTime(seconds, whichRA);
	}

	private boolean loadRollingAverages() {
		boolean refresh = false;
		for (int c = 0; c < curRASize.length && !customization.isNullType(); c++) {
			int raSize = customization.getRASize(c);
			boolean rollingAverageTrimmed = customization.isTrimmed(c);
			if (raSize != curRASize[c] || rollingAverageTrimmed != curRATrimmed[c]) {
				curRASize[c] = raSize;
				curRATrimmed[c] = rollingAverageTrimmed;
				refresh = true;
			}
		}
		return refresh;
	}

	public SolveTime average(AverageType type, int num) {
		SolveTime average;
		try {
			if(type == AverageType.SESSION_AVERAGE)
				average = curSessionAvg;
			else if(type == AverageType.BEST_ROLLING_AVERAGE)
				average = averages.get(num).get(indexOfBestRA[num]);
			else if(type == AverageType.CURRENT_AVERAGE)
				average = averages.get(num).get(averages.get(num).size() - 1);
			else
				return SolveTime.NULL_TIME;
		} catch (IndexOutOfBoundsException e) {
			return SolveTime.NULL_TIME;
		}

		if(average.isZero())
			return SolveTime.NULL_TIME;

		if(average == SolveTime.NA)
			return SolveTime.NULL_TIME;

		return average;
	}

	public boolean isValid(AverageType type, int num) {
		SolveTime average;
		try {
			if(type == AverageType.SESSION_AVERAGE)
				average = curSessionAvg;
			else if(type == AverageType.BEST_ROLLING_AVERAGE)
				average = averages.get(num).get(sortaverages.get(num).get(0));
			else if(type == AverageType.CURRENT_AVERAGE)
				average = averages.get(num).get(averages.get(num).size() - 1);
			else
				return false;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		if(average.isZero() || average == SolveTime.NA) {
			return false;
		}
		return true;
	}

	private List<Solution> getSublist(int a, int b) {
		if(b > times.size())
			b = times.size();
		else if(b < 0)
			b = 0;
		return times.subList(a, b);
	}

	private List<Solution> getSublist(AverageType type, int num) {
		int[] bounds = getBounds(type, num);
		return times.subList(bounds[0], bounds[1]);
	}

	private int[] getBounds(AverageType type, int num) {
		int lower, upper;
		if(type == AverageType.SESSION_AVERAGE) {
			lower = 0;
			upper = times.size();
		} else {
			if(type == AverageType.CURRENT_AVERAGE)
				lower = averages.get(num).size() - 1;
			else
				lower = indexOfBestRA[num];

			if(lower < 0)
				lower = 0;
			upper = lower + curRASize[num];
			if(upper > times.size())
				upper = 0; //we don't want to consider *any* solves a member of this average
		}
		return new int[] { lower, upper };
	}

	public boolean containsTime(int indexOfSolve, AverageType type, int num) {
		int bounds[] = getBounds(type, num);
		return indexOfSolve >= bounds[0] && indexOfSolve < bounds[1];
	}

	public Tuple2<SolveTime, SolveTime> getBestAndWorstTimes(int a, int b) {
		SolveTime best = SolveTime.WORST;
		SolveTime worst = SolveTime.BEST;
		for(Solution time : getSublist(a, b)){
			if(best.compareTo(time.getTime()) >= 0)
				best = time.getTime();
			// the following should not be an else
			if(worst.compareTo(time.getTime()) < 0)
				worst = time.getTime();
		}
		return new Tuple2<>(best, worst);
	}

	public SolveTime[] getBestAndWorstTimes(AverageType type, int num) {
		SolveTime best = SolveTime.WORST;
		SolveTime worst = SolveTime.BEST;
		boolean ignoreInfinite = type == AverageType.SESSION_AVERAGE;
		for(Solution time : getSublist(type, num)) {
			if(best.compareTo(time.getTime()) >= 0) {
				best = time.getTime();
			}
			// the following should not be an else
			if(worst.compareTo(time.getTime()) < 0 && !(ignoreInfinite && time.getTime().isInfiniteTime())) {
				worst = time.getTime();
			}
		}
		return new SolveTime[] { best, worst };
	}

	public String toStatsString(AverageType type, boolean showSplits, int num) {
		SolveTime[] bestAndWorst = ((type == AverageType.SESSION_AVERAGE) ? new SolveTime[] {
				null, null }
				: getBestAndWorstTimes(type, num));
		return toStatsStringHelper(getSublist(type, num), bestAndWorst[0],
				bestAndWorst[1], showSplits);
	}

	private String toStatsStringHelper(List<Solution> times,
			SolveTime best, SolveTime worst, boolean showSplits) {
		StringBuilder ret = new StringBuilder();
		int i = 0;
		for(Solution next : times){
			String comment = next.getComment();
			if(!comment.isEmpty())
				comment = "\t" + comment;
			boolean parens = next.getTime() == best || next.getTime() == worst;

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

	public String toTerseString(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		Tuple2<SolveTime, SolveTime> bestAndWorst = getBestAndWorstTimes(n, n + curRASize[num]);
		List<Solution> list = getSublist(n, n + curRASize[num]);
		if(list.size() == 0)
			return "N/A";
		
		return toTerseStringHelper(list, bestAndWorst.v1, bestAndWorst.v2);
	}

	public String toTerseString(AverageType type, int num, boolean showincomplete) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		SolveTime[] bestAndWorst = getBestAndWorstTimes(type, num);
		List<Solution> list = getSublist(type, num);
		if(list.size() != curRASize[num] && !showincomplete)
			return "N/A";
		return toTerseStringHelper(list, bestAndWorst[0], bestAndWorst[1]);
	}

	private static String toTerseStringHelper(List<Solution> printMe,
			SolveTime best, SolveTime worst) {
		StringBuilder ret = new StringBuilder();
		String nextAppend = "";
		for(Solution next : printMe){
			ret.append(nextAppend);
			boolean parens = next.getTime() == best || next.getTime() == worst;
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

	public SolveTime standardDeviation(AverageType type, int num) {
		switch (type) {
			case SESSION_AVERAGE:
				return getSessionSD();
			case BEST_ROLLING_AVERAGE:
				return sds.get(num).get(indexOfBestRA[num]);
			case CURRENT_AVERAGE:
				return sds.get(num).get(sds.get(num).size() - 1);
			default:
				return SolveTime.NA;
		}
	}

	private SolveTime bestTimeOfAverage(int n, int num) {
		return getBestAndWorstTimes(n, n + curRASize[num]).v1;
	}

	private SolveTime worstTimeOfAverage(int n, int num) {
		return getBestAndWorstTimes(n, n + curRASize[num]).v2;
	}

	public int getIndexOfBestRA(int num){
		return indexOfBestRA[num];
	}

	// access methods
	public SolveTime getSessionAvg() {
		return curSessionAvg;
	}

	public SolveTime getSessionSD() {
		return curSessionSD;
	}

	@Override
	public int getSolveCount() {
		return times.size() - solveCounter.keySet().stream()
				.filter(SolveType::isSolved)
				.map(solveCounter::get)
				.reduce((a, b) -> a + b)
				.orElse(0);
	}

	@Override
	public int getAttemptCount() {
		return times.size();
	}

	@Override
	public int getSolveTypeCount(SolveType t) {
		Integer c = solveCounter.get(t);
		if(c == null) c = 0;
		return c;
	}

	public SolveTime getTime(int n) {
		if(n < 0)
			n = times.size() + n;

		if(times.size() == 0 || n < 0 || n >= times.size())
			return SolveTime.NA;

		return times.get(n).getTime();
	}

	public SolveTime getAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages.get(num).size() + n;

		if(averages.get(num).size() == 0 || n < 0 || n >= averages.get(num).size())
			return SolveTime.NA;

		return averages.get(num).get(n);
	}

	public SolveTime getSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sds.get(num).size() + n;

		if(sds.get(num).size() == 0 || n < 0 || n >= sds.get(num).size())
			return SolveTime.NA;
		
		return sds.get(num).get(n);
	}

	/**
	 * @return time, or null if the index is out of bounds
	 */
	private SolveTime getSortTime(int n) {
		if(n < 0) {
			n = sortedTimes.size() + n;
		}

		if(sortedTimes.isEmpty() || n < 0 || n >= sortedTimes.size()) {
			return null;
		}
		
		return Iterables.get(sortedTimes, n);
	}

	public SolveTime getSortAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages.get(num).size() + n;

		if(sortaverages.get(num).size() == 0 || n < 0 || n >= sortaverages.get(num).size())
			return SolveTime.NA;
		
		return averages.get(num).get(sortaverages.get(num).get(n));
	}

	public SolveTime getSortSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortsds.get(num).size() + n;

		if(sortsds.get(num).size() == 0 || n < 0 || n >= sortsds.get(num).size())
			return SolveTime.NA;
		
		return sortsds.get(num).get(n);
	}

	public SolveTime getSortAverageSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages.get(num).size() + n;

		if(sortaverages.get(num).size() == 0 || n < 0 || n >= sortaverages.get(num).size()) {
			return SolveTime.NA;
		}
		return sds.get(num).get(sortaverages.get(num).get(n));
	}

	public SolveTime getBestTimeOfAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages.get(num).size() + n;

		if(averages.get(num).size() == 0 || n < 0 || n >= averages.get(num).size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(n, num);
	}

	public SolveTime getWorstTimeOfAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages.get(num).size() + n;

		if(averages.get(num).size() == 0 || n < 0 || n >= averages.get(num).size())
			return SolveTime.NULL_TIME;
		return worstTimeOfAverage(n, num);
	}

	public SolveTime getBestTimeOfSortAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages.get(num).size() + n;

		if(sortaverages.get(num).size() == 0 || n < 0 || n >= sortaverages.get(num).size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(sortaverages.get(num).get(n), num);
	}

	public SolveTime getWorstTimeOfSortAverage(int n, int num) {
		if(num < 0) {
			num = 0;
		}
		else if(num >= RA_SIZES_COUNT) {
			num = RA_SIZES_COUNT - 1;
		}

		if(n < 0)
			n = sortaverages.get(num).size() + n;

		if(sortaverages.get(num).size() == 0 || n < 0 || n >= sortaverages.get(num).size()) {
			return SolveTime.NULL_TIME;
		}
		return worstTimeOfAverage(sortaverages.get(num).get(n), num);
	}

	public SolveTime getSessionAverage(int n) {
		if(n < 0)
			n = sessionavgs.size() + n;

		if(sessionavgs.size() == 0 || n < 0 || n >= sessionavgs.size())
			return SolveTime.NA;
		return sessionavgs.get(n);
	}

	public SolveTime getSessionSD(int n) {
		if(n < 0) {
			n = sessionsds.size() + n;
		}

		if(sessionsds.size() == 0 || n < 0 || n >= sessionsds.size()) {
			return SolveTime.NA;
		}
		return sessionsds.get(n);
	}

	public SolveTime getProgressTime() {
		if(times.size() < 2)
			return SolveTime.NA;
		
		SolveTime t1 = getTime(-1);
		if(t1 == SolveTime.NA)
			return SolveTime.NA;
		SolveTime t2 = getTime(-2);
		if(t2 == SolveTime.NA)
			return SolveTime.NA;
		return SolveTime.substruct(t1, t2);
	}

	public SolveTime getProgressAverage(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

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
		if(sessionavgs.size() == 0) {
			return SolveTime.NA;
		} else if(sessionavgs.size() == 1) {
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
		if(sessionsds.size() < 2)
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
		return sortedTimes.isEmpty() ? SolveTime.NULL_TIME : sortedTimes.first();
	}

	public SolveTime getBestAverage(int num) {
		return getSortAverage(0, num);
	}

	public SolveTime getBestSD(int num) {
		return getSortSD(0, num);
	}

	public SolveTime getBestAverageSD(int num) {
		return getSortAverageSD(0, num);
	}

	public SolveTime getWorstTime() {
		SolveTime t;
		int c = -1;
		//look for the worst, non infinite time
		while((t = getSortTime(c--)) != null && t.isInfiniteTime()) ;
		return t == null ? SolveTime.NULL_TIME : t;
	}

	public SolveTime getWorstAverage(int num) {
		return getSortAverage(-1, num);
	}

	public SolveTime getWorstSD(int num) {
		return getSortSD(-1, num);
	}

	public SolveTime getWorstAverageSD(int num) {
		return getSortAverageSD(-1, num);
	}

	public SolveTime getCurrentTime() {
		return getTime(-1);
	}

	public SolveTime getCurrentAverage(int num) {
		return getAverage(-1, num);
	}

	public SolveTime getCurrentSD(int num) {
		return getSD(-1, num);
	}

	public SolveTime getLastTime() {
		return getTime(-2);
	}

	public SolveTime getLastAverage(int num) {
		return getAverage(-2, num);
	}

	public SolveTime getLastSD(int num) {
		return getSD(-2, num);
	}

	public SolveTime getBestTimeOfCurrentAverage(int num) {
		return getBestTimeOfAverage(-1, num);
	}

	public SolveTime getWorstTimeOfCurrentAverage(int num) {
		return getWorstTimeOfAverage(-1, num);
	}

	public SolveTime getBestTimeOfLastAverage(int num) {
		return getBestTimeOfAverage(-2, num);
	}

	public SolveTime getWorstTimeOfLastAverage(int num) {
		return getWorstTimeOfAverage(-2, num);
	}

	public SolveTime getBestTimeOfBestAverage(int num) {
		return getBestTimeOfSortAverage(0, num);
	}

	public SolveTime getWorstTimeOfBestAverage(int num) {
		return getWorstTimeOfSortAverage(0, num);
	}

	public SolveTime getBestTimeOfWorstAverage(int num) {
		return getBestTimeOfSortAverage(-1, num);
	}

	public SolveTime getWorstTimeOfWorstAverage(int num) {
		return getWorstTimeOfSortAverage(-1, num);
	}

	public String getBestAverageList(int num) {
		return toTerseString(AverageType.BEST_ROLLING_AVERAGE, num, false);
	}

	public String getCurrentAverageList(int num) {
		return toTerseString(AverageType.CURRENT_AVERAGE, num, false);
	}

	public String getSessionAverageList() {
		return toTerseString(AverageType.SESSION_AVERAGE, 0, true);
	}

	public String getWorstAverageList(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(sortaverages.get(num).size() >= 1)
			return toTerseString(sortaverages.get(num).get(sortaverages.get(num).size() - 1), num);
		
		return toTerseString(AverageType.BEST_ROLLING_AVERAGE, num, false);
	}

	public String getLastAverageList(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(sortaverages.get(num).size() > 1)
			return toTerseString(averages.get(num).size() - 2, num);
		
		return "N/A";
	}
}
