package net.gnehzr.cct.statistics;

import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.main.CurrentProfileHolder;
import net.gnehzr.cct.scrambles.PuzzleType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Session implements Commentable, Comparable<Session> {

	private static final Logger LOG = LogManager.getLogger(Session.class);

	private Long sessionId;
	private SessionSolutionsStatistics sessionSolutionsStatistics;
	private final LocalDateTime dateStarted;
	private final PuzzleType puzzleType;
	private final List<Solution> solutions = new ArrayList<>();
	private final SolutionDao solutionDao;
	private String comment = "";
	@NotNull
	private final CurrentProfileHolder currentProfileHolder;

	public Session(@NotNull LocalDateTime startSessionTime, @NotNull PuzzleType puzzleType,
				   @NotNull SolutionDao solutionDao, @NotNull CurrentProfileHolder currentProfileHolder) {
		this.dateStarted = startSessionTime;
		this.puzzleType = puzzleType;
		this.solutionDao = solutionDao;
		this.currentProfileHolder = currentProfileHolder;
		sessionSolutionsStatistics = new SessionSolutionsStatistics(this);
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	public void saveCommentInDB(String comment) {
		setComment(comment);
		solutionDao.saveSession(currentProfileHolder.getSelectedProfile(), this);
	}

	@Override
	public String getComment() {
		return comment == null ? "" : comment;
	}

	public LocalDateTime getStartTime() {
		return dateStarted;
	}

	@NotNull
	public SessionSolutionsStatistics getStatistics() {
		return sessionSolutionsStatistics;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
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
		ProfileEntity profile = new ProfileEntity();
		profile.setProfileId(profileId);

		return toSessionEntity(profile);
	}

	@NotNull
	public SessionEntity toSessionEntity(ProfileEntity profile) {
		SessionEntity sessionEntity = new SessionEntity()
				.withPluginName(getPuzzleType().getScramblePlugin().getPuzzleName())
				.withVariationName(getPuzzleType().getVariationName());
		sessionEntity.setSessionId(sessionId);
		sessionEntity.setSessionStart(getStartTime());
		sessionEntity.setComment(comment);
		sessionEntity.setProfile(profile);
		sessionEntity.setSolutions(Seq
				.iterate(0, i -> i++)
				.limit(sessionSolutionsStatistics.getSolveCounter().getSolveCount())
				.map(this::getSolution)
				.map(s -> s.toEntity(sessionEntity))
				.toList());
		return sessionEntity;
	}


	public SessionEntity toSessionEntityForDeletion(Long profileId) {
		SessionEntity sessionEntity = new SessionEntity()
				.withPluginName(getPuzzleType().getScramblePlugin().getPuzzleName())
				.withVariationName(getPuzzleType().getVariationName());
		sessionEntity.setSessionId(sessionId);
		sessionEntity.setSessionStart(getStartTime());
		sessionEntity.setComment(comment);

		ProfileEntity profile = new ProfileEntity();
		profile.setProfileId(profileId);
		sessionEntity.setProfile(profile);
		return sessionEntity;
	}

	public int getAttemptsCount() {
		return solutions.size();
	}

	public Solution getSolution(int solutionIndex) {
		return solutions.get(solutionIndex);
	}

	public void addSolution(Session currentSession, Solution solution, Runnable notifier) {
		LOG.info("add solution {}", solution);
		this.solutions.add(solution);
		solutionDao.insertSolution(currentSession, solution);
		// todo it's will recalculate whole statistics. Can be optimized
		this.getStatistics().refresh(notifier);
	}

	public void setSolutions(List<Solution> solutions) {
		LOG.trace("add solutions (size: {})", solutions.size());
		this.solutions.clear();
		this.solutions.addAll(solutions);
		this.getStatistics().refresh(() -> {});
	}

	public void removeSolution(Solution solution, Runnable notifier) {
		LOG.info("remove solution {}", solution);
		this.solutions.remove(solution);
		solutionDao.deleteSolution(solution, sessionId);
		this.getStatistics().refresh(notifier);

	}

	@Override
	public String toString() {
		return "Session{" +
				"sessionId=" + sessionId +
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
				solutions.size(), false, null);
	}

	public List<Solution> getSolutionList() {
		return Collections.unmodifiableList(solutions);
	}

	@NotNull
	public Solution getLastSolution() {
		return getSolution(getAttemptsCount() - 1);
	}
}
