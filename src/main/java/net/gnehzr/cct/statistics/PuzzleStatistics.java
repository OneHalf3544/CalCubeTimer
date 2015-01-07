package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PuzzleStatistics implements StatisticsUpdateListener, SolveCounter {

	private String customization;
	private ProfileDatabase pd;

	private final Configuration configuration;

	public PuzzleStatistics(String customization, ProfileDatabase pd, Configuration configuration,
							StatisticsTableModel statsModel) {
		this.customization = customization;
		this.pd = pd;
		this.configuration = configuration;
		//We need some way for each profile database to listen for updates,
		//this seems fine to me, although nasty
		statsModel.addStatisticsUpdateListener(this);
	}
	public String getCustomization() {
		return customization;
	}
	private CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();
	public Iterable<Session> toSessionIterable() {
		return sessions;
	}
	public int getSessionsCount() {
		return sessions.size();
	}

	public void addSession(Session s, Profile profile) {
		sessions.add(s);
		s.setPuzzleStatistics(this, profile);
		refreshStats();
		pd.fireTableDataChanged();
	}
	public void removeSession(Session s) {
		sessions.remove(s);
		refreshStats();
		pd.fireTableDataChanged();
	}
	public boolean containsSession(Session s) {
		return sessions.contains(s);
	}
	public ProfileDatabase getPuzzleDatabase() {
		return pd;
	}
	public String toString() {
		return customization;
	}
	@Override
	public void update() {
		refreshStats();
		pd.fireTableDataChanged();
	}
	
	private SolveTime bestTime;
	private double globalAverage;
	private double[] bestRAs;
	private int solvedCount;
	private int attemptCount;
	private HashMap<SolveType, Integer> typeCounter;
	private void refreshStats() {
		bestTime = SolveTime.WORST;
		solvedCount = 0;
		attemptCount = 0;
		typeCounter = new HashMap<>();
		globalAverage = 0;
		bestRAs = new double[Statistics.RA_SIZES_COUNT];
		Arrays.fill(bestRAs, Double.POSITIVE_INFINITY);
		for(Session s : sessions) {
			Statistics stats = s.getStatistics();
			SolveTime t = stats.getBestTime();
			if(t.compareTo(bestTime) < 0)
				bestTime = t;
			for(int ra = 0; ra < bestRAs.length; ra++) {
				double ave = stats.getBestAverage(ra);
				if(ave < bestRAs[ra])
					bestRAs[ra] = ave;
			}
			int solves = stats.getSolveCount();
			globalAverage += stats.getSessionAvg() * solves;
			
			solvedCount += solves;
			attemptCount += stats.getAttemptCount();
			for(SolveType type : SolveType.getSolveTypes(configuration.getStringArray(VariableKey.SOLVE_TAGS, false)))
				typeCounter.put(type, getSolveTypeCount(type) + stats.getSolveTypeCount(type));
		}
		if(solvedCount != 0)
			globalAverage /= solvedCount;
		else
			globalAverage = Double.POSITIVE_INFINITY;
	}
	
	//Getters for DynamicString
	public SolveTime getBestTime() {
		return bestTime;
	}
	public double getBestRA(int num) {
		return bestRAs[num];
	}
	public double getGlobalAverage() {
		return globalAverage;
	}
	@Override
	public int getSolveTypeCount(SolveType t) {
		Integer c = typeCounter.get(t);
		if(c == null) c = 0;
		return c;
	}
	@Override
	public int getSolveCount() {
		return solvedCount;
	}
	@Override
	public int getAttemptCount() {
		return attemptCount;
	}
}
