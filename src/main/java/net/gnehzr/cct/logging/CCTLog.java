package net.gnehzr.cct.logging;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class CCTLog {

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CCTLog.class);

	private CCTLog() {}
	static {
		try {
			LogManager.getLogManager().readConfiguration(CCTLog.class.getResourceAsStream("logging.properties"));
		} catch(SecurityException | IOException e) {
			LOG.info("unexpected exception", e);
		}
	}
	public static Logger getLogger(String loggerName) {
		return Logger.getLogger(loggerName);
	}
}
