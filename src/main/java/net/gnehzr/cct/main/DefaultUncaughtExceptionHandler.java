package net.gnehzr.cct.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @see java.awt.EventDispatchThread#processException
 * @see Thread#dispatchUncaughtException
 * @see ThreadGroup#uncaughtException
 * @author Mykhaylo Adamovych
 */
public class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    public static final String SP_SUN_AWT_EXCEPTION_HANDLER = "sun.awt.exception.handler";
    static {
        if (Thread.getDefaultUncaughtExceptionHandler() == null)
            Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());
        if (System.getProperty(SP_SUN_AWT_EXCEPTION_HANDLER) == null)
            System.setProperty(SP_SUN_AWT_EXCEPTION_HANDLER, DefaultUncaughtExceptionHandler.class.getName());
    }

	private Logger LOG = LogManager.getLogger(DefaultUncaughtExceptionHandler.class);

	public static void initialize() {
        // load class and perform initialization
    }

    public void handle(Throwable e) {
        uncaughtException(Thread.currentThread(), e);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!(e instanceof ThreadDeath)) {
			LOG.error("uncaught exception", e);
		}
    }
}
