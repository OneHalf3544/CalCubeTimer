package net.gnehzr.cct.statistics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class RollingAverageTime implements Comparable<RollingAverageTime> {

	private static final Logger LOG = LogManager.getLogger(RollingAverageTime.class);

	private final SolveTime time;
	private final int whichRA;

	public RollingAverageTime(SolveTime time, int whichRA) {
		this.time = time;
		this.whichRA = whichRA;
	}

	public SolveTime getTime() {
		return time;
	}

	public int getWhichRA() {
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
