package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.TimerState;

public interface TimingListener {
	/**
	 * Set 0.00 value for time labels
	 */
	void initializeDisplay();

	void refreshDisplay(TimerState currTime);

	void inspectionStarted();

	void timerStarted();

	void timerAccidentlyReset(TimerState lastTimeRead);

	void timerStopped(TimerState newTime);

	void timerSplit(TimerState newSplit);

	void stackmatChanged();
}
