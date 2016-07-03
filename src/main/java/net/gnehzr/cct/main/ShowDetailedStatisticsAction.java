package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.statistics.RollingAverageOf;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics;
import net.gnehzr.cct.statistics.SessionsList;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
public class ShowDetailedStatisticsAction extends AbstractNamedAction {

	private final StatsDialogHandler statsHandler;
	private final SessionSolutionsStatistics.AverageType type;
	private final RollingAverageOf num;
	private final Configuration configuration;
	private final SessionsList sessionsList;

	public ShowDetailedStatisticsAction(String actionName, SessionSolutionsStatistics.AverageType type, RollingAverageOf num, CALCubeTimerFrame cct,
										Configuration configuration, SessionsList sessionsList){
		super(actionName);
		this.configuration = configuration;
		this.sessionsList = sessionsList;
		statsHandler = new StatsDialogHandler(cct, configuration);
		this.type = type;
		this.num = num;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		statsHandler.setTitle(StringAccessor.getString("StatsDialogHandler.detailedstats") + " " + type.toString());
		statsHandler.textArea.setText(getTemplateFor(type).toString(sessionsList));
		statsHandler.setVisible(true);
	}


	@NotNull
	private DynamicString getTemplateFor(SessionSolutionsStatistics.AverageType currentAverageStatistics) {
		String template = configuration.getString(currentAverageStatistics.getConfKey(), false);
		template = template.replace("ra(-1", "ra(" + num.getCode());
		return new DynamicString(template, null, configuration);
	}
}
