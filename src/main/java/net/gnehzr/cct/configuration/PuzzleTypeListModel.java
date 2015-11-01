package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.PuzzleType;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class PuzzleTypeListModel extends DraggableJTableModel {

	private List<PuzzleType> puzzleTypes;

	private String[] columnNames = new String[]{
			StringAccessor.getString("ScrambleCustomizationListModel.puzzletype"),
			StringAccessor.getString("ScrambleCustomizationListModel.length"),
			StringAccessor.getString("ScrambleCustomizationListModel.generatorgroup"),
			"RA 0",
			"RA 1"};

	public void setContents(List<PuzzleType> contents) {
		this.puzzleTypes = contents;
		fireTableDataChanged();
	}

	public List<PuzzleType> getContents() {
		return puzzleTypes;
	}

	@Override
	public void deleteRows(int[] indices) {
		removeRows(indices);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return PuzzleType.class;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public int getRowCount() {
		return puzzleTypes == null ? 0 : puzzleTypes.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return puzzleTypes.get(rowIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex >= 1;
	}

	@Override
	public boolean isRowDeletable(int rowIndex) {
		return false;
	}

	@Override
	public void removeRows(int[] indices) {}

	@Override
	public void insertValueAt(Object value, int rowIndex) {
		puzzleTypes.add(rowIndex, (PuzzleType) value);
		fireTableRowsInserted(rowIndex, rowIndex);
	}
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		// row was updated in PuzzleSettingsTableEditor.stopCellEditing()
		fireTableRowsUpdated(rowIndex, rowIndex);
	}
	@Override
	public void showPopup(MouseEvent e, DraggableJTable source, Component prevFocusOwner) {}
}
