package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.customJTable.DraggableJTable.SelectionListener;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.SessionsListTableModel;

import javax.swing.*;
import java.awt.*;

@Singleton
public class SessionsListTable extends DraggableJTable implements SelectionListener {

	private final SessionsList sessionsList;
	private final SessionsListTableModel sessionsListTableModel;

	@Inject
	public SessionsListTable(Configuration configuration, SessionsList sessionsList,
							 SessionsListTableModel sessionsListTableModel) {
		super(configuration, false, true);
		this.sessionsList = sessionsList;
		this.sessionsListTableModel = sessionsListTableModel;
		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//for some reason, the default preferred size is huge
		this.setPreferredScrollableViewportSize(new Dimension(0, 0));
		this.setAutoCreateRowSorter(true);
		SessionTableCellRenderer sessionTableCellRenderer = new SessionTableCellRenderer(sessionsList);
		this.setDefaultRenderer(Object.class, sessionTableCellRenderer);
		this.setDefaultRenderer(Integer.class, sessionTableCellRenderer); //for some reason, Object.class is not capturing the Solve count row
		
		super.setSelectionListener(this);
		configuration.addConfigurationChangeListener((p) -> refreshModel());

		setName("sessionsTable");
		//TODO - this wastes space, probably not easy to fix...
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	@Override
	public void rowSelected(int row) {
		Session selected = sessionsList.getNthSession(convertRowIndexToModel(row));
		if (sessionsList.getCurrentSession() != selected) {
			sessionsList.setCurrentSession(selected);
		}
	}

	@Inject
	public void refreshModel() {
		super.setModel(sessionsListTableModel);
		super.sortByColumn(new RowSorter.SortKey(0, SortOrder.DESCENDING));
	}
}
