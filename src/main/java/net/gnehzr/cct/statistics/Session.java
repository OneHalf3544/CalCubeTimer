package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Comparator;

public class Session extends Commentable implements Comparable<Session> {

	private Statistics statistics;

	private PuzzleStatistics puzzleStatistics;

	private final Configuration configuration;
	private final StatisticsTableModel statisticsTableModel;

	//adds itself to the puzzlestatistics to which it belongs
	public Session(LocalDateTime d, Configuration configuration,
				   StatisticsTableModel statisticsTableModel) {
		this.configuration = configuration;
		this.statisticsTableModel = statisticsTableModel;
		statistics = new Statistics(configuration, d);
	}

	public Statistics getStatistics() {
		return statistics;
	}

	private ScrambleCustomization scrambleCustomization;

	//this should only be called by PuzzleStatistics
	public void setPuzzleStatistics(PuzzleStatistics puzzleStatistics) {
		scrambleCustomization = puzzleStatistics.getCustomization();
		statistics.setCustomization(scrambleCustomization);
		this.puzzleStatistics = puzzleStatistics;
	}

	public ScrambleCustomization getCustomization() {
		return scrambleCustomization;
	}

	public PuzzleStatistics getPuzzleStatistics() {
		return puzzleStatistics;
	}

	public int hashCode() {
		return statistics.getStartDate().hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session o = (Session) obj;
			return o.statistics.getStartDate().equals(this.statistics.getStartDate());
		}
		return false;
	}
	@Override
	public int compareTo(@NotNull Session o) {
		return Comparator.comparing((Session s) -> s.statistics.getStartDate()).compare(this, o);
	}

	public String toDateString() {
		return configuration.getDateFormat().format(statistics.getStartDate());
	}

	public String toString() {
		return toDateString();
	}

	public void setCustomization(@NotNull ScrambleCustomization customization, @NotNull Profile profile) {
		if (customization.equals(puzzleStatistics.getCustomization())) {
			return;
		}
		puzzleStatistics.removeSession(this);
		puzzleStatistics = puzzleStatistics.getPuzzleDatabase().getPuzzleStatistics(customization);
		puzzleStatistics.addSession(this);
		scrambleCustomization = puzzleStatistics.getCustomization();
		statistics.setCustomization(scrambleCustomization);

		statisticsTableModel.fireStringUpdates();
	}
	public void delete() {
		puzzleStatistics.removeSession(this);
	}

	public SessionEntity toSessionEntity() {
		SessionEntity sessionEntity = new SessionEntity();
		// todo
		return sessionEntity;
	}
}
