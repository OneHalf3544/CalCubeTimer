package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * <p>
 * Created: 08.10.2015 8:51
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class SessionsList implements Iterable<Session> {

	private static final Logger LOG = LogManager.getLogger(SessionsList.class);

	protected final Configuration configuration;
	private final SolutionDao solutionDao;
	private final ScramblePluginManager scramblePluginManager;
	protected final CalCubeTimerModel cubeTimerModel;

	private List<StatisticsUpdateListener> statisticsUpdateListeners = new ArrayList<>();

	private List<Session> sessions = new ArrayList<>();

	protected List<SessionListener> listener = new ArrayList<>();
	private Map<PuzzleType, GlobalPuzzleStatistics> statisticsByType = new HashMap<>();

	private Session currentSession;

	@Inject
	public SessionsList(Configuration configuration, CalCubeTimerModel cubeTimerModel,
						SolutionDao solutionDao, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.cubeTimerModel = cubeTimerModel;
		this.solutionDao = solutionDao;
		this.scramblePluginManager = scramblePluginManager;
		this.currentSession =  new Session(LocalDateTime.now(), configuration, scramblePluginManager.NULL_PUZZLE_TYPE, solutionDao);
		sessions.add(currentSession);
	}

	public GlobalPuzzleStatistics getGlobalPuzzleStatisticsForType(@NotNull PuzzleType puzzleType) {
		GlobalPuzzleStatistics globalPuzzleStatistics = statisticsByType.computeIfAbsent(
				puzzleType, pt -> new GlobalPuzzleStatistics(pt, this));
		globalPuzzleStatistics.refreshStats();
		return globalPuzzleStatistics;
	}

	@Override
	public Iterator<Session> iterator() {
		return sessions.iterator();
	}

	public Session getNthSession(int n) {
		return sessions.get(n);
	}

	public ImmutableList<Session> getSessions() {
		return ImmutableList.copyOf(sessions);
	}

	public void setSessions(List</*todo puzzletype?*/Session> sessions) {
		LOG.info("setSessions (count = {})", sessions.size());
		this.sessions = sessions;

		this.statisticsByType = new HashMap<>();
		sessions.stream()
				.map(Session::getPuzzleType)
				.forEach(this::getGlobalPuzzleStatisticsForType);

		this.loadOrCreateLatestSession(scramblePluginManager.NULL_PUZZLE_TYPE);
	}

	public void addSessionListener(@NotNull SessionListener sl) {
		listener.add(sl);
	}

	public void sendSessionToAnotherProfile(Session[] sessions, String anotherProfileName, ProfileDao profileDao) {
		ProfileEntity anotherProfile = profileDao.loadProfileEntity(anotherProfileName);

		for(Session session : sessions) {
			removeSession(session);
		}

		profileDao.moveSessionsTo(sessions, anotherProfile);
	}


	public void addSession(Session session) {
		LOG.info("addSession {}", session);
		sessions.add(session);
		solutionDao.saveSession(cubeTimerModel.getSelectedProfile(), session);
		configuration.addConfigurationChangeListener(profile -> session.getStatistics().refresh(this::fireStringUpdates));
		getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats();
		statisticsUpdateListeners.add(() -> listener.forEach(sl -> sl.sessionStatisticsChanged(session)));
		listener.forEach(sessionListener -> sessionListener.sessionAdded(session));
		fireStringUpdates();
	}

	public void removeSession(Session removedSession) {
		sessions.remove(removedSession);
		solutionDao.deleteSession(removedSession);
		getGlobalPuzzleStatisticsForType(removedSession.getPuzzleType()).refreshStats();

		if (currentSession == removedSession) {
			loadOrCreateLatestSession(removedSession.getPuzzleType());
		}
		listener.forEach(SessionListener::sessionsDeleted);
	}

	private void loadOrCreateLatestSession(PuzzleType puzzleType) {
		removeNullSessions();
		LOG.debug("try to load newest session");
		Optional<Session> nextSessionOptional = getSessions().stream()
                .max(Comparator.comparing(Session::getStartTime));

		if (nextSessionOptional.isPresent()) {
            LOG.debug("newest session found");
            setCurrentSession(nextSessionOptional.get());
        } else {
            createSession(puzzleType);
            LOG.info("create new session: {}", getCurrentSession());
        }
	}

	private void removeNullSessions() {
		LOG.info("remove nullSessions");
		List<Session> emptySessions = sessions.stream()
				.filter(s -> s.getPuzzleType() == scramblePluginManager.NULL_PUZZLE_TYPE)
				.collect(toList());
		sessions.removeAll(emptySessions);
	}

	@NotNull
	public Session getCurrentSession() {
		return currentSession;
	}

	public void setCurrentSession(Session session) {
		if (session == getCurrentSession()) {
			return;
		}
		LOG.info("setCurrentSession {}", session);
		if (!sessions.contains(session)) {
			addSession(session);
		}
		this.currentSession = session;
		listener.forEach(l -> l.sessionSelected(session));
		fireStringUpdates();
	}

	public void createSession(PuzzleType puzzleType) {
		addSession(new Session(LocalDateTime.now(), configuration, puzzleType, solutionDao));
	}

	public void addStatisticsUpdateListener(StatisticsUpdateListener listener) {
		LOG.trace("addStatisticsUpdateListener: {}", listener);
		statisticsUpdateListeners.add(listener);
	}

	//this is needed to update the i18n text
	public void fireStringUpdates() {
		LOG.debug("StatisticsTableModel.fireStringUpdates()");
		ImmutableList.copyOf(statisticsUpdateListeners).forEach(StatisticsUpdateListener::statisticsUpdated);
	}


	public void addSolutionToCurrentSession(Solution solution) {
		getCurrentSession().addSolution(solution, this::fireStringUpdates);
	}

	public void setComment(String comment, int index) {
		getCurrentSession().getSolution(index).setComment(comment);
		getCurrentSession().getStatistics().refresh(this::fireStringUpdates);
	}

	public void deleteSolutions(Solution[] solutions) {
		for (Solution solution : solutions) {
			getCurrentSession().removeSolution(solution, () -> {});
		}
		fireStringUpdates();
	}
}
