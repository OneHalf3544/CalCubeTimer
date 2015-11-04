package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.SolveTime;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * <p>
 * <p>
 * Created: 03.11.2015 22:35
 * <p>
 *
 * @author OneHalf
 */
public abstract class TimeRenderer<T> extends JLabel implements TableCellRenderer {

	protected final Configuration configuration;
	protected SessionsList sessionsList;

	public TimeRenderer(Configuration configuration, SessionsList sessionsList) {
		this.configuration = configuration;
		this.sessionsList = sessionsList;
		setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
												   boolean hasFocus, int row, int column) {
		setEnabled(table.isEnabled());
		setFont(table.getFont());
		if (value == null) {
			setText(SolveTime.NULL_TIME.toString(configuration));
		} else {
			//noinspection unchecked
			setText(valueToString((T)value));
		}

		setHorizontalAlignment(SwingConstants.RIGHT);
		setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		SessionSolutionsStatistics sessionStatistics = sessionsList.getCurrentSession().getStatistics();

		//noinspection unchecked
		processTime((T) value, row, sessionStatistics);
		@SuppressWarnings("unchecked")
		boolean memberOfBestRA = isMemberOfBestRA((T) value, sessionStatistics);

		setBackground(getBackgroundColor(table, isSelected, memberOfBestRA));

		return this;
	}

	protected abstract boolean isMemberOfBestRA(T value, SessionSolutionsStatistics sessionStatistics);

	protected abstract void processTime(T value, int row, SessionSolutionsStatistics sessionStatistics);

	private Color getBackgroundColor(JTable table, boolean isSelected, boolean memberOfBestRA) {
		Color background = memberOfBestRA
				? configuration.getColor(VariableKey.BEST_RA, false)
				: table.getBackground();

		if (isSelected) {
			background = background.darker();
		}
		return background;
	}

	protected abstract String valueToString(T value);
}
