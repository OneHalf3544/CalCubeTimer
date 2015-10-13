package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.scrambles.PuzzleType;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Session extends Commentable implements Comparable<Session> {

	private Long lastSessionId;
	private SessionPuzzleStatistics sessionPuzzleStatistics;
	private SessionsList sessionsList;
	private final Configuration configuration;
	private final LocalDateTime dateStarted;
	private final PuzzleType puzzleType;
	private final List<Solution> solutions = new ArrayList<>();

	//adds itself to the puzzlestatistics to which it belongs
	public Session(LocalDateTime startSessionTime, Configuration configuration, PuzzleType puzzleType) {
		this.dateStarted = startSessionTime;
		this.configuration = configuration;
		this.puzzleType = puzzleType;
		sessionPuzzleStatistics = new SessionPuzzleStatistics(this, configuration);
	}

	public LocalDateTime getStartTime() {
		return dateStarted;
	}

	public SessionPuzzleStatistics getSessionPuzzleStatistics() {
		return sessionPuzzleStatistics;
	}

	//this should only be called by PuzzleStatistics
	public void setSessionsList(SessionsList sessionsList) {
		this.sessionsList = sessionsList;
	}

	public Long getSessionId() {
		return lastSessionId;
	}

	public void setSessionId(Long lastSessionId) {
		this.lastSessionId = lastSessionId;
	}

	public PuzzleType getPuzzleType() {
		return puzzleType;
	}

	public SessionsList getSessionsList() {
		return sessionsList;
	}

	@Override
	public int hashCode() {
		return getStartTime().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session o = (Session) obj;
			return o.getStartTime().equals(this.getStartTime());
		}
		return false;
	}

	@Override
	public int compareTo(@NotNull Session o) {
		return Comparator.comparing(Session::getStartTime).compare(this, o);
	}

	public String toDateString() {
		return configuration.getDateFormat().format(getStartTime());
	}

	public void delete() {
		sessionsList.removeSession(this);
	}

	public SessionEntity toSessionEntity(Long profileId) {
		SessionEntity sessionEntity = new SessionEntity()
				.withPluginName(getPuzzleType().getScramblePlugin().getPuzzleName())
				.withVariationName(getPuzzleType().getVariationName());
		sessionEntity.setSessionId(lastSessionId);
		sessionEntity.setScrambleCustomization(puzzleType.getCustomization());
		sessionEntity.setSessionStart(getStartTime());

		ProfileEntity profile = new ProfileEntity();
		profile.setProfileId(profileId);
		sessionEntity.setProfile(profile);
		sessionEntity.setSolutions(Seq
				.iterate(0, i -> i++)
				.limit(sessionPuzzleStatistics.getSolveCounter().getSolveCount())
				.map(this::getSolution)
				.map(Solution::toEntity)
				.toList());
		return sessionEntity;
	}

	public Session cloneEmpty() {
		return new Session(LocalDateTime.now(), configuration, getPuzzleType());
	}

	public int getAttemptsCount() {
		return solutions.size();
	}

	public Solution getSolution(int solutionIndex) {
		return solutions.get(solutionIndex);
	}

	public void addSolution(Solution solution) {
		this.solutions.add(solution);
	}

	@Override
	public String toString() {
		return "Session{" +
				"lastSessionId=" + lastSessionId +
				", solutions count=" + getAttemptsCount() +
				", dateStarted=" + dateStarted +
				", puzzleType=" + puzzleType +
				'}';
	}

	public RollingAverage getRollingAverage(RollingAverageOf ra, int fromIndex, int count) {
		return RollingAverage.create(ra, this, fromIndex, count);
	}

	public RollingAverage getRollingAverageForWholeSession() {
		return new RollingAverage(Seq.seq(solutions)
				.filter(solution -> !solution.getTime().isInfiniteTime())
				.toList(),
				0,
				solutions.size(), false);
	}

	public List<Solution> getSolutionList() {
		return Collections.unmodifiableList(solutions);
	}
}
