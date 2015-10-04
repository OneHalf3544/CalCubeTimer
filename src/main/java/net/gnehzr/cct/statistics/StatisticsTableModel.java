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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class StatisticsTableModel extends DraggableJTableModel {

	private static final Logger LOG = LogManager.getLogger(StatisticsTableModel.class);

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

	private DraggableJTable timesTable;
	private Component prevFocusOwner;
	private Map<SolveType, JMenuItem> typeButtons;
	private List<StatisticsUpdateListener> statsListeners = new ArrayList<>();

	private Session session;

	@Inject
	public StatisticsTableModel(Configuration configuration) {
		this.configuration = configuration;
		session = new Session(LocalDateTime.now(), this.configuration, this);
	}

	public void setSession(@NotNull Session session) {
		this.session = Objects.requireNonNull(session);
		Statistics stats = Objects.requireNonNull(session.getStatistics());
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

	@NotNull
	public Session getCurrentSession() {
		return session;
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
		return session.getStatistics().getAttemptCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
		case 0: //get the solvetime for this index
			return session.getStatistics().get(rowIndex).getTime();
		case 1: //falls through
		case 2: //get the RA for this index in this column
			return session.getStatistics().getRA(rowIndex, columnIndex - 1);
		case 3:
			return session.getStatistics().get(rowIndex).getComment();
		case 4: //tags
			return Joiner.on(", ").join(session.getStatistics().get(rowIndex).getTime().getTypes());
		case 5: // scramble variation
			return session.getStatistics().get(rowIndex).getScrambleString().getVariation().getName();
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
		session.getStatistics().add(rowIndex, (Solution) value);
		fireTableRowsInserted(rowIndex, rowIndex);
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(columnIndex == 0 && value instanceof Solution) {
			session.getStatistics().set(rowIndex, (Solution) value);
		}
		else if(columnIndex == 3 && value instanceof String) {
			session.getStatistics().get(rowIndex).setComment((String) value);
		}
	}

	@Override
	public void deleteRows(int[] indices) {
		session.getStatistics().remove(indices);
	}

	@Override
	public void removeRows(int[] indices) {
		deleteRows(indices);
	}

	@Override
	public String getToolTip(int rowIndex, int columnIndex) {
		String t = session.getStatistics().get(rowIndex).getComment();
		return t.isEmpty() ? null : t;
	}

	private void editTimeMenuItemClicked(ActionEvent e) {
		timesTable.editCellAt(timesTable.getSelectedRow(), 0);
		if(prevFocusOwner != null) {
			prevFocusOwner.requestFocusInWindow();
		}
	}

	private void discardSolution(ActionEvent e) {
		timesTable.deleteSelectedRows(false);
		if(prevFocusOwner != null) {
			prevFocusOwner.requestFocusInWindow();
		}
	}

	private void setTagRadioButton(ActionEvent e) {
		//one of the jradio buttons
		List<SolveType> types = typeButtons.keySet().stream()
                .filter(key -> typeButtons.get(key).isSelected())
				.collect(Collectors.toList());
		session.getStatistics().setSolveTypes(timesTable.getSelectedRow(), types);
		if(prevFocusOwner != null) {
			prevFocusOwner.requestFocusInWindow();
		}
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
			Solution selectedSolve = session.getStatistics().get(timesTable.getSelectedRow());
			JMenuItem rawTime = new JMenuItem(StringAccessor.getString("StatisticsTableModel.rawtime")
					+ Utils.formatTime(selectedSolve.getTime(), configuration.getBoolean(VariableKey.CLOCK_FORMAT) ));
			rawTime.setEnabled(false);
			jpopup.add(rawTime);

			addSplitsPopup(jpopup, selectedSolve);

			JMenuItem edit = new JMenuItem(StringAccessor.getString("StatisticsTableModel.edittime"));
			edit.addActionListener(this::editTimeMenuItemClicked);
			jpopup.add(edit);

			jpopup.addSeparator();
			
			typeButtons = new HashMap<>();
			ButtonGroup independent = new ButtonGroup();
			JMenuItem attr = new JRadioButtonMenuItem("<html><b>" + StringAccessor.getString("StatisticsTableModel.nopenalty") + "</b></html>", !selectedSolve.getTime().isPenalty());
			attr.setEnabled(!selectedSolve.getTime().isTrueWorstTime());
			attr.addActionListener(this::setTagRadioButton);
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
				attr.addActionListener(this::setTagRadioButton);
				jpopup.add(attr);
				typeButtons.put(type, attr);
			}
			
			jpopup.addSeparator();
		}

		JMenuItem discard = new JMenuItem(StringAccessor.getString("StatisticsTableModel.discard"));
		discard.addActionListener(this::discardSolution);
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
