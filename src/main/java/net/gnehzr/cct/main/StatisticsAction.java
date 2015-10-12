package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.RollingAverageOf;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
class StatisticsAction extends AbstractAction {

	private StatsDialogHandler statsHandler;
	private CurrentSessionSolutionsTableModel model;
	private SessionPuzzleStatistics.AverageType type;
	private RollingAverageOf num;

	public StatisticsAction(CALCubeTimerFrame cct, CurrentSessionSolutionsTableModel model,
							SessionPuzzleStatistics.AverageType type, RollingAverageOf num,
							Configuration configuration){
		statsHandler = new StatsDialogHandler(cct, configuration);
		this.model = model;
		this.type = type;
		this.num = num;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		statsHandler.syncWithStats(model, type, num);
		statsHandler.setVisible(true);
	}
}
