package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Date;

public class SessionTableCellRenderer implements TableCellRenderer {

	private final SessionsList sessionsList;

	public SessionTableCellRenderer(SessionsList sessionsList) {
		this.sessionsList = sessionsList;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
												   boolean hasFocus, int row, int column) {
		JLabel label = new JLabel();
		if (value == null) {
			label.setText(new Date().toString());
		}
		else {
			label.setText(value.toString());
			label.setHorizontalAlignment(SwingConstants.RIGHT);
			label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		}

		//emphasize the current session
		if (sessionsList.getCurrentSession() == sessionsList.getNthSession(row)) {
			label.setBackground(Color.GREEN);
		}
		else {
			label.setBackground(Color.WHITE);
		}

		return label;
	}
}
