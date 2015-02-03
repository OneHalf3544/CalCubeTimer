package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class StatisticsTableModel extends DraggableJTableModel implements ActionListener {

	private static final Logger LOG = Logger.getLogger(StatisticsTableModel.class);

	Statistics stats;

	private Session session;

	private final Configuration configuration;

	private UndoRedoListener undoRedoListener;

	private String[] columnNames = new String[] {
			"StatisticsTableModel.times",
			"StatisticsTableModel.ra0",
			"StatisticsTableModel.ra1",
			"StatisticsTableModel.comment",
			"StatisticsTableModel.tags",
			"StatisticsTableModel.puzzleName",
	};
	private Class<?>[] columnClasses = new Class<?>[] {
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			String.class,
			String.class,
			String.class,
	};

	private JMenuItem edit, discard;
	private DraggableJTable timesTable;
	private Component prevFocusOwner;
	private Map<SolveType, JMenuItem> typeButtons;
	private List<StatisticsUpdateListener> statsListeners = new ArrayList<>();

	@Inject
	public StatisticsTableModel(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setSession(@NotNull Session session) {
		this.session = Objects.requireNonNull(session);
		if(stats != null) {
			stats.setUndoRedoListener(null);
			stats.setTableListener(null);
			stats.setStatisticsUpdateListeners(null);
		}
		stats = session.getStatistics();
		stats.setTableListener(this);
		stats.setUndoRedoListener(undoRedoListener);
		stats.setStatisticsUpdateListeners(statsListeners);
		stats.notifyListeners(false);
	}

	//@NotNull
	public Session getCurrentSession() {
		return session;
	}

	public Statistics getCurrentStatistics() {
		return stats;
	}

	public void setUndoRedoListener(@NotNull UndoRedoListener l) {
		this.undoRedoListener = Objects.requireNonNull(l);
	}

	public void addStatisticsUpdateListener(StatisticsUpdateListener l) {
		//This nastyness is to ensure that PuzzleStatistics have had a chance to see the change (see notifyListeners() in Statistics)
		//before the dynamicstrings
		if(l instanceof PuzzleStatistics)
			statsListeners.add(0, l);
		else
			statsListeners.add(l);
	}

	public void removeStatisticsUpdateListener(StatisticsUpdateListener l) {
		statsListeners.remove(l);
	}

	//this is needed to update the i18n text
	public void fireStringUpdates() {
		LOG.debug("StatisticsTableModel.fireStringUpdates()");
		statsListeners.forEach(StatisticsUpdateListener::update);
		undoRedoListener.refresh();
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
	public Class<?> getColumnClass(int columnIndex) {
		return columnClasses[columnIndex];
	}

	public int getSize() {
		return getRowCount();
	}

	@Override
	public int getRowCount() {
		return stats == null ? 0 : stats.getAttemptCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
		case 0: //get the solvetime for this index
			return stats.get(rowIndex).getTime();
		case 1: //falls through
		case 2: //get the RA for this index in this column
			return stats.getRA(rowIndex, columnIndex - 1).getTime();
		case 3:
			return stats.get(rowIndex).getTime().getComment();
		case 4: //tags
			return Joiner.on(", ").join(stats.get(rowIndex).getTime().getTypes());
		case 5: // scramble variation
			return stats.get(rowIndex).getScramble().getVariation().getName();
		default:
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0 || columnIndex == 3;
	}

	@Override
	public boolean isRowDeletable(int rowIndex) {
		return true;
	}

	@Override
	public void insertValueAt(Object value, int rowIndex) {
		stats.add(rowIndex, (Solution) value);
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(columnIndex == 0 && value instanceof Solution) {
			stats.set(rowIndex, (Solution) value);
		}
		else if(columnIndex == 3 && value instanceof String) {
			stats.get(rowIndex).getTime().setComment((String) value);
		}
	}

	@Override
	public void deleteRows(int[] indices) {
		stats.remove(indices);
	}

	@Override
	public void removeRows(int[] indices) {
		deleteRows(indices);
	}

	@Override
	public String getToolTip(int rowIndex, int columnIndex) {
		String t = stats.get(rowIndex).getTime().getComment();
		return t.isEmpty() ? null : t;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == edit) {
			timesTable.editCellAt(timesTable.getSelectedRow(), 0);
		} else {
			if(source == discard) {
				timesTable.deleteSelectedRows(false);
			} else {
			 	//one of the jradio buttons
				List<SolveType> types = typeButtons.keySet().stream()
						.filter(key -> typeButtons.get(key).isSelected())
						.collect(Collectors.toList());
				stats.setSolveTypes(timesTable.getSelectedRow(), types);
			}
		}
		if(prevFocusOwner != null)
			prevFocusOwner.requestFocusInWindow();
	}

	@Override
	public void showPopup(MouseEvent e, DraggableJTable timesTable, Component prevFocusOwner) {
		this.timesTable = timesTable;
		this.prevFocusOwner = prevFocusOwner;
		JPopupMenu jpopup = new JPopupMenu();
		int[] selectedSolves = timesTable.getSelectedRows();
		if(selectedSolves.length == 0) {
			return;
		}
		else if(selectedSolves.length == 1) {
			Solution selectedSolve = stats.get(timesTable.getSelectedRow());
			JMenuItem rawTime = new JMenuItem(StringAccessor.getString("StatisticsTableModel.rawtime")
					+ Utils.formatTime(selectedSolve.getTime(), configuration.getBoolean(VariableKey.CLOCK_FORMAT, false) ));
			rawTime.setEnabled(false);
			jpopup.add(rawTime);

			addSplitsPopup(jpopup, selectedSolve);

			edit = new JMenuItem(StringAccessor.getString("StatisticsTableModel.edittime"));
			edit.addActionListener(this);
			jpopup.add(edit);

			jpopup.addSeparator();
			
			typeButtons = new HashMap<>();
			ButtonGroup independent = new ButtonGroup();
			JMenuItem attr = new JRadioButtonMenuItem("<html><b>" + StringAccessor.getString("StatisticsTableModel.nopenalty") + "</b></html>", !selectedSolve.getTime().isPenalty());
			attr.setEnabled(!selectedSolve.getTime().isTrueWorstTime());
			attr.addActionListener(this);
			jpopup.add(attr);
			independent.add(attr);
			List<String> solveTagsArray = configuration.getStringArray(VariableKey.SOLVE_TAGS, false);
			Collection<SolveType> types = SolveType.getSolveTypes(solveTagsArray);
			for(SolveType type : types) {
				if(type.isIndependent()) {
					attr = new JRadioButtonMenuItem("<html><b>" + type.toString() + "</b></html>", selectedSolve.getTime().isType(type));
					attr.setEnabled(!selectedSolve.getTime().isTrueWorstTime());
					independent.add(attr);
				} else {
					attr = new JCheckBoxMenuItem(type.toString(), selectedSolve.getTime().isType(type));
				}
				attr.addActionListener(this);
				jpopup.add(attr);
				typeButtons.put(type, attr);
			}
			
			jpopup.addSeparator();
		}

		discard = new JMenuItem(StringAccessor.getString("StatisticsTableModel.discard"));
		discard.addActionListener(this);
		jpopup.add(discard);
		timesTable.requestFocusInWindow();
		jpopup.show(e.getComponent(), e.getX(), e.getY());
	}

	private void addSplitsPopup(JPopupMenu jpopup, Solution selectedSolve) {
		List<SolveTime> splits = selectedSolve.getSplits();
		for (int i = 0; i < splits.size(); i++) {
			SolveTime next = splits.get(i);

			JMenuItem rawTime = new JMenuItem(StringAccessor.getString("StatisticsTableModel.split") + i + ": " + next);
			rawTime.setEnabled(false);
			jpopup.add(rawTime);
		}
	}
}
