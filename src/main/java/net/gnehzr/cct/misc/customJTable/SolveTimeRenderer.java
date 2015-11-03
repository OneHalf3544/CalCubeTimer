package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Will highlight times from current average and from best rolling average
 */
public class SolveTimeRenderer extends TimeRenderer<SolveTime> {

	public SolveTimeRenderer(SessionsList sessionsList, Configuration configuration) {
		super(configuration, sessionsList);
	}

	@Override
	protected void processTime(SolveTime solveTime, int row, SessionSolutionsStatistics sessionStatistics) {
		setForeground(getForegroundColor(solveTime, sessionStatistics));

		RollingAverage currentRollingAverage = sessionStatistics.getCurrentRollingAverage(RollingAverageOf.OF_5);
		if (currentRollingAverage.containsTime(solveTime)) {
            boolean firstOfCurrentAverage = currentRollingAverage.getFirst() == solveTime;
            boolean lastOfCurrentAverage = currentRollingAverage.getLast() == solveTime;

            Border b;
            Color c = configuration.getColor(VariableKey.CURRENT_AVERAGE, false);
            if (firstOfCurrentAverage)
                b = BorderFactory.createMatteBorder(2, 2, 0, 2, c);
            else if(lastOfCurrentAverage)
                b = BorderFactory.createMatteBorder(0, 2, 2, 2, c);
            else
                b = BorderFactory.createMatteBorder(0, 2, 0, 2, c);
            setBorder(BorderFactory.createCompoundBorder(b, getBorder()));
        }
	}

	private Color getForegroundColor(SolveTime solveTime, SessionSolutionsStatistics sessionStatistics) {
		RollingAverage session = sessionStatistics.getSession().getRollingAverageForWholeSession();

		if (session.getBestTime() == solveTime) {
			return configuration.getColor(VariableKey.BEST_TIME, false);
        }
		if (session.getWorstTime() == solveTime) {
            return configuration.getColor(VariableKey.WORST_TIME, false);
        }

		return null;
	}

	@Override
	protected String valueToString(SolveTime value) {
		return value.toString();
	}

	@Override
	protected boolean isMemberOfBestRA(SolveTime value, SessionSolutionsStatistics sessionStatistics) {
		return sessionStatistics.getBestAverage(RollingAverageOf.OF_5).containsTime(value);
	}
}
