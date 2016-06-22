package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.main.CurrentProfileHolder;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>
 * <p>
 * Created: 08.10.2015 8:51
 * <p>
 *
 * @author OneHalf
 */
@Service
public class SessionsList implements Iterable<Session> {

	private static final Logger LOG = LogManager.getLogger(SessionsList.class);

	protected final Configuration configuration;
	private final SolutionDao solutionDao;
	private final ScramblePluginManager scramblePluginManager;
	protected final CurrentProfileHolder currentProfileHolder;

	private List<StatisticsUpdateListener> statisticsUpdateListeners = new ArrayList<>();

	private List<Session> sessions = new ArrayList<>();

	private List<SessionListener> listener = new ArrayList<>();
	private Map<PuzzleType, GlobalPuzzleStatistics> statisticsByType = new HashMap<>();

	private Session currentSession;

	@Autowired
	public SessionsList(Configuration configuration, CurrentProfileHolder currentProfileHolder,
						SolutionDao solutionDao, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.currentProfileHolder = currentProfileHolder;
		this.solutionDao = solutionDao;
		this.scramblePluginManager = scramblePluginManager;
		this.currentSession =  new Session(
				LocalDateTime.now(), scramblePluginManager.getDefaultPuzzleType(), solutionDao, currentProfileHolder);
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

	public void setSessions(List<Session> sessions) {
		LOG.info("setSessions (count = {})", sessions.size());
		this.sessions = sessions;

		this.statisticsByType = new HashMap<>();
		sessions.stream()
				.map(Session::getPuzzleType)
				.forEach(this::getGlobalPuzzleStatisticsForType);

		Optional<Session> lastSession = findSessionById(sessions, currentProfileHolder.getSelectedProfile().getLastSessionId());

		if (lastSession.isPresent()) {
			setCurrentSession(lastSession.get());
		} else {
			this.loadOrCreateLatestSession(scramblePluginManager.getDefaultPuzzleType());
		}

		listener.forEach(sessionListener -> sessionListener.sessionAdded(getCurrentSession()));
	}

	private Optional<Session> findSessionById(List<Session> sessions, Long lastSessionId) {
		if (lastSessionId == null) {
			return Optional.empty();
		}
		return sessions.stream()
				.filter(s -> Objects.equals(s.getSessionId(), lastSessionId))
				.findAny();
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
		solutionDao.saveSession(currentProfileHolder.getSelectedProfile(), session);
		configuration.addConfigurationChangeListener(profile -> session.getStatistics().refresh(this::fireStringUpdates));
		getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats();
		statisticsUpdateListeners.add(() -> listener.forEach(sl -> sl.sessionStatisticsChanged(session)));
		listener.forEach(sessionListener -> sessionListener.sessionAdded(session));
		fireStringUpdates();
	}

	public void removeSession(Session removedSession) {
		sessions.remove(removedSession);
		solutionDao.deleteSession(removedSession, currentProfileHolder.getSelectedProfile().toEntity());
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
            LOG.info("create new session");
            createSession(puzzleType);
        }
	}

	private void removeNullSessions() {
		ImmutableList.copyOf(sessions).stream()
				.filter(session -> session.getAttemptsCount() == 0)
				.peek(emptySession -> LOG.info("remove empty session {}", emptySession))
				.forEach(this::removeSession);
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
		checkState(session.getSessionId() != null);
		this.currentSession = session;
		listener.forEach(l -> l.sessionSelected(session));
		fireStringUpdates();
	}

	public void createSession(PuzzleType puzzleType) {
		setCurrentSession(new Session(LocalDateTime.now(), puzzleType, solutionDao, currentProfileHolder));
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
		getCurrentSession().addSolution(getCurrentSession(), solution, this::fireStringUpdates);
	}

	public void setCommentToSolution(String comment, int index) {
		Solution solution = getCurrentSession().getSolution(index);
		solution.setComment(comment);
		solutionDao.updateSolution(getCurrentSession(), solution);
		getCurrentSession().getStatistics().refresh(this::fireStringUpdates);
	}

	public void setScrambleToSolution(ScrambleString scrambleString, int index) {
		Solution solution = getCurrentSession().getSolution(index);
		solution.setScrambleString(scrambleString);
		solutionDao.updateSolution(getCurrentSession(), solution);
		getCurrentSession().getStatistics().refresh(this::fireStringUpdates);
	}

	public void deleteSolutions(Solution[] solutions) {
		for (Solution solution : solutions) {
			getCurrentSession().removeSolution(solution, () -> {});
		}
		fireStringUpdates();
	}
}
