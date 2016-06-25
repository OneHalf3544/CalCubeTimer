package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Solution;

public interface SolvingProcessListener {

	void inspectionStarted();

	void timerStarted();

	void timerSplit(TimerState newSplit);

	void timerStopped();

	boolean confirmDuplicateTime(TimerState timerState);

	void timerAccepted(Solution newTime);

	void timerAccidentlyReset(TimerState lastTimeRead);
}
