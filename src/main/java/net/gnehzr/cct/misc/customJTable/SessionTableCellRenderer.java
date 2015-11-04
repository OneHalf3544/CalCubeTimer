package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.statistics.Session;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class SessionTableCellRenderer implements TableCellRenderer {

	private final SessionsList sessionsList;

	public SessionTableCellRenderer(SessionsList sessionsList) {
		this.sessionsList = sessionsList;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
												   boolean hasFocus, int row, int column) {
		Session session = sessionsList.getNthSession(table.convertRowIndexToModel(row));

		JLabel label = new JLabel();
		label.setOpaque(true);

		boolean isCurrentSession = sessionsList.getCurrentSession() == session;
		label.setBackground(getBackgroundColor(table, isCurrentSession, isSelected));

		label.setText(value.toString());
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		return label;
	}

	private Color getBackgroundColor(JTable table, boolean isCurrentSession, boolean isSelected) {
		Color result = isCurrentSession ? Color.GREEN : table.getBackground();
		return isSelected ? result.darker() : result;
	}
}
