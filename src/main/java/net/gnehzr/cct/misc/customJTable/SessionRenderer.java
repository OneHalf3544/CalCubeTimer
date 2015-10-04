package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Date;

public class SessionRenderer extends JLabel implements TableCellRenderer {

	private final CurrentSessionSolutionsTableModel statsModel;

	public SessionRenderer(CurrentSessionSolutionsTableModel statsModel) {
		this.statsModel = statsModel;
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if(value instanceof Session)
			setText(((Session)value).toDateString());
		else if(value == null)
			setText(new Date().toString());
		else {
			setText(value.toString());
			setHorizontalAlignment(SwingConstants.RIGHT);
			setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		}
		setBackground(Color.WHITE);
		if(row < table.getRowCount() && statsModel.getCurrentSession() == ((SessionsTable) table).getValueAt(row, table.convertColumnIndexToView(0))) //emphasize the current session
			setBackground(Color.GREEN);
		return this;
	}
}
