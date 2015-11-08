package net.gnehzr.cct.main;

import net.gnehzr.cct.stackmatInterpreter.TimerState;

public interface TimingListener {

	void refreshDisplay(TimerState currTime);

	void stackmatChanged();

	void refreshTimer();

	void changeGreenLight(boolean b);
}
