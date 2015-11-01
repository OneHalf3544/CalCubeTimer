package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.dao.ProfileEntity;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.misc.customJTable.SessionListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.Arrays;

@Singleton
public class SessionsListTableModel extends DraggableJTableModel {

	private static final String SEND_TO_PROFILE = "sendToProfile";

	private static final String[] COLUMN_NAMES = new String[] {
			"ProfileDatabase.datestarted",
			"ProfileDatabase.customization",
			"ProfileDatabase.sessionaverage",
			"ProfileDatabase.bestra0",
			"ProfileDatabase.bestra1",
			"ProfileDatabase.besttime",
			"ProfileDatabase.stdev",
			"ProfileDatabase.solvecount",
			"ProfileDatabase.comment" };

	private static final Class<?>[] COLUMN_CLASSES = new Class<?>[] {
			LocalDateTime.class,
			String.class,
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
	private ProfileDao profileDao;
	@Inject
	private Configuration configuration;

	@Inject
	public SessionsListTableModel(SessionsList sessionsList) {
		this.sessionsList = sessionsList;
		sessionsList.addSessionListener(new SessionListener() {
			@Override
			public void sessionSelected(Session s) { }

			@Override
			public void sessionAdded(Session session) {
				fireTableDataChanged();
			}

			@Override
			public void sessionStatisticsChanged(Session session) {
				fireTableDataChanged();
			}

			@Override
			public void sessionsDeleted() {
				fireTableDataChanged();
			}
		});
	}
	
	@Override
	public void fireTableDataChanged() {
		super.fireTableDataChanged();
	}

	@Override
	public String getColumnName(int column) {
		return StringAccessor.getString(COLUMN_NAMES[column]);
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		return sessionsList.getSessions().size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return COLUMN_CLASSES[columnIndex];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Session session = sessionsList.getNthSession(rowIndex);
		switch(columnIndex) {
		case 0: //data started
			return configuration.getDateFormat().format(session.getStartTime());
		case 1: //customization
			return session.getPuzzleType().getVariationName();
		case 2: //session average
			return session.getStatistics().getWholeSessionAverage().getAverage();
		case 3: //best ra0
			return session.getStatistics().getBestAverage(RollingAverageOf.OF_5).getAverage();
		case 4: //best ra1
			return session.getStatistics().getBestAverage(RollingAverageOf.OF_12).getAverage();
		case 5: //best time
			return session.getStatistics().getSession().getRollingAverageForWholeSession().getBestTime();
		case 6: //stdev
			return session.getStatistics().getWholeSessionAverage().getStandartDeviation();
		case 7: //solve count
			return session.getStatistics().getSolveCounter().getSolveCount();
		case 8: //comment
			return session.getComment();
		default:
			throw new IllegalArgumentException("illegal column index: " + columnIndex);
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
			sessionsList.removeSession(sessionsList.getNthSession(indices[ch]));
		}
		fireTableDataChanged();
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
		for(ProfileEntity profile : profileDao.getProfileEntitiesExcept(
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
			sessions[ch] = sessionsList.getNthSession(Integer.parseInt(rows[ch]));
		}

		sessionsList.sendSessionToAnotherProfile(sessions, anotherProfileName, profileDao);
	}
}
