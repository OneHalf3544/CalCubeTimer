package net.gnehzr.cct.statistics;

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

	private final Configuration configuration;

	private UndoRedoListener undoRedoListener;

	private List<StatisticsUpdateListener> statsListeners = new ArrayList<>();

	private Session currentSession;

	@Inject
	public CurrentSessionSolutionsList(Configuration configuration,
									   ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		currentSession = new Session(LocalDateTime.now(), this.configuration, scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION);
	}

	public void setCurrentSession(Profile selectedProfile, @NotNull Session session, CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel) {
		this.currentSession = Objects.requireNonNull(session);
		Statistics statistics = Objects.requireNonNull(session.getStatistics());

		SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics = selectedProfile.getSessionsDatabase().getPuzzleStatisticsForType(session.getCustomization());
		if (!sessionsListAndPuzzleStatistics.containsSession(session)) {
			sessionsListAndPuzzleStatistics.addSession(session);
		}

		if(statistics != null) {
			statistics.setUndoRedoListener(null);
			statistics.setTableListener(null);
			statistics.setStatisticsUpdateListeners(null);
		}
		statistics = session.getStatistics();

		statistics.setTableListener(currentSessionSolutionsTableModel);
		statistics.setUndoRedoListener(undoRedoListener);
		statistics.setStatisticsUpdateListeners(statsListeners);

		statistics.notifyListeners(false);
	}

	@NotNull
	public Session getCurrentSession() {
		return currentSession;
	}

	public void setUndoRedoListener(@NotNull UndoRedoListener l) {
		this.undoRedoListener = Objects.requireNonNull(l);
	}

	public void addStatisticsUpdateListener(StatisticsUpdateListener l) {
		//This nastyness is to ensure that PuzzleStatistics have had a chance to see the change (see notifyListeners() in Statistics)
		//before the dynamicstrings
		if(l instanceof SessionsListAndPuzzleStatistics)
			statsListeners.add(0, l);
		else
			statsListeners.add(l);
	}

	public void removeStatisticsUpdateListener(StatisticsUpdateListener l) {
		statsListeners.remove(l);
	}

	//this is needed to update the i18n text
	public void fireStringUpdates() {
		LOG.debug("StatisticsTableModel.fireStringUpdates()");
		statsListeners.forEach(StatisticsUpdateListener::update);
		undoRedoListener.refresh();
	}

	public int getSize() {
		return currentSession.getStatistics().getAttemptCount();
	}

	public void addSolution(Solution solution, int rowIndex) {
		currentSession.getStatistics().add(rowIndex, (Solution) solution);
	}

	public void setSolution(Solution solution, int index) {
		currentSession.getStatistics().set(index, solution);
	}

	public void setComment(String comment, int index) {
		currentSession.getStatistics().get(index).setComment(comment);
	}

	public void deleteRows(int[] indices) {
		currentSession.getStatistics().remove(indices);
	}
}
