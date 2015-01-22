package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class Session extends Commentable implements Comparable<Session> {

	private Statistics s;

	private PuzzleStatistics puzzStats;

	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private final StatisticsTableModel statsModel;

	//adds itself to the puzzlestatistics to which it belongs
	public Session(LocalDateTime d, Configuration configuration,
				   ScramblePluginManager scramblePluginManager, StatisticsTableModel statsModel) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.statsModel = statsModel;
		s = new Statistics(configuration, d);
	}

	public Statistics getStatistics() {
		return s;
	}

	private ScrambleCustomization sc;

	//this should only be called by PuzzleStatistics
	public void setPuzzleStatistics(PuzzleStatistics puzzStats, Profile profile) {
		sc = scramblePluginManager.getCustomizationFromString(profile, puzzStats.getCustomization());
		s.setCustomization(sc);
		this.puzzStats = puzzStats;
	}

	public ScrambleCustomization getCustomization() {
		return sc;
	}

	public PuzzleStatistics getPuzzleStatistics() {
		return puzzStats;
	}

	public int hashCode() {
		return s.getStartDate().hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof Session) {
			Session o = (Session) obj;
			return o.s.getStartDate().equals(this.s.getStartDate());
		}
		return false;
	}
	@Override
	public int compareTo(@NotNull Session o) {
		return this.s.getStartDate().compareTo(o.s.getStartDate());
	}

	public String toDateString() {
		return configuration.getDateFormat().format(s.getStartDate());
	}

	public String toString() {
		return toDateString();
	}

	public void setCustomization(String customization, Profile profile) {
		if(!customization.equals(puzzStats.getCustomization())) {
			puzzStats.removeSession(this);
			puzzStats = puzzStats.getPuzzleDatabase().getPuzzleStatistics(customization);
			puzzStats.addSession(this, profile);
			sc = scramblePluginManager.getCustomizationFromString(profile, puzzStats.getCustomization());
			s.setCustomization(sc);

			statsModel.fireStringUpdates();
		}
	}
	public void delete() {
		puzzStats.removeSession(this);
	}
}
