package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.main.ScrambleCustomizationChooserComboBox;
import net.gnehzr.cct.misc.customJTable.DraggableJTable.SelectionListener;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsListTableModel;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;

public class SessionsTable extends DraggableJTable implements SelectionListener {
	private final CurrentSessionSolutionsTableModel statsModel;
	private final ProfileDao profileDao;

	public SessionsTable(CurrentSessionSolutionsTableModel statsModel, Configuration configuration, ScramblePluginManager scramblePluginManager, ProfileDao profileDao) {
		super(configuration, false, true);
		this.statsModel = statsModel;
		this.profileDao = profileDao;
		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//for some reason, the default preferred size is huge
		this.setPreferredScrollableViewportSize(new Dimension(0, 0));
		this.setAutoCreateRowSorter(true);
		SessionRenderer r = new SessionRenderer(statsModel);
		this.setDefaultRenderer(Object.class, r);
		this.setDefaultRenderer(Integer.class, r); //for some reason, Object.class is not capturing the Solve count row
		
		ScrambleCustomizationChooserComboBox chooserComboBox = new ScrambleCustomizationChooserComboBox(false, scramblePluginManager, configuration);
		this.setDefaultEditor(ScrambleCustomization.class, new DefaultCellEditor(chooserComboBox));
		this.setDefaultRenderer(ScrambleCustomization.class, chooserComboBox);
		this.setRowHeight(chooserComboBox.getPreferredSize().height);
		super.setSelectionListener(this);
		configuration.addConfigurationChangeListener((p) -> refreshModel());
		super.sortByColumn(new RowSorter.SortKey(0, SortOrder.DESCENDING));
		refreshModel();
	}
	@Override
	public void rowSelected(int row) {
		Session selected = (Session) getValueAt(row, convertColumnIndexToView(0));
		if(statsModel.getCurrentSession() != selected) {  //we don't want to reload the current session
			fireSessionSelected(selected);
		}
	}
	
	private SessionsListTableModel pd;

	public void refreshModel() {
		if(pd != null)
			pd.setSessionListener(null);
		pd = profileDao.getSelectedProfile().getSessionsDatabase();
		pd.setSessionListener(l);
		super.setModel(pd);
	}
	
	@Override
	public void tableChanged(TableModelEvent event) {
		int modelRow = event.getFirstRow();
		boolean oneRowSelected = (modelRow == event.getLastRow());
		if(modelRow != -1 && event.getType() == TableModelEvent.UPDATE && oneRowSelected) {
			Session s = pd.getNthSession(modelRow);
			if(s != null && s == statsModel.getCurrentSession()) {
				//this indicates that the ScrambleCustomization of the currently selected profile has been changed
				//we deal with this by simply reselecting the current session
				fireSessionSelected(s);
			}
		}
		super.tableChanged(event);
	}
	
	private SessionListener l;
	public void setSessionListener(SessionListener sl) {
		l = sl;
	}
	private void fireSessionSelected(Session s) {
		if(l != null) {
			l.sessionSelected(s);
		}
	}
}
