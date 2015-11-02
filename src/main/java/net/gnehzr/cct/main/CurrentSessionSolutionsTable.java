package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.SolveTimeEditor;
import net.gnehzr.cct.misc.customJTable.SolveTimeRenderer;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.SolveTime;

import javax.swing.*;

/**
 * <p>
 * <p>
 * Created: 03.11.2015 0:14
 * <p>
 *
 * @author OneHalf
 */
@Singleton
public class CurrentSessionSolutionsTable extends DraggableJTable {

	@Inject
    public CurrentSessionSolutionsTable(Configuration configuration, SessionsList sessionsList,
										CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel,
										SolveTimeEditor solveTimeEditor) {
        super(configuration, false, true);
        setName("timesTable");
        setDefaultEditor(SolveTime.class, solveTimeEditor);
        setDefaultRenderer(SolveTime.class, new SolveTimeRenderer(sessionsList, configuration));
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setModel(currentSessionSolutionsTableModel);
        //TODO - this wastes space, probably not easy to fix...
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }
}
