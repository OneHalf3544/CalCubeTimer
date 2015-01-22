package net.gnehzr.cct.keyboardTiming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.main.TimingListener;
import net.gnehzr.cct.stackmatInterpreter.TimerState;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;

@Singleton
public class KeyboardHandler extends Timer {

	private static final Duration PERIOD = Duration.ofMillis(90);

	private TimingListener timingListener;
	private final Configuration configuration;

	private boolean reset;
	private boolean inspecting;

	private Instant start;
	private Instant current;

	public boolean isReset() {
		return reset;
	}
	public boolean isInspecting() {
		return inspecting;
	}

	@Inject
	public KeyboardHandler(TimingListener timingListener, Configuration configuration) {
		super((int)PERIOD.toMillis(), null);
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
		return Duration.between(current, Instant.now()).toMillis() > configuration.getInt(VariableKey.DELAY_BETWEEN_SOLVES, false);
	}

	public void startTimer() {
		boolean inspectionEnabled = configuration.getBoolean(VariableKey.COMPETITION_INSPECTION, false);
		if(!canStartTimer()) {
			return;
		}
		current = start = Instant.now();
		if(!inspectionEnabled || inspecting) {
			start();
			inspecting = false;
			reset = false;
			timingListener.timerStarted();
		} else {
			inspecting = true;
			timingListener.inspectionStarted();
		}
	}

	@Override
	protected void fireActionPerformed(ActionEvent e) {
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

	@Override
	public void stop() {
		current = Instant.now();
		super.stop();
		timingListener.refreshDisplay(getTimerState());
	}

	public void fireStop() {
		timingListener.timerStopped(getTimerState());
		reset = true;
		current = Instant.now();
	}
}
