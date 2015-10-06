package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.lambda.tuple.Tuple2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.toMap;

public class SessionsListTableModel extends DraggableJTableModel {

	private static final Logger LOG = LogManager.getLogger(SessionsListTableModel.class);

	private static final String SEND_TO_PROFILE = "sendToProfile";

	private Map<PuzzleType, SessionsListAndPuzzleStatistics> mapScrambleTypeStatistics = new HashMap<>();

	private final Configuration configuration;
	private final ProfileDao profileDao;
	private final CalCubeTimerModel cubeTimerModel;
	private final CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

	private List<Session> sessionCache = new ArrayList<>();
	private SessionListener l;

	private String[] columnNames = new String[] {
			"ProfileDatabase.datestarted",
			"ProfileDatabase.customization",
			"ProfileDatabase.sessionaverage",
			"ProfileDatabase.bestra0",
			"ProfileDatabase.bestra1",
			"ProfileDatabase.besttime",
			"ProfileDatabase.stdev",
			"ProfileDatabase.solvecount",
			"ProfileDatabase.comment" };

	private Class<?>[] columnClasses = new Class<?>[] {
			Session.class,
			PuzzleType.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			Integer.class,
			String.class
	};

	public SessionsListTableModel(Configuration configuration, ProfileDao profileDao,
								  CalCubeTimerModel cubeTimerModel,
								  CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel) {
		this.configuration = configuration;
		this.profileDao = profileDao;
		this.cubeTimerModel = cubeTimerModel;
		this.currentSessionSolutionsTableModel = currentSessionSolutionsTableModel;
	}

	private List<SessionsListAndPuzzleStatistics> getPuzzlesStatistics() {
		return new ArrayList<>(mapScrambleTypeStatistics.values());
	}

	public List<PuzzleType> getCustomizations() {
		return new ArrayList<>(mapScrambleTypeStatistics.keySet());
	}

	public SessionsListAndPuzzleStatistics getPuzzleStatisticsForType(PuzzleType customization) {
		SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics = mapScrambleTypeStatistics.get(customization);
		if(sessionsListAndPuzzleStatistics == null) {
			sessionsListAndPuzzleStatistics = new SessionsListAndPuzzleStatistics(customization, this, configuration, currentSessionSolutionsTableModel);
			mapScrambleTypeStatistics.put(customization, sessionsListAndPuzzleStatistics);
		}
		return sessionsListAndPuzzleStatistics;
	}
	
	public void removeEmptySessions() {
		for(SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics : getPuzzlesStatistics()) {
			for(Session session : sessionsListAndPuzzleStatistics.toSessionIterable()) {
				if(session.getStatistics().getAttemptCount() == 0) {
					sessionsListAndPuzzleStatistics.removeSession(session);
				}
			}
		}
	}

	public int getDatabaseTypeCount(SolveType t) {
		int c = 0;
		for(SessionsListAndPuzzleStatistics ps : mapScrambleTypeStatistics.values())
			c += ps.getSolveTypeCount(t);
		return c;
	}
	
	public Session getNthSession(int n) {
		return sessionCache.get(n);
	}

	public ImmutableList<Session> getSessions() {
		return ImmutableList.copyOf(sessionCache);
	}

	public void setSessions(List<Session> sessions) {
		mapScrambleTypeStatistics = sessions.stream()
				.map(s -> new Tuple2<>(s.getCustomization(), s.getSessionsListAndPuzzleStatistics()))
				.collect(toMap(t -> t.v1, t -> t.v2));
		fireTableDataChanged();
	}

	public void setSessionListener(SessionListener sl) {
		l = sl;
	}

	private void fireSessionsDeleted() {
		if(l != null) {
			l.sessionsDeleted();
		}
	}
	
	//DraggableJTableModel methods
	
	@Override
	public void fireTableDataChanged() {
		updateSessionCache();
		super.fireTableDataChanged();
	}

	private void updateSessionCache() {
		sessionCache.clear();
		for(SessionsListAndPuzzleStatistics sessionsListAndPuzzleStatistics : getPuzzlesStatistics()) {
			for (Session s : sessionsListAndPuzzleStatistics.toSessionIterable()) {
				sessionCache.add(s);
			}
		}
	}

	@Override
	public String getColumnName(int column) {
		return StringAccessor.getString(columnNames[column]);
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return sessionCache.size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClasses[columnIndex];
	}

	@Override
	public Object getValueAt(int row, int col) {
		Session s = getNthSession(row);
		switch(col) {
		case 0: //data started
			return s;
		case 1: //customization
			return s.getCustomization();
		case 2: //session average
			return s.getStatistics().average(AverageType.SESSION, 0);
		case 3: //best ra0
			return s.getStatistics().getBestAverage(0);
		case 4: //best ra1
			return s.getStatistics().getBestAverage(1);
		case 5: //best time
			return s.getStatistics().getBestTime();
		case 6: //stdev
			return s.getStatistics().standardDeviation(AverageType.SESSION, 0);
		case 7: //solve count
			return s.getStatistics().getSolveCount();
		case 8: //comment
			return s.getComment();
		default:
			return null;
		}
	}
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(columnIndex == 8 && value instanceof String) {
			getNthSession(rowIndex).setComment((String) value);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// allow comment modification
		return columnIndex == 8;
	}

	@Override
	public void insertValueAt(Object value, int rowIndex) {
		//this only gets called if dragging is enabled
	}

	@Override
	public void deleteRows(int[] indices) {
		for(int ch = indices.length - 1; ch >= 0; ch--) {
			getNthSession(indices[ch]).delete();
		}
		fireTableDataChanged();
		fireSessionsDeleted();
	}

	@Override
	public boolean isRowDeletable(int rowIndex) {
		return true;
	}

	@Override
	public void removeRows(int[] indices) {
		deleteRows(indices);
	}

	@Override
	public String getToolTip(int rowIndex, int columnIndex) {
		String t = getNthSession(rowIndex).getComment();
		return t.isEmpty() ? null : t;
	}

	@Override
	public void showPopup(MouseEvent e, final DraggableJTable source, Component prevFocusOwner) {
		JPopupMenu jpopup = new JPopupMenu();

		JMenuItem discard = new JMenuItem(StringAccessor.getString("ProfileDatabase.discard"));
		discard.addActionListener(e1 -> source.deleteSelectedRows(false));
		jpopup.add(discard);
		
		JMenu sendTo = new JMenu(StringAccessor.getString("ProfileDatabase.sendto"));
		for(Profile profile : profileDao.getProfiles()) {
			if(profile == cubeTimerModel.getSelectedProfile()) {
				continue;
			}
			sendTo.add(createProfileMenuItem(source, profile));
		}
		jpopup.add(sendTo);
		
		source.requestFocusInWindow();
		jpopup.show(e.getComponent(), e.getX(), e.getY());
	}

	private JMenuItem createProfileMenuItem(DraggableJTable source, Profile p) {
		JMenuItem profileMenuItem = new JMenuItem(p.getName());
		String rows = Joiner.on(",").join(Arrays.asList(source.getSelectedRows()));

		profileMenuItem.setActionCommand(SEND_TO_PROFILE + rows);
		profileMenuItem.addActionListener(this::sendSessionToAnotherProfile);
		return profileMenuItem;
	}

	public void sendSessionToAnotherProfile(ActionEvent e) {
		Profile anotherProfile = profileDao.loadProfile(((JMenuItem) e.getSource()).getText());

		String[] rows = e.getActionCommand().substring(SEND_TO_PROFILE.length()).split(",");

		Session[] sessions = new Session[rows.length];
		for(int ch = 0; ch < rows.length; ch++) {
            int row = Integer.parseInt(rows[ch]);
            sessions[ch] = getNthSession(row);
        }

		for(Session session : sessions) {
            getPuzzleStatisticsForType(session.getCustomization()).removeSession(session);
        }
		fireSessionsDeleted();

		profileDao.moveSessionsTo(sessions, anotherProfile);
	}
}
