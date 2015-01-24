package net.gnehzr.cct.misc.customJTable;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
* <p>
* <p>
* Created: 23.01.2015 1:26
* <p>
*
* @author OneHalf
*/
class JTableModelWrapper extends DraggableJTableModel {

    private DraggableJTable draggableJTable;
    private DraggableJTableModel wrapped;

	public JTableModelWrapper(DraggableJTable draggableJTable, DraggableJTableModel wrapped) {
        this.draggableJTable = draggableJTable;
        this.wrapped = wrapped;
        wrapped.addTableModelListener(e -> {
			fireTableChanged(e);
			int[] rows = draggableJTable.getSelectedRows();
			if(rows.length > 0)
			draggableJTable.setRowSelectionInterval(rows[0], rows[0]);
		});
    }
    @Override
    public void deleteRows(int[] indices) {
        wrapped.deleteRows(indices);
    }

	@Override
    public int getColumnCount() {
        return wrapped.getColumnCount();
    }

	@Override
    public int getRowCount() {
        if(draggableJTable.addText == null)
            return wrapped.getRowCount();
        return wrapped.getRowCount() + 1;
    }

	@Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(rowIndex == wrapped.getRowCount()) {
            if(columnIndex == 0)
                return draggableJTable.addText;
            return "";
        }

        return wrapped.getValueAt(rowIndex, columnIndex);
    }

	@Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if(rowIndex == wrapped.getRowCount())
            return columnIndex == 0;

        return wrapped.isCellEditable(rowIndex, columnIndex);
    }

	@Override
    public boolean isRowDeletable(int rowIndex) {
        return rowIndex != wrapped.getRowCount() && wrapped.isRowDeletable(rowIndex);

    }

	@Override
    public void removeRows(int[] indices) {
        wrapped.removeRows(indices);
    }

	@Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        wrapped.setValueAt(value, rowIndex, columnIndex);
    }

	@Override
    public void showPopup(MouseEvent e, DraggableJTable source, Component prevFocusOwner) {
        if(draggableJTable.rowAtPoint(e.getPoint()) != wrapped.getRowCount())
            wrapped.showPopup(e, source, prevFocusOwner);
    }

	@Override
    public Class<?> getColumnClass(int columnIndex) {
        return wrapped.getColumnClass(columnIndex);
    }

	@Override
    public String getColumnName(int column) {
        return wrapped.getColumnName(column);
    }

	@Override
    public void insertValueAt(Object value, int rowIndex) {
        wrapped.insertValueAt(value, rowIndex);
    }

	@Override
    public String getToolTip(int rowIndex, int columnIndex) {
        if(rowIndex == wrapped.getRowCount()) {
			return null;
		}
        return wrapped.getToolTip(rowIndex, columnIndex);
    }
}
