package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.TimerState;

public interface TimingListener {
	/**
	 * Set 0.00 value for time labels
	 */
	public void initializeDisplay();

	public void refreshDisplay(TimerState currTime);

	public void inspectionStarted();

	public void timerStarted();

	public void timerAccidentlyReset(TimerState lastTimeRead);

	public void timerStopped(TimerState newTime);

	public void timerSplit(TimerState newSplit);

	public void stackmatChanged();
}
