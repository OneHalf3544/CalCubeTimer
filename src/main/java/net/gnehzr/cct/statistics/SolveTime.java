package net.gnehzr.cct.statistics;

import com.google.common.collect.ImmutableSet;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

public class SolveTime implements Comparable<SolveTime> {

	private static final Logger LOG = LogManager.getLogger(SolveTime.class);

	public static final SolveTime NULL_TIME = new SolveTime((Duration)null);
	public static final SolveTime ZERO_TIME = new SolveTime(Duration.ZERO);
	public static final SolveTime BEST = ZERO_TIME;
	public static final SolveTime WORST = new SolveTime((Duration)null);
	public static final SolveTime NA = WORST;

	private final Set<SolveType> types = new HashSet<>();

	private final Duration time;

	@Deprecated
	public SolveTime(double seconds) {
		this(Duration.ofMillis(10 * (long) (100 * seconds + .5)));
	}

	public SolveTime(Duration time) {
		this.time = time;
		LOG.trace("new SolveTime " + time);
	}

	public SolveTime(String time) {
		this(parseTime(time, ImmutableSet.of()));
	}

	protected static Duration parseTime(String toParse, Set<SolveType> types) {
		toParse = toParse.trim();
		if(toParse.isEmpty()) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.noemptytimes"));
		}
		
		String[] split = toParse.split(",");
		int c;
		for(c = 0; c < split.length - 1; c++) {
			SolveType t = SolveType.getSolveType(split[c]);
			if(t == null) {
				t = SolveType.createSolveType(split[c]);
			}
			types.add(t);
		}
		String time = split[c];
		if(time.equals(SolveType.DNF.toString())) { //this indicated a pure dnf (no time associated with it)
			types.add(SolveType.DNF);
			return Duration.ZERO;
		}
		
		//parse time to determine raw seconds
		if(time.endsWith("+")) {
			types.add(SolveType.PLUS_TWO);
			time = time.substring(0, time.length() - 1);
		}

		Duration seconds = parseSeconds(time, types);
		if(seconds.compareTo(Duration.ofDays(210)) > 0) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.toolarge"));
		}

		return seconds;
	}

	private static Duration parseSeconds(String time, Set<SolveType> types) {
		String[] temp = time.split(":");
		if(temp.length > 3 || time.lastIndexOf(":") == time.length() - 1) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.invalidcolons"));
		}
		if(time.indexOf(".") != time.lastIndexOf(".")) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.toomanydecimals"));
		}
		if(time.contains(".") && time.contains(":") && time.indexOf(".") < time.lastIndexOf(":")) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.invaliddecimal"));
		}
		if(time.contains("-")) {
			throw new IllegalArgumentException(StringAccessor.getString("SolveTime.nonpositive"));
		}

		double seconds = 0;
		for(int i = 0; i < temp.length; i++) {
			seconds *= 60;
			double d;
			try {
				d = Double.parseDouble(temp[i]); //we want this to handle only "." as a decimal separator
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException(StringAccessor.getString("SolveTime.invalidnumerals"));
			}
			if(i != 0 && d >= 60) {
				throw new IllegalArgumentException(StringAccessor.getString("SolveTime.toolarge"));
			}
			seconds += d;
		}
		Duration parse = Duration.ofMillis((long) (seconds * 1000));
		return types.contains(SolveType.PLUS_TWO) ? parse.minusSeconds(2) : parse;
	}

	public Duration getTime() {
		return time;
	}

	//this is for display by CCT
	public String toString(Configuration configuration) {
		if(time == null || time.isNegative()) {
			return "N/A";
		}
		boolean useClockFormat = configuration.isPropertiesLoaded()
				&& configuration.useClockFormat();

		return toString(useClockFormat);
	}

	//this is for display by CCT
	@Override
	public String toString() {
		if(time == null || time.isNegative()) {
			return "N/A";
		}
		return toString(true);
	}

	private String toString(boolean useClockFormat) {
		return types.stream()
				.filter(t -> !t.isSolved())
				.findFirst()
				.map(Object::toString)
				.orElseGet(() -> Utils.formatTime(this, useClockFormat) + (isType(SolveType.PLUS_TWO) ? "+" : ""));
	}

	private int value() {
		return (int) (time.toMillis() / 10  + (isType(SolveType.PLUS_TWO) ? 200 : 0));
	}

	//the behavior of the following 3 methods is kinda contradictory,
	//keep this in mind if you're ever going to use SolveTimes in complicated
	//data structures that depend on these methods
	@Override
	public int hashCode() {
		return this.value();
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != SolveTime.class) {
			return false;
		}
		SolveTime another = (SolveTime) obj;
		if (another.isInfiniteTime() && isInfiniteTime() ) {
			return true;
		}
		if (this.isInfiniteTime() || another.isInfiniteTime()) {
			return false;
		}
		return this.value() == another.value();
	}

	@Override
	public int compareTo(@NotNull SolveTime o) {
		if(o == WORST) {
			return -1;
		}
		if(this == WORST) {
			return 1;
		}
		if(o.isInfiniteTime()) {
			return -1;
		}
		if(this.isInfiniteTime()) {
			return 1;
		}
		return this.value() - o.value();
	}

	public List<SolveType> getTypes() {
		return new ArrayList<>(types);
	}

	public boolean isType(SolveType t) {
		return types.contains(t);
	}

	public void clearType() {
		types.clear();
	}

	public void setTypes(Collection<SolveType> newTypes) {
		types.clear();
		types.addAll(newTypes);
	}

	public boolean isPenalty() {
		return isType(SolveType.DNF) || isType(SolveType.PLUS_TWO);
	}

	public boolean isInfiniteTime() {
		return isType(SolveType.DNF) || time == null;
	}

	//"true" in the sense that it was manually entered as POP or DNF
	public boolean isTrueWorstTime(){
		return time.isZero() && isInfiniteTime();
	}

	public static SolveTime sum(SolveTime a, SolveTime b) {
		if (a.isInfiniteTime() || b.isInfiniteTime()) {
			return SolveTime.WORST;
		}
		return new SolveTime(a.getTime().plus(b.getTime()));
	}

	public static SolveTime divide(SolveTime a, long divisor) {
		if (a.isInfiniteTime()) {
			return SolveTime.WORST;
		}
 		return new SolveTime(a.getTime().dividedBy(divisor));
	}

	public static SolveTime multiply(SolveTime a, long multiplier) {
		if (a.isInfiniteTime()) {
			return SolveTime.WORST;
		}
		return new SolveTime(a.getTime().multipliedBy(multiplier));
	}

	public static SolveTime substruct(SolveTime time1, SolveTime time2) {
		if (time1.isInfiniteTime() || time2.isInfiniteTime()) {
			return SolveTime.NA;
		}
		return new SolveTime(time1.getTime().minus(time2.getTime()));
	}

	public boolean isZero() {
		return time != null && time.isZero();
	}

	public boolean isNegative() {
		return time != null && time.isNegative();
	}

	public boolean isDefined() {
		return !(isInfiniteTime() || isZero() || isNegative());
	}

	public boolean better(SolveTime solveTime) {
		return Utils.lessThan(this, solveTime);
	}
}
