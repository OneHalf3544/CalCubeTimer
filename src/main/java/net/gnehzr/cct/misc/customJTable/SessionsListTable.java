package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.SessionsListTableModel;

import javax.swing.*;
import java.awt.*;

@Singleton
public class SessionsListTable extends DraggableJTable {

	private final SessionsListTableModel sessionsListTableModel;

	@Inject
	public SessionsListTable(Configuration configuration, SessionsList sessionsList,
							 SessionsListTableModel sessionsListTableModel) {
		super(configuration, false, true);
		setName("sessionsTable");
		this.sessionsListTableModel = sessionsListTableModel;
		//for some reason, the default preferred size is huge
		this.setPreferredScrollableViewportSize(new Dimension(0, 0));
		this.setAutoCreateRowSorter(true);
		this.setDefaultRenderer(Object.class, new SessionTableCellRenderer(sessionsList));
		this.setDefaultRenderer(Number.class, null); //for some reason, Object.class is not capturing the Solve count row
		super.setSelectionListener(row -> {
			Session selected = sessionsList.getNthSession(convertRowIndexToModel(row));
			if (sessionsList.getCurrentSession() != selected) {
				sessionsList.setCurrentSession(selected);
			}
		});
		configuration.addConfigurationChangeListener((p) -> refreshModel());

		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//TODO - this wastes space, probably not easy to fix...
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	@Inject
	public void refreshModel() {
		super.setModel(sessionsListTableModel);
		super.sortByColumn(new RowSorter.SortKey(0, SortOrder.DESCENDING));
	}
}
