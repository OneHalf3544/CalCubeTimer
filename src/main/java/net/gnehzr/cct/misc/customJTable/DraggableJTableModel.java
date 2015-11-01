package net.gnehzr.cct.misc.customJTable;

import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class DraggableJTableModel extends AbstractTableModel {

	@Override
	public abstract int getRowCount();

	@Override
	public abstract int getColumnCount();

	@Override
	public abstract Object getValueAt(int rowIndex, int columnIndex);

	@Override
	public abstract boolean isCellEditable(int rowIndex, int columnIndex);

	public abstract boolean isRowDeletable(int rowIndex);

	//this should deal with the case where rowIndex == getRowCount by appending value
	@Override
	public abstract void setValueAt(Object value, int rowIndex, int columnIndex);

	public abstract void insertValueAt(Object value, int rowIndex);

	@Override
	public abstract Class<?> getColumnClass(int columnIndex);

	/* This is to just remove the indices from the list
	 * NOTE: Must be sorted!
	 */
	public abstract void removeRows(int[] indices);
	/* This is to actually delete the indices
	 * NOTE: Must be sorted!
	 */
	public abstract void deleteRows(int[] indices);

	public void showPopup(MouseEvent e, DraggableJTable source, Component prevFocusOwner) {}

	//return null to have no tooltip
	@Nullable
	public String getToolTip(int rowIndex, int columnIndex) {
		return null;
	}
}
