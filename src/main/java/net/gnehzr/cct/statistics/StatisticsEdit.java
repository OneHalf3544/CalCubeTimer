package net.gnehzr.cct.statistics;

import java.util.Arrays;
import java.util.List;

/**
* <p>
* <p>
* Created: 01.02.2015 16:05
* <p>
*
* @author OneHalf
*/
class StatisticsEdit implements CCTUndoableEdit {
    private Statistics statistics;
    private int[] positions;
    SolveTime[] oldTimes;
    private SolveTime newTime;
    public StatisticsEdit(Statistics statistics, int[] rows, SolveTime[] oldValues, SolveTime newValue) {
        this.statistics = statistics;
        positions = rows;
        oldTimes = oldValues;
        newTime = newValue;
    }
    int row = -1;

    private List<SolveType> oldTypes;
    private List<SolveType> newTypes;

    public StatisticsEdit(Statistics statistics, int row, List<SolveType> oldTypes, List<SolveType> newTypes) {
        this.statistics = statistics;
        this.row = row;
        this.oldTypes = oldTypes;
        this.newTypes = newTypes;
    }
    @Override
    public void doEdit() {
        if(row != -1) { //changed type
            statistics.times.get(row).setTypes(newTypes);
            statistics.refresh();
        } else { //time added/removed/changed
            statistics.editActions.setEnabled(false);
            if(oldTimes == null) { //add newTime
                statistics.add(positions[0], newTime);
            } else if(newTime == null) { //remove oldTimes
                statistics.remove(positions);
            } else { //change oldTime to newTime
                statistics.set(positions[0], newTime);
            }
            statistics.editActions.setEnabled(true);
        }
    }
    @Override
    public void undoEdit() {
        if(row != -1) { //changed type
            statistics.times.get(row).setTypes(oldTypes);
            statistics.refresh();
        } else { //time added/removed/changed
            statistics.editActions.setEnabled(false);
            if(oldTimes == null) { //undo add
                statistics.remove(positions);
            } else if(newTime == null) { //undo removal
                for(int ch = 0; ch < positions.length; ch++) {
                    if(positions[ch] >= 0) {
                        //we don't want this to change the scramble #
                        statistics.addSilently(positions[ch], oldTimes[ch]);
                    }
                }
            } else { //undo change
                statistics.set(positions[0], oldTimes[0]);
            }
            statistics.editActions.setEnabled(true);
        }
    }
    public String toString() {
        if(oldTimes == null) { //add newTime
            return "added"+newTime;
        } else if(newTime == null) { //remove oldTime
            return "removed"+ Arrays.toString(oldTimes);
        } else { //change oldTime to newTime
            return "changed"+oldTimes[0]+"->"+newTime;
        }
    }
}
