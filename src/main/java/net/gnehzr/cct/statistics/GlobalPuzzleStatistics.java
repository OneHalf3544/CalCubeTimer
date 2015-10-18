package net.gnehzr.cct.statistics;

import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.PuzzleType;

import java.time.Duration;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class GlobalPuzzleStatistics {

	private PuzzleType puzzleType;
	private final SessionsList sessionsList;

	private SolveTime bestTime;
	private SolveTime globalAverage;
	private Map<RollingAverageOf, SolveTime> bestRAs;
	private SolveCounter solvesCounter;

	public GlobalPuzzleStatistics(PuzzleType puzzleType, SessionsList sessions) {
		this.puzzleType = puzzleType;
		this.sessionsList = sessions;

	}

	public String toString() {
		return puzzleType.toString();
	}

	void refreshStats() {
		solvesCounter = new SolveCounter();

		bestTime = sessionsList.getSessions().stream()
				.map(s -> s.getStatistics().getSession().getRollingAverageForWholeSession().getBestTime())
				.min(Comparator.comparing(Function.<SolveTime>identity()))
				.orElse(SolveTime.WORST);

		bestRAs = new EnumMap<>(RollingAverageOf.class);
		bestRAs.put(RollingAverageOf.OF_5, SolveTime.WORST);
		bestRAs.put(RollingAverageOf.OF_12, SolveTime.WORST);

		globalAverage = new SolveTime(Duration.ZERO);

		for(Session s : sessionsList) {
			SessionPuzzleStatistics sessionStatistics = s.getStatistics();
			for(RollingAverageOf ra : RollingAverageOf.values()) {
				SolveTime ave = sessionStatistics.getBestAverage(ra).getAverage();
				if (Utils.lessThan(ave, bestRAs.get(ra))) {
					bestRAs.put(ra, ave);
				}
			}
			int solves = sessionStatistics.getSolveCounter().getSolveCount();
			globalAverage = SolveTime.sum(globalAverage, SolveTime.multiply(sessionStatistics.getSessionAvg(), solves));
			
		}

		solvesCounter = SolveCounter.fromSessions(sessionsList);

		if(getSolveCounter().getSolveCount() != 0) {
			globalAverage = SolveTime.divide(globalAverage, getSolveCounter().getSolveCount());
		}
		else {
			globalAverage = SolveTime.WORST;
		}
	}
	
	//Getters for DynamicString
	public SolveTime getBestTime() {
		return bestTime;
	}

	public SolveTime getBestRA(RollingAverageOf num) {
		return bestRAs.get(num);
	}

	public SolveTime getGlobalAverage() {
		return globalAverage;
	}

	public SolveCounter getSolveCounter() {
		return solvesCounter;
	}
}
