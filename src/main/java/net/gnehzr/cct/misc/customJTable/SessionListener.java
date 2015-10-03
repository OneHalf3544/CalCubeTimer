package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.statistics.Session;

public interface SessionListener {

	void sessionSelected(Session s);

	void sessionsDeleted();
}
