package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.SessionEntity;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class SessionsTableModel extends DraggableJTableModel {

	private static final Logger LOG = LogManager.getLogger(SessionsTableModel.class);

	private Map<ScrambleCustomization, PuzzleStatistics> database = new HashMap<>();

	private final Configuration configuration;
	private final ProfileDao profileDao;
	private final StatisticsTableModel statsModel;
	private final ScramblePluginManager scramblePluginManager;

	public SessionsTableModel(Configuration configuration, ProfileDao profileDao,
							  StatisticsTableModel statsModel, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.profileDao = profileDao;
		this.statsModel = statsModel;
		this.scramblePluginManager = scramblePluginManager;
	}

	private List<PuzzleStatistics> getPuzzlesStatistics() {
		return new ArrayList<>(database.values());
	}

	public List<ScrambleCustomization> getCustomizations() {
		return new ArrayList<>(database.keySet());
	}

	public PuzzleStatistics getPuzzleStatistics(ScrambleCustomization customization) {
		PuzzleStatistics puzzleStatistics = database.get(customization);
		if(puzzleStatistics == null) {
			puzzleStatistics = new PuzzleStatistics(customization, this, configuration, statsModel);
			database.put(customization, puzzleStatistics);
		}
		return puzzleStatistics;
	}
	
	public void removeEmptySessions() {
		for(PuzzleStatistics puzzleStatistics : getPuzzlesStatistics()) {
			for(Session session : puzzleStatistics.toSessionIterable()) {
				if(session.getStatistics().getAttemptCount() == 0) {
					puzzleStatistics.removeSession(session);
				}
			}
		}
	}

	public int getDatabaseTypeCount(SolveType t) {
		int c = 0;
		for(PuzzleStatistics ps : database.values())
			c += ps.getSolveTypeCount(t);
		return c;
	}
	
	public Session getNthSession(int n) {
		return sessionCache.get(n);
	}

	public ImmutableList<Session> getSessions() {
		return ImmutableList.copyOf(sessionCache);
	}

	public int indexOf(Session findMe) {
		int n = 0;
		for(PuzzleStatistics ps : getPuzzlesStatistics()) {
			for(Session s : ps.toSessionIterable()) {
				if(s == findMe) {
					return n;
				}
				n++;
			}
		}
		return -1;
	}
	
	private SessionListener l;
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

	private List<Session> sessionCache = new ArrayList<>();

	private void updateSessionCache() {
		sessionCache.clear();
		for(PuzzleStatistics ps : getPuzzlesStatistics()) {
			for (Session s : ps.toSessionIterable()) {
				sessionCache.add(s);
			}
		}
	}
	
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
			ScrambleCustomization.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			Integer.class,
			String.class
	};

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
		if(columnIndex == 1 && value instanceof ScrambleCustomization) { //setting the customization
			ScrambleCustomization sc = (ScrambleCustomization) value;
			Session s = getNthSession(rowIndex);
			if(!s.getCustomization().equals(sc)) { //we're not interested in doing anything if they select the same customization
				s.setCustomization(sc, profileDao.getSelectedProfile());
				updateSessionCache();
				rowIndex = indexOf(s); //changing the customization will change the index in the model
				fireTableRowsUpdated(rowIndex, rowIndex);
			}
		} else if(columnIndex == 8 && value instanceof String) {
			getNthSession(rowIndex).setComment((String) value);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// allow modification of the session customization or comment
		return columnIndex == 1 || columnIndex == 8;
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

	private static final String SEND_TO_PROFILE = "sendToProfile";

	@Override
	public void showPopup(MouseEvent e, final DraggableJTable source, Component prevFocusOwner) {
		JPopupMenu jpopup = new JPopupMenu();

		JMenuItem discard = new JMenuItem(StringAccessor.getString("ProfileDatabase.discard"));
		discard.addActionListener(e1 -> source.deleteSelectedRows(false));
		jpopup.add(discard);
		
		JMenu sendTo = new JMenu(StringAccessor.getString("ProfileDatabase.sendto"));
		for(Profile profile : profileDao.getProfiles(configuration)) {
			if(profile == profileDao.getSelectedProfile()) {
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
		profileMenuItem.addActionListener(this::switchProfile);
		return profileMenuItem;
	}

	public void switchProfile(ActionEvent e) {
		Profile to = profileDao.getProfileByName(((JMenuItem) e.getSource()).getText());
		profileDao.loadDatabase(to, scramblePluginManager);

		String[] rows = e.getActionCommand().substring(SEND_TO_PROFILE.length()).split(",");

		Session[] sessions = new Session[rows.length];
		for(int ch = 0; ch < rows.length; ch++) {
            int row = Integer.parseInt(rows[ch]);
            sessions[ch] = getNthSession(row);
        }
		for(Session session : sessions) {
            ScrambleCustomization custom = session.getCustomization();
            session.delete();
            to.getSessionsDatabase().getPuzzleStatistics(custom).addSession(session);
        }
		fireSessionsDeleted();
		profileDao.saveDatabase(to);
	}

	public List<SessionEntity> toEntityList() {
		return Collections.singletonList(statsModel.getCurrentSession().toSessionEntity());
	}
}
