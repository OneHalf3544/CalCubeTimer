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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Singleton
public class CurrentSessionSolutionsTableModel extends DraggableJTableModel {

	private String[] columnNames = new String[] {
			"StatisticsTableModel.times",
			"StatisticsTableModel.ra0",
			"StatisticsTableModel.ra1",
			"StatisticsTableModel.comment",
			"StatisticsTableModel.tags",
			"StatisticsTableModel.puzzleName",
			"StatisticsTableModel.scramble",
	};
	private Class<?>[] columnClasses = new Class<?>[] {
			SolveTime.class,
			SolveTime.class,
			SolveTime.class,
			String.class,
			String.class,
			String.class,
			String.class,
	};

	private DraggableJTable timesTable;
	private Component prevFocusOwner;
	private Map<SolveType, JMenuItem> typeButtons;
	private final Configuration configuration;
	private final SessionsList sessionsList;

	@Inject
	public CurrentSessionSolutionsTableModel(Configuration configuration, SessionsList sessionsList) {
		this.configuration = configuration;
		this.sessionsList = sessionsList;
		sessionsList.addStatisticsUpdateListener(() -> {
			if (sessionsList.getCurrentSession().getAttemptsCount() > 0) {
				fireTableDataChanged();
			}
		});
	}

	@NotNull
	@Deprecated
	public Session getCurrentSession() {
		return sessionsList.getCurrentSession();
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
		return sessionsList.getCurrentSession().getAttemptsCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Session currentSession = sessionsList.getCurrentSession();
		switch(columnIndex) {
		case 0: //get the solvetime for this index
			return currentSession.getSolution(rowIndex).getTime();
		case 1:
			return currentSession.getStatistics().getRA(rowIndex, RollingAverageOf.OF_5).getAverage();
		case 2:
			return currentSession.getStatistics().getRA(rowIndex, RollingAverageOf.OF_12).getAverage();
		case 3:
			return currentSession.getSolution(rowIndex).getComment();
		case 4: //tags
			return Joiner.on(", ").join(currentSession.getSolution(rowIndex).getTime().getTypes());
		case 5: // puzzle type
			return currentSession.getSolution(rowIndex).getScrambleString().getPuzzleType().getVariationName();
		case 6: // scramble
			return currentSession.getSolution(rowIndex).getScrambleString().getScramble();
		default:
			throw new IllegalArgumentException("unsupported column index");
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(columnIndex == 0 && value instanceof Solution) {
			sessionsList.addSolutionToCurrentSession((Solution) value);
		}
		else if(columnIndex == 3 && value instanceof String) {
			sessionsList.setComment((String) value, rowIndex);
		}
	}

	@Override
	public void deleteRows(int[] indices) {
		Session currentSession = sessionsList.getCurrentSession();
		sessionsList.deleteSolutions(IntStream.of(indices)
				.mapToObj(currentSession::getSolution)
				.toArray(Solution[]::new));
	}

	@Override
	public void removeRows(int[] indices) {
		deleteRows(indices);
	}

	@Override
	public String getToolTip(int rowIndex, int columnIndex) {
		String t = sessionsList.getCurrentSession().getSolution(rowIndex).getComment();
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
		sessionsList.getCurrentSession().getStatistics().setSolveTypes(timesTable.getSelectedRow(), types, sessionsList::fireStringUpdates);
		if (prevFocusOwner != null) {
			prevFocusOwner.requestFocusInWindow();
		}
	}

	@Override
	public void showPopup(MouseEvent e, DraggableJTable timesTable, Component prevFocusOwner) {
		this.timesTable = timesTable;
		this.prevFocusOwner = prevFocusOwner;
		JPopupMenu jpopup = new JPopupMenu();
		int[] selectedSolves = timesTable.getSelectedRows();
		if (selectedSolves.length == 0) {
			return;
		}
		else if(selectedSolves.length == 1) {
			Solution selectedSolve = sessionsList.getCurrentSession().getSolution(timesTable.getSelectedRow());
			JMenuItem rawTime = new JMenuItem(StringAccessor.getString("StatisticsTableModel.rawtime")
					+ Utils.formatTime(selectedSolve.getTime(), configuration.useClockFormat() ));
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

		JMenuItem discard = new JMenuItem(	StringAccessor.getString("StatisticsTableModel.discard"));
		discard.addActionListener(this::discardSolution);
		jpopup.add(discard);
		timesTable.requestFocusInWindow();
		jpopup.show(e.getComponent(), e.getX(), e.getY());
	}

	void addSplitsPopup(JPopupMenu jpopup, Solution selectedSolve) {
		List<SolveTime> splits = selectedSolve.getSplits();
		for (int i = 0; i < splits.size(); i++) {
			SolveTime next = splits.get(i);

			JMenuItem rawTime = new JMenuItem(StringAccessor.getString("StatisticsTableModel.split") + i + ": " + next);
			rawTime.setEnabled(false);
			jpopup.add(rawTime);
		}
	}
}
