package net.gnehzr.cct.keyboardTiming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.main.TimingListener;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
public class KeyboardHandler {

	private static final Logger LOGGER = LogManager.getLogger(KeyboardHandler.class);

	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

	private static final Duration PERIOD = Duration.ofMillis(45);

	private TimingListener timingListener;
	private final Configuration configuration;

	private boolean reset;
	private boolean inspecting;

	private Instant start;
	private Instant current;
	private ScheduledFuture<?> scheduledFuture;

	public boolean isReset() {
		return reset;
	}
	public boolean isInspecting() {
		return inspecting;
	}

	@Inject
	public KeyboardHandler(TimingListener timingListener, Configuration configuration) {
		this.timingListener = timingListener;
		this.configuration = configuration;
	}

	@Inject
	void initialize() {
		reset = true;
		inspecting = false;
		current = Instant.now();
		timingListener.initializeDisplay();
	}

	public void reset() {
		reset = true;
		inspecting = false;
		stop();
	}
	
	public boolean canStartTimer() {
		return Duration.between(current, Instant.now()).toMillis() > configuration.getInt(VariableKey.DELAY_BETWEEN_SOLVES);
	}

	public void startTimer() {
		boolean inspectionEnabled = configuration.getBoolean(VariableKey.COMPETITION_INSPECTION);
		if(!canStartTimer()) {
			return;
		}
		current = start = Instant.now();
		if(!inspectionEnabled || inspecting) {
			scheduledFuture = EXECUTOR.scheduleAtFixedRate(
					this::refreshTime, 0, PERIOD.toMillis(), TimeUnit.MILLISECONDS);
			inspecting = false;
			reset = false;
			timingListener.timerStarted();
		} else {
			inspecting = true;
			timingListener.inspectionStarted();
		}
	}

	protected void refreshTime() {
		current = Instant.now();
		timingListener.refreshDisplay(getTimerState());
	}
	
	private TimerState getTimerState() {
		return new TimerState(configuration, getElapsedTimeSeconds());
	}

	private Duration getElapsedTimeSeconds() {
		if(reset) {
			return Duration.ZERO;
		}
		return Duration.between(start, current);
	}

	public void split() {
		timingListener.timerSplit(getTimerState());
	}

	public void stop() {
		current = Instant.now();
		if (isRunning()) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		} else {
			LOGGER.info("nothing to stop");
		}
		timingListener.refreshDisplay(getTimerState());
	}

	public void fireStop() {
		timingListener.timerStopped(getTimerState());
		reset = true;
		current = Instant.now();
	}

	public boolean isRunning() {
		return scheduledFuture != null && !scheduledFuture.isCancelled();
	}
}
