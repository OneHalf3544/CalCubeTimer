package net.gnehzr.cct.statistics;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class RollingAverageTime implements Comparable<RollingAverageTime> {

	private final SolveTime time;
	private final RollingAverageOf whichRA;

	public RollingAverageTime(SolveTime time, RollingAverageOf whichRA) {
		this.time = time;
		this.whichRA = whichRA;
	}

	public SolveTime getTime() {
		return time;
	}

	public RollingAverageOf getWhichRA() {
		return whichRA;
	}

	@Override
	public String toString() {
		return time.toString();
	}

	@Override
	public int compareTo(@NotNull RollingAverageTime o) {
		return Comparator.comparing(RollingAverageTime::getTime).compare(this, o);
	}

}
