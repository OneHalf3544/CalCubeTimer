package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.statistics.Session;

public interface SessionListener {

	void sessionSelected(Session s);

	default void sessionAdded(Session session) { }

	default void sessionStatisticsChanged(Session session) { }

	void sessionsDeleted();
}
