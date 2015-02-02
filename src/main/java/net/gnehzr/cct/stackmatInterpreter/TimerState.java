package net.gnehzr.cct.stackmatInterpreter;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SolveTime;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class TimerState implements Comparable<TimerState> {

	protected final Configuration configuration;

	private Duration time;

	public TimerState(Configuration configuration) {
		this.configuration = configuration;
	}

	public TimerState(Configuration configuration, @NotNull Duration time) {
		this.configuration = configuration;
		this.time = time;
	}

	public SolveTime toSolveTime(String scramble, List<SolveTime> splits) {
		return new SolveTime(this, scramble, splits);
	}

	public Duration getTime() {
		return time;
	}

	public int hashCode() {
		return this.getTime().hashCode();
	}
	public boolean equals(Object obj) {
		if(obj instanceof TimerState) {
			TimerState o = (TimerState) obj;
			return this.getTime() == o.getTime();
		}
		return false;
	}

	@Override
	public int compareTo(@NotNull TimerState o) {
		if(o == null || o.getTime() == null) {
			return (int) this.getTime().toMillis();
		}
		return this.getTime().compareTo(o.getTime());
	}
	@Override
	public String toString() {
		return toSolveTime(null, null).toString();
	}

	public void setTime(Duration value) {
		this.time = value;
	}
}
