package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.Statistics;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import net.gnehzr.cct.statistics.StatisticsTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class SolveTimeRenderer extends JLabel implements TableCellRenderer {
	// Will highlight times from current average and from best rolling average
	private StatisticsTableModel statsModel;
	private final Configuration configuration;

	public SolveTimeRenderer(StatisticsTableModel statsModel, Configuration configuration) {
		this.statsModel = statsModel;
		this.configuration = configuration;
		setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		setEnabled(table.isEnabled());
		setFont(table.getFont());
		if(value == null)
			setText(new SolveTime(configuration).toString());
		else
			setText(value.toString());
		setHorizontalAlignment(SwingConstants.RIGHT);
		setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		Color foreground = null;
		Color background = null;

		if(value instanceof SolveTime) {
			Statistics times = statsModel.getCurrentStatistics();
			boolean memberOfBestRA;
			boolean memberOfCurrentAverage = false;
			SolveTime st = (SolveTime) value;
			int whichRA;
			if((whichRA = st.getWhichRA()) != -1) { //this indicates we're dealing with an average, not a solve time
				int raSize = times.getRASize(whichRA);
				int indexOfBestRA = times.getIndexOfBestRA(whichRA);
				memberOfBestRA = indexOfBestRA != -1 && (indexOfBestRA + raSize == row + 1);
			} else {
				SolveTime[] bestAndWorst = times.getBestAndWorstTimes(AverageType.SESSION, 0);
				if(bestAndWorst[0] == value) {
					foreground = configuration.getColor(VariableKey.BEST_TIME, false);
				} else if(bestAndWorst[1] == value) {
					foreground = configuration.getColor(VariableKey.WORST_TIME, false);
				}
				memberOfBestRA = times.containsTime(row, AverageType.RA, 0);
				memberOfCurrentAverage = times.containsTime(row, AverageType.CURRENT, 0);
			}
			
			if(memberOfCurrentAverage) {
				boolean firstOfCurrentAverage = row == times.getAttemptCount() - times.getRASize(0);
				boolean lastOfCurrentAverage = row == times.getAttemptCount() - 1;
				
				Border b;
				Color c = configuration.getColor(VariableKey.CURRENT_AVERAGE, false);
				if(firstOfCurrentAverage)
					b = BorderFactory.createMatteBorder(2, 2, 0, 2, c);
				else if(lastOfCurrentAverage)
					b = BorderFactory.createMatteBorder(0, 2, 2, 2, c);
				else
					b = BorderFactory.createMatteBorder(0, 2, 0, 2, c);
				setBorder(BorderFactory.createCompoundBorder(b, getBorder()));
			}
			if(memberOfBestRA)
				background = configuration.getColor(VariableKey.BEST_RA, false);
		}
		if(isSelected) {
			if(background == null)
				background = Color.GRAY;
			else
				background = background.darker();
		} else if(background == null)
			background = table.getBackground();

		setForeground(foreground);
		setBackground(background);

		return this;
	}
}
