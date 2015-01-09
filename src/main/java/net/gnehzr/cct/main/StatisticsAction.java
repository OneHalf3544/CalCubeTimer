package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.Statistics;
import net.gnehzr.cct.statistics.StatisticsTableModel;

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
	private StatisticsTableModel model;
	private Statistics.AverageType type;
	private int num;
	public StatisticsAction(CALCubeTimer cct, StatisticsTableModel model, Statistics.AverageType type, int num,
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
