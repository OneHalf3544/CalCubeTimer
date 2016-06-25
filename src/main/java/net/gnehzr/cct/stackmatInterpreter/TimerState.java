package net.gnehzr.cct.stackmatInterpreter;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.statistics.Solution;
import net.gnehzr.cct.statistics.SolveTime;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public abstract class TimerState implements Comparable<TimerState> {

	public static final TimerState ZERO = new TimerState(Duration.ZERO) {
		@Override
		public boolean isInspecting() {
			return false;
		}
	};

	private final Duration time;

	public TimerState(@NotNull Duration time) {
		this.time = time;
	}

	public Solution toSolution(@NotNull ScrambleString scramble, List<SolveTime> splits) {
		return new Solution(this, scramble, splits);
	}

	public Duration getTime() {
		return time;
	}

	public abstract boolean isInspecting();

	public int hashCode() {
		return this.getTime().hashCode();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof TimerState)) {
			return false;
		}
		TimerState o = (TimerState) obj;
		return Objects.equals(this.getTime(), o.getTime());
	}

	@Override
	public int compareTo(@NotNull TimerState o) {
		if(o.getTime() == null) {
			return (int) this.getTime().toMillis();
		}
		return this.getTime().compareTo(o.getTime());
	}

	public String toString(Configuration configuration) {
		return new SolveTime(getTime()).toString(configuration);
	}

	@Override
	public String toString() {
		return Utils.formatTime(new SolveTime(getTime()), true);
	}
}
