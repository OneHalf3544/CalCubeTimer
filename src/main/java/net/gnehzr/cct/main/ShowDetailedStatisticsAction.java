package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.statistics.RollingAverageOf;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics;
import net.gnehzr.cct.statistics.SessionsList;
import org.jetbrains.annotations.NotNull;

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
class ShowDetailedStatisticsAction extends AbstractAction {

	private final StatsDialogHandler statsHandler;
	private final SessionPuzzleStatistics.AverageType type;
	private final RollingAverageOf num;
	private final Configuration configuration;
	private final SessionsList sessionsList;

	public ShowDetailedStatisticsAction(CALCubeTimerFrame cct,
										SessionPuzzleStatistics.AverageType type, RollingAverageOf num,
										Configuration configuration, SessionsList sessionsList){
		this.configuration = configuration;
		this.sessionsList = sessionsList;
		statsHandler = new StatsDialogHandler(cct, configuration);
		this.type = type;
		this.num = num;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		statsHandler.setTitle(StringAccessor.getString("StatsDialogHandler.detailedstats") + " " + type.toString());
		statsHandler.textArea.setText(getTemplateFor(type).toString(num, sessionsList));
		statsHandler.setVisible(true);
	}


	@NotNull
	private DynamicString getTemplateFor(SessionPuzzleStatistics.AverageType currentAverageStatistics) {
		return new DynamicString(configuration.getString(currentAverageStatistics.getConfKey(), false), null, configuration);
	}
}
