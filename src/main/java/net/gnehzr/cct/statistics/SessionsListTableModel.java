package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.AverageType;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.RollingAverageOf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class SessionsListTableModel extends DraggableJTableModel {

	private static final String SEND_TO_PROFILE = "sendToProfile";

	private static final String[] columnNames = new String[] {
			"ProfileDatabase.datestarted",
			"ProfileDatabase.customization",
			"ProfileDatabase.sessionaverage",
			"ProfileDatabase.bestra0",
			"ProfileDatabase.bestra1",
			"ProfileDatabase.besttime",
			"ProfileDatabase.stdev",
			"ProfileDatabase.solvecount",
			"ProfileDatabase.comment" };

	private static final Class<?>[] columnClasses = new Class<?>[] {
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

	private final SessionsList sessionsList;

	@Inject
	public SessionsListTableModel(Configuration configuration, ProfileDao profileDao,
								  CalCubeTimerModel cubeTimerModel,
								  CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel) {
		this.sessionsList = new SessionsList(currentSessionSolutionsTableModel, configuration, cubeTimerModel, profileDao);
	}

	private void fireSessionsDeleted() {
		if(sessionsList.listener != null) {
			sessionsList.listener.sessionsDeleted();
		}
	}
	
	//DraggableJTableModel methods
	
	@Override
	public void fireTableDataChanged() {
		super.fireTableDataChanged();
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
		return sessionsList.getSessions().size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClasses[columnIndex];
	}

	@Override
	public Object getValueAt(int row, int col) {
		Session s = sessionsList.getNthSession(row);
		switch(col) {
		case 0: //data started
			return s;
		case 1: //customization
			return s.getPuzzleType();
		case 2: //session average
			return s.getSessionPuzzleStatistics().average(AverageType.SESSION_AVERAGE, null);
		case 3: //best ra0
			return s.getSessionPuzzleStatistics().getBestAverage(RollingAverageOf.OF_5);
		case 4: //best ra1
			return s.getSessionPuzzleStatistics().getBestAverage(RollingAverageOf.OF_12);
		case 5: //best time
			return s.getSessionPuzzleStatistics().getBestTime();
		case 6: //stdev
			return s.getSessionPuzzleStatistics().standardDeviation(AverageType.SESSION_AVERAGE, null);
		case 7: //solve count
			return s.getSessionPuzzleStatistics().getSolveCounter().getSolveCount();
		case 8: //comment
			return s.getComment();
		default:
			return null;
		}
	}
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(columnIndex == 8 && value instanceof String) {
			sessionsList.getNthSession(rowIndex).setComment((String) value);
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
			sessionsList.getNthSession(indices[ch]).delete();
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
		String t = sessionsList.getNthSession(rowIndex).getComment();
		return t.isEmpty() ? null : t;
	}

	@Override
	public void showPopup(MouseEvent e, final DraggableJTable source, Component prevFocusOwner) {
		JPopupMenu jpopup = new JPopupMenu();

		JMenuItem discard = new JMenuItem(StringAccessor.getString("ProfileDatabase.discard"));
		discard.addActionListener(e1 -> source.deleteSelectedRows(false));
		jpopup.add(discard);
		
		JMenu sendTo = new JMenu(StringAccessor.getString("ProfileDatabase.sendto"));
		for(ProfileEntity profile : sessionsList.profileDao.getProfileEntitiesExcept(
				sessionsList.cubeTimerModel.getSelectedProfile().getName())) {
			sendTo.add(createProfileMenuItem(source, profile));
		}
		jpopup.add(sendTo);
		
		source.requestFocusInWindow();
		jpopup.show(e.getComponent(), e.getX(), e.getY());
	}

	private JMenuItem createProfileMenuItem(DraggableJTable source, ProfileEntity profile) {
		JMenuItem profileMenuItem = new JMenuItem(profile.getName());
		String rows = Joiner.on(",").join(Arrays.asList(source.getSelectedRows()));

		profileMenuItem.setActionCommand(SEND_TO_PROFILE + rows);
		profileMenuItem.addActionListener(this::sendSessionToAnotherProfile);
		return profileMenuItem;
	}

	public void sendSessionToAnotherProfile(ActionEvent e) {
		String anotherProfileName = ((JMenuItem) e.getSource()).getText();
		String[] rows = e.getActionCommand().substring(SessionsListTableModel.SEND_TO_PROFILE.length()).split(",");

		Session[] sessions = new Session[rows.length];
		for(int ch = 0; ch < rows.length; ch++) {
			int row = Integer.parseInt(rows[ch]);
			sessions[ch] = sessionsList.getNthSession(row);
		}

		sessionsList.sendSessionToAnotherProfile(sessions, anotherProfileName);

		fireSessionsDeleted();
	}

	public SessionsList getSessionsList() {
		return sessionsList;
	}
}
