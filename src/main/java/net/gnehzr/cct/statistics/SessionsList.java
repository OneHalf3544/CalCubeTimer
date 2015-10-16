package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.ProfileEntity;
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
public class SessionsList implements Iterable<Session> {

	private static final Logger LOG = LogManager.getLogger(SessionsListTableModel.class);

	protected final Configuration configuration;
	protected final ProfileDao profileDao;
	protected final CalCubeTimerModel cubeTimerModel;
	protected final CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

	private CopyOnWriteArrayList<Session> sessions = new CopyOnWriteArrayList<>();

	protected SessionListener listener;
	private Map<PuzzleType, GlobalPuzzleStatistics> statisticsByType = new HashMap<>();

	public SessionsList(CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel, Configuration configuration, CalCubeTimerModel cubeTimerModel, ProfileDao profileDao) {
		this.currentSessionSolutionsTableModel = currentSessionSolutionsTableModel;
		this.configuration = configuration;
		this.cubeTimerModel = cubeTimerModel;
		this.profileDao = profileDao;
	}

	public List<PuzzleType> getUsedPuzzleTypes() {
		return new ArrayList<>(statisticsByType.keySet());
	}

	public GlobalPuzzleStatistics getGlobalPuzzleStatisticsForType(PuzzleType puzzleType) {
		GlobalPuzzleStatistics globalPuzzleStatistics = statisticsByType.computeIfAbsent(puzzleType,
				pt -> new GlobalPuzzleStatistics(pt, currentSessionSolutionsTableModel));
		globalPuzzleStatistics.refreshStats(this);
		return globalPuzzleStatistics;
	}

	public void removeEmptySessions() {
		for(Session session : this) {
            if(session.getStatistics().getSolveCounter().getAttemptCount() == 0) {
                removeSession(session);
            }
        }
	}

	@Override
	public Iterator<Session> iterator() {
		return sessions.iterator();
	}

	public int getDatabaseTypeCount(SolveType t) {
		int c = 0;
		for(GlobalPuzzleStatistics ps : statisticsByType.values())
			c += ps.getSolveCounter().getSolveTypeCount(t);
		return c;
	}

	public Session getNthSession(int n) {
		return sessions.get(n);
	}

	public ImmutableList<Session> getSessions() {
		return ImmutableList.copyOf(sessions);
	}

	public void setSessions(List<Session> sessions) {
		statisticsByType = sessions.stream()
				.map(s -> new Tuple2<>(s.getPuzzleType(), s.getSessionsList().getGlobalPuzzleStatisticsForType(s.getPuzzleType())))
				.collect(toMap(t -> t.v1, t -> t.v2));
	}

	public void setSessionListener(SessionListener sl) {
		listener = sl;
	}

	public void sendSessionToAnotherProfile(Session[] sessions, String anotherProfileName) {
		ProfileEntity anotherProfile = profileDao.loadProfileEntity(anotherProfileName);

		for(Session session : sessions) {
			removeSession(session);
		}

		profileDao.moveSessionsTo(sessions, anotherProfile);
	}


	public void addSession(Session session, Profile selectedProfile) {
/*
		if (!sessions.containsSession(newSession)) {
			sessions.addSession(newSession, selectedProfile);
		}
		if (oldSession.getPuzzleType().isNullType()) {
			sessions.removeSession(oldSession);
		}
*/

		currentSessionSolutionsTableModel.setCurrentSession(session);

		sessions.add(session);
		session.setSessionsList(this);
		session.getSessionsList().getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats(this);
		//sessionsListTableModel.fireTableDataChanged();
	}

	public void removeSession(Session session) {
		sessions.remove(session);
		session.getSessionsList().getGlobalPuzzleStatisticsForType(session.getPuzzleType()).refreshStats(this);
		//sessionsListTableModel.fireTableDataChanged();
	}

	public boolean containsSession(Session s) {
		return sessions.contains(s);
	}

	public Session getCurrentSession() {
		return currentSessionSolutionsTableModel.getCurrentSession();
	}

}
