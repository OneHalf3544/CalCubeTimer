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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.lambda.tuple.Tuple2;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.stream.Collectors.toMap;

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

	private static final Logger LOG = LogManager.getLogger(SessionsListTableModel.class);

	protected final Configuration configuration;
	private final SolutionDao solutionDao;
	protected final CalCubeTimerModel cubeTimerModel;
	protected final CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

	private CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();

	protected SessionListener listener;
	private Map<PuzzleType, GlobalPuzzleStatistics> statisticsByType = new HashMap<>();

	@Inject
	public SessionsList(CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel,
						Configuration configuration, CalCubeTimerModel cubeTimerModel,
						SolutionDao solutionDao) {
		this.currentSessionSolutionsTableModel = currentSessionSolutionsTableModel;
		this.configuration = configuration;
		this.cubeTimerModel = cubeTimerModel;
		this.solutionDao = solutionDao;
	}

	public GlobalPuzzleStatistics getGlobalPuzzleStatisticsForType(PuzzleType puzzleType) {
		GlobalPuzzleStatistics globalPuzzleStatistics = statisticsByType.computeIfAbsent(puzzleType,
				pt -> {
					GlobalPuzzleStatistics gPuzzleStat = new GlobalPuzzleStatistics(pt, this);
					// We need some way for each profile database to listen for updates,
					// this seems fine to me, although nasty
					currentSessionSolutionsTableModel.addStatisticsUpdateListener(gPuzzleStat::refreshStats);
					return gPuzzleStat;
				});
		globalPuzzleStatistics.refreshStats();
		return globalPuzzleStatistics;
	}

	public void removeEmptySessions() {
		for(Session session : this) {
			if(session.getPuzzleType().isNullType() || session.getAttemptsCount() == 0) {
                removeSession(session);
            }
        }
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
		statisticsByType = sessions.stream()
				.map(s -> new Tuple2<>(s.getPuzzleType(), getGlobalPuzzleStatisticsForType(s.getPuzzleType())))
				.collect(toMap(t -> t.v1, t -> t.v2));
	}

	public void setSessionListener(SessionListener sl) {
		listener = sl;
	}

	public void sendSessionToAnotherProfile(Session[] sessions, String anotherProfileName, ProfileDao profileDao) {
		ProfileEntity anotherProfile = profileDao.loadProfileEntity(anotherProfileName);

		for(Session session : sessions) {
			removeSession(session);
		}

		profileDao.moveSessionsTo(sessions, anotherProfile);
	}


	public void addSession(Session session) {
		sessions.add(session);

		currentSessionSolutionsTableModel.setCurrentSession(session);

		solutionDao.saveSession(cubeTimerModel.getSelectedProfile(), session);
		getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats();
		//sessionsListTableModel.fireTableDataChanged();
	}

	public void removeSession(Session session) {
		sessions.remove(session);
		solutionDao.deleteSession(session);
		getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats();
		//sessionsListTableModel.fireTableDataChanged();
	}

	public boolean containsSession(Session s) {
		return sessions.contains(s);
	}

	public Session getCurrentSession() {
		return currentSessionSolutionsTableModel.getCurrentSession();
	}

}
