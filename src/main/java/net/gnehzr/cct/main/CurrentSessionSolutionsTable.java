package net.gnehzr.cct.main;

import net.gnehzr.cct.main.actions.*;
import net.gnehzr.cct.main.actions.ActionMap;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.statistics.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.customJTable.*;
import net.gnehzr.cct.scrambles.ScrambleString;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;

/**
 * <p>
 * <p>
 * Created: 03.11.2015 0:14
 * <p>
 *
 * @author OneHalf
 */
@org.springframework.stereotype.Component
public class CurrentSessionSolutionsTable extends DraggableJTable {

    @Autowired
    private SessionsList sessionsList;
    @Autowired
    private NumberSpeaker numberSpeaker;
    @Autowired
    private ActionMap actionMap;

    @Autowired
    public CurrentSessionSolutionsTable(Configuration configuration, SessionsList sessionsList,
										CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel,
										SolveTimeEditor solveTimeEditor, ScrambleStringEditor scrambleStringEditor) {
        super(configuration, false, true);
        setName("timesTable");

        setDefaultEditor(SolveTime.class, solveTimeEditor);
        setDefaultEditor(ScrambleString.class, scrambleStringEditor);

        setDefaultRenderer(SolveTime.class, new SolveTimeRenderer(sessionsList, configuration));
        setDefaultRenderer(RollingAverage.class, new RollingAverageRenderer(sessionsList, configuration));

        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setModel(currentSessionSolutionsTableModel);
        //TODO - this wastes space, probably not easy to fix...
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken
        currentSessionSolutionsTableModel.addTableModelListener(this::newSolutionAdded);
    }

    private void newSolutionAdded(TableModelEvent event) {
        final Solution latestSolution = sessionsList.getCurrentSession().getLastSolution();

        if (event.getType() == TableModelEvent.INSERT) {
            //make the new time visible
            this.invalidate(); //the table needs to be invalidated to force the new time to "show up"!!!
            Rectangle newTimeRect = this.getCellRect(sessionsList.getCurrentSession().getAttemptsCount(), 0, true);
            this.scrollRectToVisible(newTimeRect);

            numberSpeaker.speakTime(latestSolution.getTime());
        }

        actionMap.updateStatisticActionsStatuses();
    }
}
