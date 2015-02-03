package net.gnehzr.cct.statistics;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.ScrambleCustomization;

import java.time.LocalDateTime;
import java.util.*;

public class Statistics implements SolveCounter {

	public static enum AverageType {
		CURRENT {
			public String toString() {
				return StringAccessor.getString("Statistics.currentaverage");
			}
		},
		RA {
			public String toString() {
				return StringAccessor.getString("Statistics.bestRA");
			}
		},
		SESSION {
			public String toString() {
				return StringAccessor.getString("Statistics.sessionAverage");
			}
		}
	}

	public void redo() {
		editActions.getNext().doEdit();
	}

	//returns true if the caller should decrement the scramble #
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
	private ArrayList<Double>[] averages;
	private ArrayList<Double>[] sds;
	private ArrayList<Double> sessionavgs;
	private ArrayList<Double> sessionsds;

	private int[] indexOfBestRA;

	private SortedSet<SolveTime> sortedTimes;
	private ArrayList<Integer>[] sortaverages;
	private ArrayList<Double>[] sortsds;

	private double runningTotal;
	private double curSessionAvg;
	private double runningSquareTotal;
	private	double curSessionSD;
	
	private HashMap<SolveType, Integer> solveCounter;

	private int[] curRASize;
	private boolean[] curRATrimmed;

	public static final int RA_SIZES_COUNT = 2;

	private LocalDateTime dateStarted;

	public LocalDateTime getStartDate() {
		return dateStarted;
	}

	public Statistics(Configuration configuration, LocalDateTime d) {
		dateStarted = d;
		configuration.addConfigurationChangeListener(this::onConfigurationChange);

		//we'll initialized these arrays when our scramble customization is set
		curRASize = new int[RA_SIZES_COUNT];
		curRATrimmed = new boolean[RA_SIZES_COUNT];

		averages = new ArrayList[RA_SIZES_COUNT];
		sds = new ArrayList[RA_SIZES_COUNT];
		sortaverages = new ArrayList[RA_SIZES_COUNT];
		sortsds = new ArrayList[RA_SIZES_COUNT];
		indexOfBestRA = new int[RA_SIZES_COUNT];

		sessionavgs = new ArrayList<>();
		sessionsds = new ArrayList<>();
		
		for(int i = 0; i < RA_SIZES_COUNT; i++){
			averages[i] = new ArrayList<>();
			sds[i] = new ArrayList<>();
			sortaverages[i] = new ArrayList<>();
			sortsds[i] = new ArrayList<>();
		}
		
		solveCounter = new HashMap<>();
		
		times = new ArrayList<>();
		sortedTimes = new TreeSet<>();
		initialize();
	}

	private void onConfigurationChange(Profile profile) {
		if(loadRAs()) {
			refresh();
		}
	}

	private ScrambleCustomization customization;
	public void setCustomization(ScrambleCustomization sc) {
		customization = sc;
		onConfigurationChange(null);
	}

	private void initialize() {
		times.clear();
		sortedTimes.clear();
		sessionavgs.clear();
		sessionsds.clear();

		for(int i = 0; i < RA_SIZES_COUNT; i++){
			averages[i].clear();
			sds[i].clear();
			sortaverages[i].clear();
			sortsds[i].clear();
			indexOfBestRA[i] = -1;
		}

		runningTotal = runningSquareTotal = 0;
		curSessionAvg = 0;
		curSessionSD = Double.POSITIVE_INFINITY;
		
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
		if(tableListener != null) {
			if(newTime) {
				int row = times.size() - 1;
				tableListener.fireTableRowsInserted(row, row);
			} else
				tableListener.fireTableDataChanged();
		}
		editActions.notifyListener();
		if(statisticsUpdateListeners != null) {
			statisticsUpdateListeners.forEach(StatisticsUpdateListener::update);
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
			solution.setScramble(times.get(pos).getScramble());
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

	private void addHelper(Solution solveTime) {
		times.add(solveTime);
		sortedTimes.add(solveTime.getTime());

		for(int k = 0; k < RA_SIZES_COUNT; k++)
			if(times.size() >= curRASize[k])
				calculateCurrentAverage(k);

		for(SolveType t : solveTime.getTime().getTypes()) {
			Integer count = solveCounter.get(t);
			if(count == null)
				count = 0;
			count++;
			solveCounter.put(t, count);
		}
		Integer numDNFs = getSolveTypeCount(SolveType.DNF);
		if(!solveTime.getTime().isInfiniteTime()) {
			double t = solveTime.getTime().secondsValue();
			runningTotal += t;
			curSessionAvg = runningTotal / getSolveCount();
			sessionavgs.add(curSessionAvg);
			runningSquareTotal += t * t;
			curSessionSD = Math.sqrt(runningSquareTotal
					/ (times.size() - numDNFs) - curSessionAvg
					* curSessionAvg);
			sessionsds.add(curSessionSD);
		}
	}

	private void calculateCurrentAverage(int k) {
		double avg = calculateRA(times.size() - curRASize[k], times.size(), k, curRATrimmed[k]);
		if(avg > 0) {
			Double s;
			int i;

			Double av = new Double(avg);
			averages[k].add(av);

			if(avg == Double.POSITIVE_INFINITY) {
				s = new Double(Double.POSITIVE_INFINITY);
				sds[k].add(s);
				sortsds[k].add(s);
			} else {
				double sd = calculateRSD(times.size() - curRASize[k], k);
				s = new Double(sd);
				sds[k].add(s);

				for(i = 0; i < sortsds[k].size() && sortsds[k].get(i).compareTo(s) <= 0; i++) ;
				sortsds[k].add(i, s);
			}

			for(i = 0; i < sortaverages[k].size() && averages[k].get(sortaverages[k].get(i)).compareTo(av) < 0; i++) ;
			sortaverages[k].add(i, averages[k].size() - 1);
			if(i == 0){
				int newbest = averages[k].size() - 1;
				if(indexOfBestRA[k] < 0 || !Utils.equalDouble(averages[k].get(indexOfBestRA[k]), averages[k].get(newbest))){
					indexOfBestRA[k] = newbest;
				}
				else{
					//in the event of a tie, we compare the 2 untrimmed averages
					double newave = calculateRA(times.size() - curRASize[k], times.size(), k, false);
					double oldave = calculateRA(indexOfBestRA[k], indexOfBestRA[k] + curRASize[k], k, false);
					if(Utils.equalDouble(newave, oldave)) {
						if(bestTimeOfAverage(indexOfBestRA[k], k).compareTo(bestTimeOfAverage(newbest, k)) > 0)
							indexOfBestRA[k] = newbest;
						else if(bestTimeOfAverage(indexOfBestRA[k], k).equals(bestTimeOfAverage(newbest, k))){
							if(worstTimeOfAverage(indexOfBestRA[k], k).compareTo(worstTimeOfAverage(newbest, k)) > 0)
								indexOfBestRA[k] = newbest;
						}
					} else if(newave < oldave)
						indexOfBestRA[k] = newbest;
				}
			}
		}
	}

	private double calculateRA(int a, int b, int num, boolean trimmed) {
		if(a < 0)
			return -1;
		SolveTime best = null, worst = null;
		int ignoredSolves = 0;
		if(trimmed) {
			SolveTime[] bestWorst = getBestAndWorstTimes(a, b);
			best = bestWorst[0];
			worst = bestWorst[1];
			ignoredSolves = 2;
		}
		double total = 0;
		int multiplier = 1;
		for(int i = a; i < b; i++) {
			SolveTime time = times.get(i).getTime();
			if(time != best && time != worst) {
				if(time.isInfiniteTime()) {
					if(trimmed)
						return Double.POSITIVE_INFINITY;
					
					multiplier = -1;
				} else
					total += time.secondsValue();
			}
		}
		//if we're calling this method with trimmed == false, we know the RA is valid, and we will return the negative of the true average if there was one infinite time
		return multiplier * total / (curRASize[num] - ignoredSolves);
	}

	private double calculateRSD(int start, int num) {
		if(start < 0)
			return -1;
		int end = start + getRASize(num);
		double average = averages[num].get(start);
		if(average == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		SolveTime[] best_worst = getBestAndWorstTimes(start, end);
		double deviation = 0;
		for(int i = start; i < end; i++) {
			if(times.get(i).getTime() != best_worst[0] && times.get(i).getTime() != best_worst[1]) {
				double diff = times.get(i).getTime().secondsValue() - average;
				deviation += diff*diff;
			}
		}
		return Math.sqrt(deviation / getRASize(num));
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
		if(n < 0)
			n = times.size() + n;

		if(times.size() == 0 || n < 0 || n >= times.size())
			return null;
		
		return times.get(n);
	}

	public int getRASize(int num) {
		return curRASize[num];
	}
	
	public Solution getRA(int num, int whichRA) {
		int RAnum = 1 + num - curRASize[whichRA];
		double seconds;
		if(RAnum < 0)
			seconds = -1;
		else
			seconds = averages[whichRA].get(RAnum);
		return new Solution(seconds, whichRA);
	}

	private boolean loadRAs() {
		boolean refresh = false;
		for(int c = 0; c < curRASize.length && customization != null; c++) {
			int raSize = customization.getRASize(c);
			boolean raTrimmed = customization.isTrimmed(c);
			if(raSize != curRASize[c] || raTrimmed != curRATrimmed[c]) {
				curRASize[c] = raSize;
				curRATrimmed[c] = raTrimmed;
				refresh = true;
			}
		}
		return refresh;
	}

	public SolveTime average(AverageType type, int num) {
		double average;
		try {
			if(type == AverageType.SESSION)
				average = curSessionAvg;
			else if(type == AverageType.RA)
				average = averages[num].get(indexOfBestRA[num]);
			else if(type == AverageType.CURRENT)
				average = averages[num].get(averages[num].size() - 1);
			else
				return SolveTime.NULL_TIME;
		} catch (IndexOutOfBoundsException e) {
			return SolveTime.NULL_TIME;
		}

		if(average == 0)
			return SolveTime.NULL_TIME;

		if(average == Double.POSITIVE_INFINITY)
			return SolveTime.NULL_TIME;

		return new SolveTime(average);
	}

	public boolean isValid(AverageType type, int num) {
		double average;
		try {
			if(type == AverageType.SESSION)
				average = curSessionAvg;
			else if(type == AverageType.RA)
				average = averages[num].get(sortaverages[num].get(0));
			else if(type == AverageType.CURRENT)
				average = averages[num].get(averages[num].size() - 1);
			else
				return false;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		if(average == 0 || average == Double.POSITIVE_INFINITY) {
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
		if(type == AverageType.SESSION) {
			lower = 0;
			upper = times.size();
		} else {
			if(type == AverageType.CURRENT)
				lower = averages[num].size() - 1;
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

	public SolveTime[] getBestAndWorstTimes(int a, int b) {
		SolveTime best = SolveTime.WORST;
		SolveTime worst = SolveTime.BEST;
		for(Solution time : getSublist(a, b)){
			if(best.compareTo(time.getTime()) >= 0)
				best = time.getTime();
			// the following should not be an else
			if(worst.compareTo(time.getTime()) < 0)
				worst = time.getTime();
		}
		return new SolveTime[] { best, worst };
	}

	public SolveTime[] getBestAndWorstTimes(AverageType type, int num) {
		SolveTime best = SolveTime.WORST;
		SolveTime worst = SolveTime.BEST;
		boolean ignoreInfinite = type == AverageType.SESSION;
		for(Solution time : getSublist(type, num)) {
			if(best.compareTo(time.getTime()) >= 0)
				best = time.getTime();
			// the following should not be an else
			if(worst.compareTo(time.getTime()) < 0 && !(ignoreInfinite && time.getTime().isInfiniteTime()))
				worst = time.getTime();
		}
		return new SolveTime[] { best, worst };
	}

	public String toStatsString(AverageType type, boolean showSplits, int num) {
		SolveTime[] bestAndWorst = ((type == AverageType.SESSION) ? new SolveTime[] {
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
			String comment = next.getTime().getComment();
			if(!comment.isEmpty())
				comment = "\t" + comment;
			boolean parens = next.getTime() == best || next.getTime() == worst;

			ret.append(++i).append(".\t");
			if(parens) ret.append("(");
			ret.append(next.toString());
			if(parens) ret.append(")\t");
			else ret.append("\t");
			ret.append(next.getScramble());
			if(showSplits) ret.append(StringAccessor.getString("Statistics.splits")).append(next.toSplitsString());
			ret.append(comment);
			ret.append("\n");
		}
		return ret.toString();
	}

	public String toTerseString(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		SolveTime[] bestAndWorst = getBestAndWorstTimes(n, n + curRASize[num]);
		List<Solution> list = getSublist(n, n + curRASize[num]);
		if(list.size() == 0)
			return "N/A";
		
		return toTerseStringHelper(list, bestAndWorst[0], bestAndWorst[1]);
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
			if(parens) ret.append("(");
			ret.append(next.toString());
			if(parens) ret.append(")");
			nextAppend = ", ";
		}
		return ret.toString();
	}

	public SolveTime standardDeviation(AverageType type, int num) {
		double sd = Double.POSITIVE_INFINITY;
		if(type == AverageType.SESSION)
			sd = curSessionSD;
		else if(type == AverageType.RA)
			sd = sds[num].get(indexOfBestRA[num]).doubleValue();
		else if(type == AverageType.CURRENT)
			sd = sds[num].get(sds[num].size() - 1).doubleValue();
		return new SolveTime(sd);
	}

	private SolveTime bestTimeOfAverage(int n, int num) {
		return getBestAndWorstTimes(n, n + curRASize[num])[0];
	}

	private SolveTime worstTimeOfAverage(int n, int num) {
		return getBestAndWorstTimes(n, n + curRASize[num])[1];
	}

	public int getIndexOfBestRA(int num){
		return indexOfBestRA[num];
	}

	// access methods
	public double getSessionAvg() {
		return curSessionAvg;
	}

	public double getSessionSD() {
		return curSessionSD;
	}

	@Override
	public int getSolveCount() {
		int unsolved = 0;
		for(SolveType t : solveCounter.keySet())
			if(!t.isSolved())
				unsolved += solveCounter.get(t);
		return times.size() - unsolved;
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

	public double getTime(int n) {
		if(n < 0)
			n = times.size() + n;

		if(times.size() == 0 || n < 0 || n >= times.size())
			return Double.POSITIVE_INFINITY;
		
		return times.get(n).getTime().secondsValue();
	}

	public double getAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages[num].size() + n;

		if(averages[num].size() == 0 || n < 0 || n >= averages[num].size())
			return Double.POSITIVE_INFINITY;

		return averages[num].get(n).doubleValue();
	}

	public double getSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sds[num].size() + n;

		if(sds[num].size() == 0 || n < 0 || n >= sds[num].size())
			return Double.POSITIVE_INFINITY;
		
		return sds[num].get(n).doubleValue();
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

	public double getSortAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages[num].size() + n;

		if(sortaverages[num].size() == 0 || n < 0 || n >= sortaverages[num].size())
			return Double.POSITIVE_INFINITY;
		
		return averages[num].get(sortaverages[num].get(n));
	}

	public double getSortSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortsds[num].size() + n;

		if(sortsds[num].size() == 0 || n < 0 || n >= sortsds[num].size())
			return Double.POSITIVE_INFINITY;
		
		return sortsds[num].get(n);
	}

	public double getSortAverageSD(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages[num].size() + n;

		if(sortaverages[num].size() == 0 || n < 0 || n >= sortaverages[num].size()) {
			return Double.POSITIVE_INFINITY;
		}
		return sds[num].get(sortaverages[num].get(n));
	}

	public SolveTime getBestTimeOfAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages[num].size() + n;

		if(averages[num].size() == 0 || n < 0 || n >= averages[num].size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(n, num);
	}

	public SolveTime getWorstTimeOfAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = averages[num].size() + n;

		if(averages[num].size() == 0 || n < 0 || n >= averages[num].size())
			return SolveTime.NULL_TIME;
		return worstTimeOfAverage(n, num);
	}

	public SolveTime getBestTimeOfSortAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages[num].size() + n;

		if(sortaverages[num].size() == 0 || n < 0 || n >= sortaverages[num].size())
			return SolveTime.NULL_TIME;
		return bestTimeOfAverage(sortaverages[num].get(n), num);
	}

	public SolveTime getWorstTimeOfSortAverage(int n, int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(n < 0)
			n = sortaverages[num].size() + n;

		if(sortaverages[num].size() == 0 || n < 0 || n >= sortaverages[num].size())
			return SolveTime.NULL_TIME;
		return worstTimeOfAverage(sortaverages[num].get(n), num);
	}

	public double getSessionAverage(int n) {
		if(n < 0)
			n = sessionavgs.size() + n;

		if(sessionavgs.size() == 0 || n < 0 || n >= sessionavgs.size())
			return Double.POSITIVE_INFINITY;
		return sessionavgs.get(n);
	}

	public double getSessionSD(int n) {
		if(n < 0)
			n = sessionsds.size() + n;

		if(sessionsds.size() == 0 || n < 0 || n >= sessionsds.size())
			return Double.POSITIVE_INFINITY;
		return sessionsds.get(n);
	}

	public double getProgressTime() {
		if(times.size() < 2)
			return Double.POSITIVE_INFINITY;
		
		double t1 = getTime(-1);
		if(t1 == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		double t2 = getTime(-2);
		if(t2 == Double.POSITIVE_INFINITY)
			return Double.NEGATIVE_INFINITY;
		return t1 - t2;
	}

	public double getProgressAverage(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(averages[num].size() == 0) {
			return Double.POSITIVE_INFINITY;
		} else if(averages[num].size() == 1) {
			return Double.NEGATIVE_INFINITY;
		} else {
			double t1 = getAverage(-1, num);
			if(t1 == Double.POSITIVE_INFINITY)
				return Double.POSITIVE_INFINITY;
			double t2 = getAverage(-2, num);
			if(t2 == Double.POSITIVE_INFINITY)
				return Double.NEGATIVE_INFINITY;
			return t1 - t2;
		}
	}

	public double getProgressSessionAverage() {
		if(sessionavgs.size() == 0) {
			return Double.POSITIVE_INFINITY;
		} else if(sessionavgs.size() == 1) {
			return Double.NEGATIVE_INFINITY;
		} else {
			double t1 = getSessionAverage(-1);
			if(t1 == Double.POSITIVE_INFINITY)
				return Double.POSITIVE_INFINITY;
			double t2 = getSessionAverage(-2);
			if(t2 == Double.POSITIVE_INFINITY)
				return Double.NEGATIVE_INFINITY;
			return t1 - t2;
		}
	}

	public double getProgressSessionSD() {
		if(sessionsds.size() < 2)
			return Double.POSITIVE_INFINITY;
		
		double t1 = getSessionSD(-1);
		if(t1 == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		double t2 = getSessionSD(-2);
		if(t2 == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		return t1 - t2;
	}

	public SolveTime getBestTime() {
		return sortedTimes.isEmpty() ? SolveTime.NULL_TIME : sortedTimes.first();
	}

	public double getBestAverage(int num) {
		return getSortAverage(0, num);
	}

	public double getBestSD(int num) {
		return getSortSD(0, num);
	}

	public double getBestAverageSD(int num) {
		return getSortAverageSD(0, num);
	}

	public SolveTime getWorstTime() {
		SolveTime t;
		int c = -1;
		//look for the worst, non infinite time
		while((t = getSortTime(c--)) != null && t.isInfiniteTime()) ;
		return t == null ? SolveTime.NULL_TIME : t;
	}

	public double getWorstAverage(int num) {
		return getSortAverage(-1, num);
	}

	public double getWorstSD(int num) {
		return getSortSD(-1, num);
	}

	public double getWorstAverageSD(int num) {
		return getSortAverageSD(-1, num);
	}

	public double getCurrentTime() {
		return getTime(-1);
	}

	public double getCurrentAverage(int num) {
		return getAverage(-1, num);
	}

	public double getCurrentSD(int num) {
		return getSD(-1, num);
	}

	public double getLastTime() {
		return getTime(-2);
	}

	public double getLastAverage(int num) {
		return getAverage(-2, num);
	}

	public double getLastSD(int num) {
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
		return toTerseString(AverageType.RA, num, false);
	}

	public String getCurrentAverageList(int num) {
		return toTerseString(AverageType.CURRENT, num, false);
	}

	public String getSessionAverageList() {
		return toTerseString(AverageType.SESSION, 0, true);
	}

	public String getWorstAverageList(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(sortaverages[num].size() >= 1)
			return toTerseString(sortaverages[num].get(sortaverages[num].size() - 1), num);
		
		return toTerseString(AverageType.RA, num, false);
	}

	public String getLastAverageList(int num) {
		if(num < 0) num = 0;
		else if(num >= RA_SIZES_COUNT) num = RA_SIZES_COUNT - 1;

		if(sortaverages[num].size() > 1)
			return toTerseString(averages[num].size() - 2, num);
		
		return "N/A";
	}
}
