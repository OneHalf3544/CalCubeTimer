package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.SolutionDao;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.main.ScrambleCustomizationChooserComboBox;
import net.gnehzr.cct.misc.customJTable.DraggableJTable.SelectionListener;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.SessionsListTableModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.awt.*;

@Singleton
public class SessionsTable extends DraggableJTable implements SelectionListener {

	private final SolutionDao solutionDao;
	private final SessionsList sessionsList;
	private SessionListener sessionListener;
	private TableModel sessionsListTableModel;

	@Inject
	public SessionsTable(CurrentSessionSolutionsTableModel statsModel, Configuration configuration,
						 ScramblePluginManager scramblePluginManager, CalCubeTimerModel timerModel,
						 SolutionDao solutionDao, SessionsList sessionsList, SessionsListTableModel sessionsListTableModel) {
		super(configuration, false, true);
		//this.statsModel = statsModel;
		this.solutionDao = solutionDao;
		this.sessionsList = sessionsList;
		this.sessionsListTableModel = sessionsListTableModel;
		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//for some reason, the default preferred size is huge
		this.setPreferredScrollableViewportSize(new Dimension(0, 0));
		this.setAutoCreateRowSorter(true);
		SessionRenderer r = new SessionRenderer(statsModel);
		this.setDefaultRenderer(Object.class, r);
		this.setDefaultRenderer(Integer.class, r); //for some reason, Object.class is not capturing the Solve count row
		
		ScrambleCustomizationChooserComboBox chooserComboBox = new ScrambleCustomizationChooserComboBox(false, scramblePluginManager, configuration);
		this.setDefaultEditor(PuzzleType.class, new DefaultCellEditor(chooserComboBox));
		this.setDefaultRenderer(PuzzleType.class, chooserComboBox);
		this.setRowHeight(chooserComboBox.getPreferredSize().height);
		super.setSelectionListener(this);
		configuration.addConfigurationChangeListener((p) -> refreshModel());
	}

	@Override
	public void rowSelected(int row) {
		Session selected = (Session) getValueAt(row, convertColumnIndexToView(0));
		if(sessionsList.getCurrentSession() != selected) {  //we don't want to reload the current session
			fireSessionSelected(selected);
		}
	}

	@Inject
	public void refreshModel() {
		sessionsList.setSessionListener(sessionListener);
		super.setModel(sessionsListTableModel);
		super.sortByColumn(new RowSorter.SortKey(0, SortOrder.DESCENDING));
	}
	
	@Override
	public void tableChanged(TableModelEvent event) {
		int modelRow = event.getFirstRow();
		boolean oneRowSelected = (modelRow == event.getLastRow());
		if(modelRow != -1 && event.getType() == TableModelEvent.UPDATE && oneRowSelected) {
			Session s = sessionsList.getNthSession(modelRow);
			if(s != null && s == sessionsList.getCurrentSession()) {
				//this indicates that the ScrambleCustomization of the currently selected profile has been changed
				//we deal with this by simply reselecting the current session
				fireSessionSelected(s);
			}
		}
		super.tableChanged(event);
	}

	private void fireSessionSelected(Session s) {
		if(sessionListener != null) {
			sessionListener.sessionSelected(s);
		}
	}
}
