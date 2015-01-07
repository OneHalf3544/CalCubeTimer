package net.gnehzr.cct.scrambles;

import com.google.common.base.Throwables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public final class TimeoutJob {

	private static final Logger LOG = Logger.getLogger(TimeoutJob.class);
	private final Configuration configuration;

	private TimeoutJob(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public static final ScramblePluginClassLoader PLUGIN_LOADER = new ScramblePluginClassLoader();

	private static class ThreadJob<T> extends Thread {
		T result;
		Throwable error;
		private Callable<T> callMe;
		public ThreadJob(Callable<T> callMe) {
			this.callMe = callMe;
		}
		@Override
		public void run() {
			try {
				result = callMe.call();
			} catch (Throwable e) {				
				error = e;
			}
		}
	}
	
	//throws TimeoutException if the job timed out
	public static <T> T doWork(final Callable<T> callMe, Configuration configuration) throws Throwable {
		ThreadJob<T> t = new ThreadJob<T>(callMe);
		t.setContextClassLoader(PLUGIN_LOADER);
		t.start();
		Integer timeout = null;
		try {
			timeout = configuration.getInt(VariableKey.SCRAMBLE_PLUGIN_TIMEOUT, false);
		} catch(Throwable c) {
			//we want to be able to handle no configuration at all
			LOG.warn("cannot get configuration parameter", c);
		}
		if(timeout == null)
			timeout = 0;
		try {
			t.join(timeout);
		} catch(InterruptedException e) {
			throw Throwables.propagate(e);
		}
		if(t.isAlive()) {
			t.stop();
			throw new TimeoutException("Job timed out after " + timeout + " milliseconds.");
		}
		if(t.error != null)
			throw t.error;
		return t.result;
	}

//	public static void main(String[] args) {
//		try { //exception
//			LOG.info(TimeoutJob.doWork(new Callable<String>() {
//				public String call() throws Exception {
//					return ((String)null).intern();
//				}
//			}));
//		} catch (Throwable e) {
//			LOG.info("unexpected exception", e);
//		}
//
//		try { //timeout
//			LOG.info(TimeoutJob.doWork(new Callable<String>() {
//				public String call() throws Exception {
//					for(;;);
//				}
//			}));
//		} catch (Throwable e) {
//			LOG.info("unexpected exception", e);
//		}
//		
//		final PrintStream ps = System.out;
//		System.setOut(new PrintStream(new OutputStream() {
//			boolean stamp = true;
//			public void write(int b) throws IOException {
//				if(stamp) {
//					ps.print(Thread.currentThread().getName() + "\t");
//				}
//				ps.write(b);
//				stamp = (b == '\n');
//			}
//		}));
//		LOG.info("awesome");
//		LOG.info("Wowiiee!");
//		try { //correct
//			LOG.info(TimeoutJob.doWork(new Callable<String>() {
//				public String call() throws Exception {
//					return "jeremy rocks!";
//				}
//			}));
//		} catch (Throwable e) {
//			LOG.info("unexpected exception", e);
//		}
//	}
}
