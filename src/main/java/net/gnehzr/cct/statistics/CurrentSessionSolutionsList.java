package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Singleton
public class CurrentSessionSolutionsList {

	private static final Logger LOG = LogManager.getLogger(CurrentSessionSolutionsList.class);

	private List<StatisticsUpdateListener> statisticsUpdateListeners = new ArrayList<>();

	@NotNull
	private Session currentSession;

	@Inject
	public CurrentSessionSolutionsList(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		currentSession = new Session(LocalDateTime.now(), configuration, scramblePluginManager.NULL_PUZZLE_TYPE);
	}

	public void setCurrentSession(@NotNull Session session) {
		this.currentSession = Objects.requireNonNull(session);
		LOG.info("setCurrentSession {}", session);
		SessionPuzzleStatistics sessionPuzzleStatistics = session.getSessionPuzzleStatistics();

		sessionPuzzleStatistics.notifyListeners();
	}

	@NotNull
	public Session getCurrentSession() {
		return currentSession;
	}

	public void addStatisticsUpdateListener(StatisticsUpdateListener listener) {
		LOG.trace("addStatisticsUpdateListener: {}", listener);
		statisticsUpdateListeners.add(listener);
	}

	public void removeStatisticsUpdateListener(StatisticsUpdateListener l) {
		statisticsUpdateListeners.remove(l);
	}

	//this is needed to update the i18n text
	public void fireStringUpdates(SessionsList sessionsList) {
		LOG.debug("StatisticsTableModel.fireStringUpdates()");
		ImmutableList.copyOf(statisticsUpdateListeners).forEach(e -> e.update(sessionsList));
	}

	public int getSize() {
		return currentSession.getAttemptsCount();
	}

	public void addSolution(Solution solution, int rowIndex) {
		currentSession.getSessionPuzzleStatistics().refresh();
	}

	public void setComment(String comment, int index) {
		currentSession.getSolution(index).setComment(comment);
		currentSession.getSessionPuzzleStatistics().refresh();
	}

	public void deleteRows(int[] indices) {
		currentSession.getSessionPuzzleStatistics().refresh();
	}
}
