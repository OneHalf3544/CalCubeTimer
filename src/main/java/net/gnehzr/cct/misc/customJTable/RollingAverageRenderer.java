package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.RollingAverage;
import net.gnehzr.cct.statistics.RollingAverageOf;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics;
import net.gnehzr.cct.statistics.SessionsList;

import java.util.Arrays;

/**
 * <p>
 * <p>
 * Created: 03.11.2015 23:38
 * <p>
 *
 * @author OneHalf
 */
public class RollingAverageRenderer extends TimeRenderer<RollingAverage> {

    public RollingAverageRenderer(SessionsList sessionsList, Configuration configuration) {
        super(configuration, sessionsList);
    }

    @Override
    protected void processTime(RollingAverage value, int row, SessionSolutionsStatistics sessionStatistics) {
    }

    @Override
    protected String valueToString(RollingAverage value) {
        return value.getAverage().toString();
    }

    @Override
    protected boolean isMemberOfBestRA(RollingAverage value, SessionSolutionsStatistics sessionStatistics) {
        return Arrays.stream(RollingAverageOf.values())
                .anyMatch(raOf -> isBestRollingAverage(value, sessionStatistics, raOf));
    }

    private boolean isBestRollingAverage(RollingAverage value, SessionSolutionsStatistics sessionStatistics, RollingAverageOf raOf) {
        RollingAverage bestAverage = sessionStatistics.getBestAverage(raOf);
        return bestAverage != RollingAverage.NOT_AVAILABLE && bestAverage == value;
    }
}
