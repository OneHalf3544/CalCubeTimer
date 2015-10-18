package net.gnehzr.cct.misc.dynamicGUI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class DynamicStringSettableManger implements StatisticsUpdateListener, ConfigurationChangeListener {

	private final List<DynamicStringSettable> dynamicStringSettables = new ArrayList<>();

	private final ScramblePluginManager scramblePluginManager;
	private final MessageAccessor xmlGuiMessages;
	private final Configuration configuration;


	@Inject
	private SessionsList sessionsList;

	@Inject
	public DynamicStringSettableManger(ScramblePluginManager scramblePluginManager,
									   XMLGuiMessages xmlGuiMessages, Configuration configuration,
									   CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel){
		this.scramblePluginManager = scramblePluginManager;
		this.xmlGuiMessages = xmlGuiMessages;
		this.configuration = configuration;
		configuration.addConfigurationChangeListener(currentProfile -> {
			if (sessionsList != null) {
				update();
			}
		});
		currentSessionSolutionsTableModel.addStatisticsUpdateListener(this);
	}

	public void registerDynamicComponent(DynamicStringSettable dynamicString) {
		dynamicStringSettables.add(dynamicString);
	}

	@Override
	public void update() {
		for (DynamicStringSettable settable : dynamicStringSettables) {
			settable.updateTextFromDynamicString(configuration, sessionsList);
		}
	}

	@Override
	public void configurationChanged(Profile currentProfile) {

	}

	@Override
	public String toString() {
		return "DynamicStringManger{" +
				"xmlGuiMessages=" + xmlGuiMessages +
				'}';
	}

	public void destroy() {
		dynamicStringSettables.clear();
	}
}
