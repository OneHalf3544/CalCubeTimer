package net.gnehzr.cct.keyboardTiming;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.main.TimingListener;
import net.gnehzr.cct.stackmatInterpreter.TimerState;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;

public class KeyboardHandler extends Timer {

	private static final int PERIOD = 90; //measured in milliseconds

	private TimingListener timingListener;

	public KeyboardHandler(TimingListener timingListener) {
		super(PERIOD, null);
		this.timingListener = timingListener;
		reset();
	}

	public void reset() {
		reset = true;
		inspecting = false;
		this.stop();
	}
	
	public boolean canStartTimer() {
		return Duration.between(current, Instant.now()).toMillis() > Configuration.getInt(VariableKey.DELAY_BETWEEN_SOLVES, false);
	}

	private Instant start;

	public void startTimer() {
		boolean inspectionEnabled = Configuration.getBoolean(VariableKey.COMPETITION_INSPECTION, false);
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
	
	private Instant current;
	@Override
	protected void fireActionPerformed(ActionEvent e) {
		current = Instant.now();
		timingListener.refreshDisplay(getTimerState());
	}
	
	private TimerState getTimerState() {
		return new TimerState(getElapsedTimeSeconds());
	}

	private Duration getElapsedTimeSeconds() {
		if(reset) {
			return Duration.ZERO;
		}
		return Duration.between(start, current);
	}

	private boolean reset;
	private boolean inspecting;
	public boolean isReset() {
		return reset;
	}
	public boolean isInspecting() {
		return inspecting;
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
