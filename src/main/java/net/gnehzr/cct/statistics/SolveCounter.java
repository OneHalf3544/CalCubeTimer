package net.gnehzr.cct.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SolveCounter {

	private final int attemptsCount;
	private final Multiset<SolveType> tagCounter;

	public SolveCounter() {
		this(0, HashMultiset.create());
	}
	public SolveCounter(int attemptsCount, Multiset<SolveType> tagCounter) {
		this.attemptsCount = attemptsCount;
		this.tagCounter = tagCounter;
	}

	public int getSolveTypeCount(@NotNull SolveType solveType) {
		return tagCounter.count(solveType);
	}

	public int getSolveCount() {
		return attemptsCount - tagCounter.count(SolveType.DNF);
	}

	public int getAttemptCount() {
		return attemptsCount;
	}

	public static SolveCounter fromSolutions(List<Solution> solutionList) {
		Multiset<SolveType> counter = HashMultiset.create();
		for (Solution solution : solutionList) {
			counter.addAll(solution.getTime().getTypes());
		}
		return new SolveCounter(solutionList.size(), counter);
	}

	public static SolveCounter sum(SolveCounter solveCounters1, SolveCounter solveCounters2) {
		Multiset<SolveType> solveTypes = HashMultiset.create();
		solveTypes.addAll(solveCounters1.tagCounter);
		solveTypes.addAll(solveCounters2.tagCounter);
		return new SolveCounter(
				solveCounters1.getAttemptCount() + solveCounters2.getAttemptCount(),
				solveTypes);
	}

	public static SolveCounter fromSessions(SessionsList sessions) {
		return sessions.getSessions().stream()
				.map(session -> session.getStatistics().getSolveCounter())
				.reduce(new SolveCounter(), (solveCounter1, solveCounter2) -> SolveCounter.sum(solveCounter1, solveCounter2));
	}
}
