package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.scrambles.PuzzleType;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

import java.time.LocalDateTime;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;

public class Session extends Commentable implements Comparable<Session> {

	private Long lastSessionId;
	private Statistics statistics;
	private SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics;
	private final Configuration configuration;
	private final PuzzleType puzzleType;

	//adds itself to the puzzlestatistics to which it belongs
	public Session(LocalDateTime startSessionTime, Configuration configuration,
				   PuzzleType puzzleType) {
		this.configuration = configuration;
		this.puzzleType = puzzleType;
		statistics = new Statistics(configuration, startSessionTime, puzzleType);
	}

	public Statistics getStatistics() {
		return statistics;
	}

	//this should only be called by PuzzleStatistics
	public void setSessionsListAndPuzzleStatistics(SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics) {
		checkArgument(sessionsListAndPuzzleStatistics.getCustomization().equals(puzzleType));

		/*scrambleCustomization = puzzleStatistics.getCustomization();
		statistics.setCustomization(scrambleCustomization);*/
		this.sessionsListAndPuzzleStatistics = sessionsListAndPuzzleStatistics;
	}

	public Long getSessionId() {
		return lastSessionId;
	}

	public void setSessionId(Long lastSessionId) {
		this.lastSessionId = lastSessionId;
	}

	public PuzzleType getCustomization() {
		return puzzleType;
	}

	public SessionsListAndPuzzleStatistics getSessionsListAndPuzzleStatistics() {
		return sessionsListAndPuzzleStatistics;
	}

	@Override
	public int hashCode() {
		return statistics.getStartTime().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session o = (Session) obj;
			return o.statistics.getStartTime().equals(this.statistics.getStartTime());
		}
		return false;
	}
	@Override
	public int compareTo(@NotNull Session o) {
		return Comparator.comparing((Session s) -> s.statistics.getStartTime()).compare(this, o);
	}

	public String toDateString() {
		return configuration.getDateFormat().format(statistics.getStartTime());
	}

	public String toString() {
		return toDateString();
	}

	public void delete() {
		sessionsListAndPuzzleStatistics.removeSession(this);
	}

	public SessionEntity toSessionEntity(Long profileId) {
		SessionEntity sessionEntity = new SessionEntity();
		sessionEntity.setSessionId(lastSessionId);
		sessionEntity.setScrambleCustomization(puzzleType.getCustomization());
		sessionEntity.setSessionStart(getStatistics().getStartTime());
		ProfileEntity profile = new ProfileEntity();
		profile.setProfileId(profileId);
		sessionEntity.setProfileId(profile);
		sessionEntity.setSolutions(Seq
				.iterate(0, i -> i++)
				.limit(statistics.getSolveCount())
				.map(statistics::get)
				.map(Solution::toEntity)
				.toList());
		return sessionEntity;
	}
}
