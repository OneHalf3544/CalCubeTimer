package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;

public interface DynamicStringSettable {

	void setDynamicString(DynamicString s);

	void updateTextFromDynamicString(Configuration configuration, SessionsList sessionsList);
}
