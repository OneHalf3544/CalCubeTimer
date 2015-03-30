package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleCustomization;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class PuzzleStatistics implements StatisticsUpdateListener, SolveCounter {

	private ScrambleCustomization customization;

	private SessionsTableModel sessionsTableModel;

	private final Configuration configuration;

	private SolveTime bestTime;
	private SolveTime globalAverage;
	private SolveTime[] bestRAs;
	private int solvedCount;
	private int attemptCount;
	private Map<SolveType, Integer> typeCounter;

	private CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();

	public PuzzleStatistics(ScrambleCustomization customization, SessionsTableModel sessionsTableModel,
							Configuration configuration, StatisticsTableModel statsModel) {
		this.customization = customization;
		this.sessionsTableModel = sessionsTableModel;
		this.configuration = configuration;
		//We need some way for each profile database to listen for updates,
		//this seems fine to me, although nasty
		statsModel.addStatisticsUpdateListener(this);
	}

	public ScrambleCustomization getCustomization() {
		return customization;
	}

	public Iterable<Session> toSessionIterable() {
		return sessions;
	}

	public int getSessionsCount() {
		return sessions.size();
	}

	public void addSession(Session session) {
		sessions.add(session);
		session.setPuzzleStatistics(this);
		refreshStats();
		sessionsTableModel.fireTableDataChanged();
	}

	public void removeSession(Session s) {
		sessions.remove(s);
		refreshStats();
		sessionsTableModel.fireTableDataChanged();
	}

	public boolean containsSession(Session s) {
		return sessions.contains(s);
	}

	public SessionsTableModel getPuzzleDatabase() {
		return sessionsTableModel;
	}

	public String toString() {
		return customization.toString();
	}

	@Override
	public void update() {
		refreshStats();
		sessionsTableModel.fireTableDataChanged();
	}

	private void refreshStats() {
		solvedCount = 0;
		attemptCount = 0;
		typeCounter = new HashMap<>();

		bestTime = sessions.stream()
				.map(s -> s.getStatistics().getBestTime())
				.min(Comparator.comparing(Function.<SolveTime>identity()))
				.orElse(SolveTime.WORST);

		bestRAs = new SolveTime[Statistics.RA_SIZES_COUNT];
		Arrays.fill(bestRAs, SolveTime.WORST);
		globalAverage = new SolveTime(Duration.ZERO);
		for(Session s : sessions) {
			Statistics stats = s.getStatistics();
			for(int ra = 0; ra < bestRAs.length; ra++) {
				SolveTime ave = stats.getBestAverage(ra);
				if (Utils.lessThan(ave, bestRAs[ra])) {
					bestRAs[ra] = ave;
				}
			}
			int solves = stats.getSolveCount();
			globalAverage = SolveTime.sum(globalAverage, SolveTime.multiply(stats.getSessionAvg(), solves));
			
			solvedCount += solves;
			attemptCount += stats.getAttemptCount();
			for(SolveType type : SolveType.getSolveTypes(configuration.getStringArray(VariableKey.SOLVE_TAGS, false)))
				typeCounter.put(type, getSolveTypeCount(type) + stats.getSolveTypeCount(type));
		}
		if(solvedCount != 0)
			globalAverage = SolveTime.divide(globalAverage, solvedCount);
		else
			globalAverage = SolveTime.WORST;
	}
	
	//Getters for DynamicString
	public SolveTime getBestTime() {
		return bestTime;
	}

	public SolveTime getBestRA(int num) {
		return bestRAs[num];
	}

	public SolveTime getGlobalAverage() {
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
