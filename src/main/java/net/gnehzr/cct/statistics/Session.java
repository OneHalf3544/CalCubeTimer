package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.scrambles.PuzzleType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

import java.time.LocalDateTime;
import java.util.*;

public class Session extends Commentable implements Comparable<Session> {

	private static final Logger LOG = LogManager.getLogger(Session.class);

	private Long lastSessionId;
	private SessionPuzzleStatistics sessionPuzzleStatistics;
	private final Configuration configuration;
	private final LocalDateTime dateStarted;
	private final PuzzleType puzzleType;
	private final List<Solution> solutions = new ArrayList<>();
	private final SolutionDao solutionDao;

	//adds itself to the puzzlestatistics to which it belongs
	public Session(@NotNull LocalDateTime startSessionTime, @NotNull Configuration configuration,
				   @NotNull PuzzleType puzzleType, @NotNull SolutionDao solutionDao) {
		this.dateStarted = startSessionTime;
		this.configuration = configuration;
		this.puzzleType = puzzleType;
		this.solutionDao = solutionDao;
		sessionPuzzleStatistics = new SessionPuzzleStatistics(this);
	}

	public LocalDateTime getStartTime() {
		return dateStarted;
	}

	@NotNull
	public SessionPuzzleStatistics getStatistics() {
		return sessionPuzzleStatistics;
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
		return new Session(LocalDateTime.now(), configuration, getPuzzleType(), solutionDao);
	}

	public int getAttemptsCount() {
		return solutions.size();
	}

	public Solution getSolution(int solutionIndex) {
		return solutions.get(solutionIndex);
	}

	public void addSolution(Solution solution, Runnable notifier) {
		LOG.info("add solution {}", solution);
		this.solutions.add(solution);
		solutionDao.insertSolution(solution);
		// todo it's will recalculate whole statistics. Can be optimized
		this.getStatistics().refresh(notifier);
	}

	public void removeSolution(Solution solution, Runnable notifier) {
		LOG.info("remove solution {}", solution);
		this.solutions.remove(solution);
		solutionDao.deleteSolution(solution);
		// todo it's will recalculate whole statistics. Can be optimized
		this.getStatistics().refresh(notifier);

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

	public RollingAverage getRollingAverage(RollingAverageOf ra, int count, int toIndex) {
		return RollingAverage.create(ra, this, toIndex, count);
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

	@NotNull
	public Solution getLastSolution() {
		return getSolution(getAttemptsCount() - 1);
	}
}
