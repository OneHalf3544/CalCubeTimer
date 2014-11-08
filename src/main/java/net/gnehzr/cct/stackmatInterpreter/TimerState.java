package net.gnehzr.cct.stackmatInterpreter;

import net.gnehzr.cct.statistics.SolveTime;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class TimerState implements Comparable<TimerState> {

	public static final TimerState ZERO_STATE = new TimerState(Duration.ZERO);

	private Duration time;

	public TimerState() {
	}

	public TimerState(@NotNull Duration hundredths) {
		this.time = hundredths;
	}

	public SolveTime toSolveTime(String scramble, List<SolveTime> splits) {
		return new SolveTime(this, scramble, splits);
	}
	public Duration value() {
		return time;
	}
	public int hashCode() {
		return this.value().hashCode();
	}
	public boolean equals(Object obj) {
		if(obj instanceof TimerState) {
			TimerState o = (TimerState) obj;
			return this.value() == o.value();
		}
		return false;
	}
	public int compareTo(TimerState o) {
		if(o == null || o.getTime() == null) {
			return (int) this.value().toMillis();
		}
		return this.value().compareTo(o.value());
	}
	public String toString() {
		return toSolveTime(null, null).toString();
	}

	public void setValue(Duration value) {
		this.time = value;
	}

	public Duration getTime() {
		return time;
	}

	public void setTime(Duration time) {
		this.time = time;
	}
}
