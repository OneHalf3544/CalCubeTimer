package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.misc.customJTable.SessionListener;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.transform.TransformerConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProfileDatabase extends DraggableJTableModel implements ActionListener {

	private static final Logger LOG = Logger.getLogger(ProfileDatabase.class);

	private Map<String, PuzzleStatistics> database = new HashMap<>();

	private final Configuration configuration;
	private final ProfileDao profileDao;
	private final StatisticsTableModel statsModel;
	private final ScramblePluginManager scramblePluginManager;

	public ProfileDatabase(Configuration configuration, ProfileDao profileDao,
						   StatisticsTableModel statsModel, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.profileDao = profileDao;
		this.statsModel = statsModel;
		this.scramblePluginManager = scramblePluginManager;
	}

	public Collection<PuzzleStatistics> getPuzzlesStatistics() {
		return new ArrayList<>(database.values());
	}
	public Collection<String> getCustomizations() {
		return new ArrayList<>(database.keySet());
	}

	public PuzzleStatistics getPuzzleStatistics(String customization) {
		PuzzleStatistics t = database.get(customization);
		if(t == null) {
			t = new PuzzleStatistics(customization, this, configuration, statsModel);
			database.put(customization, t);
		}
		return t;
	}
	
	public void removeEmptySessions() {
		for(PuzzleStatistics ps : getPuzzlesStatistics()) {
			for(Session s : ps.toSessionIterable()) {
				if(s.getStatistics().getAttemptCount() == 0)
					ps.removeSession(s);
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
				if(s == findMe)
					return n;
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
		for(PuzzleStatistics ps : getPuzzlesStatistics())
			for(Session s : ps.toSessionIterable())
				sessionCache.add(s);
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

	private Class<?>[] columnClasses = new Class<?>[] { Session.class, ScrambleCustomization.class, SolveTime.class, SolveTime.class, SolveTime.class, SolveTime.class, SolveTime.class, Integer.class, String.class};
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
			return new SolveTime(s.getStatistics().getBestAverage(0), null, configuration);
		case 4: //best ra1
			return new SolveTime(s.getStatistics().getBestAverage(1), null, configuration);
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
				s.setCustomization(sc.toString(), profileDao.getSelectedProfile());
				updateSessionCache();
				rowIndex = indexOf(s); //changing the customization will change the index in the model
				fireTableRowsUpdated(rowIndex, rowIndex);
			}
		} else if(columnIndex == 8 && value instanceof String)
			getNthSession(rowIndex).setComment((String) value);
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 1 || columnIndex == 8; //allow modification of the session customization or comment
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
		for(Profile p : profileDao.getProfiles(configuration)) {
			if(p == profileDao.getSelectedProfile())
				continue;
			JMenuItem profile = new JMenuItem(p.getName());
			String rows = "";
			for(int r : source.getSelectedRows())
				rows += "," + source.convertRowIndexToModel(r);
			rows = rows.substring(1);
			profile.setActionCommand(SEND_TO_PROFILE + rows);
			profile.addActionListener(this);
			sendTo.add(profile);
		}
		jpopup.add(sendTo);
		
		source.requestFocusInWindow();
		jpopup.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().startsWith(SEND_TO_PROFILE)) {
			Profile to = profileDao.getProfileByName(((JMenuItem) e.getSource()).getText());
			profileDao.loadDatabase(to, scramblePluginManager);
			
			String[] rows = e.getActionCommand().substring(SEND_TO_PROFILE.length()).split(",");
			Session[] seshs = new Session[rows.length];
			for(int ch = 0; ch < rows.length; ch++) {
				int row = Integer.parseInt(rows[ch]);
				seshs[ch] = getNthSession(row);
			}
			for(Session s : seshs) {
				String custom = s.getCustomization().toString();
				s.delete();
				to.getPuzzleDatabase().getPuzzleStatistics(custom).addSession(s, profileDao.getSelectedProfile());
			}
			fireSessionsDeleted();
			try {
				profileDao.saveDatabase(to);
			} catch (TransformerConfigurationException | SAXException | IOException e1) {
				LOG.info("unexpected exception", e1);
			}
		}
	}

}
