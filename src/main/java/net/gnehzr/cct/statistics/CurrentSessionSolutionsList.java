package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Singleton
public class CurrentSessionSolutionsList {

	private static final Logger LOG = LogManager.getLogger(CurrentSessionSolutionsList.class);

	private List<StatisticsUpdateListener> statisticsUpdateListeners = new ArrayList<>();
	private DraggableJTableModel tableListener;

	@NotNull
	private Session currentSession;

	@Inject
	private SolutionDao solutionDao;

	@Inject
	public CurrentSessionSolutionsList(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		currentSession = new Session(LocalDateTime.now(), configuration, scramblePluginManager.NULL_PUZZLE_TYPE);
	}

	public void setCurrentSession(@NotNull Session session) {
		this.currentSession = Objects.requireNonNull(session);
		LOG.info("setCurrentSession {}", session);

		notifyListeners();
	}

	public void setStatisticsUpdateListeners(List<StatisticsUpdateListener> listener) {
		statisticsUpdateListeners = listener;
	}

	public void setTableListener(DraggableJTableModel tableListener) {
		this.tableListener = tableListener;
	}

	public void notifyListeners() {
		if (tableListener != null) {
			tableListener.fireTableDataChanged();
		}
		fireStringUpdates();
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
	public void fireStringUpdates() {
		LOG.debug("StatisticsTableModel.fireStringUpdates()");
		ImmutableList.copyOf(statisticsUpdateListeners).forEach(StatisticsUpdateListener::update);
	}

	public int getSize() {
		return currentSession.getAttemptsCount();
	}

	public void addSolution(Solution solution) {
		solutionDao.insertSolution(solution);
		currentSession.addSolution(solution, this::notifyListeners);
	}

	public void setComment(String comment, int index) {
		currentSession.getSolution(index).setComment(comment);
		currentSession.getStatistics().refresh(this::notifyListeners);
	}

	public void deleteRows(int[] indices) {
		currentSession.getStatistics().refresh(this::notifyListeners);
	}
}
