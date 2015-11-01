package net.gnehzr.cct.misc.dynamicGUI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class DynamicStringSettableManger {

	private final List<DynamicStringSettable> dynamicStringSettables = new ArrayList<>();

	private final MessageAccessor xmlGuiMessages;
	private final StatisticsUpdateListener listener;

	@Inject
	public DynamicStringSettableManger(XMLGuiMessages xmlGuiMessages, Configuration configuration,
									   SessionsList sessionsList){
		this.xmlGuiMessages = xmlGuiMessages;
		listener = new StatisticsUpdateListener() {
			@Override
			public void statisticsUpdated() {
				for (DynamicStringSettable settable : dynamicStringSettables) {
					settable.updateTextFromDynamicString(configuration, sessionsList);
				}
			}
		};
		sessionsList.addStatisticsUpdateListener(listener);
		configuration.addConfigurationChangeListener(currentProfile -> listener.statisticsUpdated());
	}

	public void registerDynamicComponent(DynamicStringSettable dynamicString) {
		dynamicStringSettables.add(dynamicString);
	}

	public StatisticsUpdateListener asStatisticsUpdateListener() {
		return listener;
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
