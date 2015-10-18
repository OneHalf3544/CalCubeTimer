package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Solution;

public interface TimingListener {
	/**
	 * Set 0.00 value for time labels
	 */
	void initializeDisplay();

	void refreshDisplay(TimerState currTime);

	void inspectionStarted();

	void timerStarted();

	void timerSplit(TimerState newSplit);

	void timerStopped(TimerState newTime);

	void timerAccepted(Solution newTime);

	void timerAccidentlyReset(TimerState lastTimeRead);

	void stackmatChanged();
}
