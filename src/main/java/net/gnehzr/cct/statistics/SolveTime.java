package net.gnehzr.cct.statistics;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public class SolveTime extends Commentable implements Comparable<SolveTime> {

	private static final Logger LOG = Logger.getLogger(SolveTime.class);

	public static final SolveTime BEST = new SolveTime(0, null, null) {
		@Override
		public void setTime(String toParse, boolean importing) { throw new AssertionError(); };
	};
	public static final SolveTime WORST = new SolveTime(null) {
		@Override
		public void setTime(String toParse, boolean importing) { throw new AssertionError(); };
	};
	public static final SolveTime NA = WORST;

	private Set<SolveType> types = new HashSet<>();

	private Duration time;
	private String scramble = null;
	private List<SolveTime> splits = ImmutableList.of();

	//this constructor exists to allow the jtable of times to contain the averages also
	//we need to know the index so we can syntax highlight it
	private int whichRA = -1;

	private final Configuration configuration;

	public SolveTime(Configuration configuration) {
		this.configuration = configuration;
		time = null;
		setScramble(null);
	}

	public SolveTime(double seconds, String scramble, Configuration configuration) {
		this.time = Duration.ofMillis(10 * (long) (100 * seconds + .5));
		LOG.trace("new SolveTime " + seconds);
		setScramble(scramble);
		this.configuration = configuration;
	}
	public SolveTime(double seconds, int whichRA, Configuration configuration) {
		this(seconds, null, configuration);
		this.whichRA = whichRA;
	}

	private SolveTime(TimerState time, String scramble, Configuration configuration) {
		this.configuration = configuration;
		if(time != null) {
			this.time = time.getTime();
			LOG.trace("new SolveTime " + time.getTime());
		}
		setScramble(scramble);
	}

	public int getWhichRA() {
		return whichRA;
	}

	public SolveTime(TimerState time, String scramble, List<SolveTime> splits, Configuration configuration) {
		this(time, scramble, configuration);
		this.splits = splits;
	}

	public SolveTime(String time, String scramble, Configuration configuration) throws Exception {
		this.configuration = configuration;
		setTime(time, false);
		setScramble(scramble);
	}
	
	//This will create the appropriate scramble types if necessary, should probably only
	//be called when parsing the xml gui
	public void parseTime(String toParse) throws Exception {
		setTime(toParse, true);
	}

	protected void setTime(String toParse, boolean importing) throws Exception {
		time = Duration.ZERO; //don't remove this
		toParse = toParse.trim();
		if(toParse.isEmpty()) {
			throw new Exception(StringAccessor.getString("SolveTime.noemptytimes"));
		}
		
		String[] split = toParse.split(",");
		int c;
		for(c = 0; c < split.length - 1; c++) {
			SolveType t = SolveType.getSolveType(split[c]);
			if(t == null) {
				if(!importing) {
					throw new Exception(StringAccessor.getString("SolveTime.invalidtype"));
				}
				t = SolveType.createSolveType(split[c]);
			}
			types.add(t);
		}
		String time = split[c];
		if(time.equals(SolveType.DNF.toString())) { //this indicated a pure dnf (no time associated with it)
			types.add(SolveType.DNF);
			return;
		}
		
		//parse time to determine raw seconds
		if(time.endsWith("+")) {
			types.add(SolveType.PLUS_TWO);
			time = time.substring(0, time.length() - 1);
		}
		time = toUSFormatting(time);
		String[] temp = time.split(":");
		if(temp.length > 3 || time.lastIndexOf(":") == time.length() - 1) {
			throw new Exception(StringAccessor.getString("SolveTime.invalidcolons"));
		}
		if(time.indexOf(".") != time.lastIndexOf(".")) {
			throw new Exception(StringAccessor.getString("SolveTime.toomanydecimals"));
		}
		if(time.contains(".") && time.contains(":") && time.indexOf(".") < time.lastIndexOf(":")) {
			throw new Exception(StringAccessor.getString("SolveTime.invaliddecimal"));
		}
		if(time.contains("-")) {
			throw new Exception(StringAccessor.getString("SolveTime.nonpositive"));
		}

		double seconds = 0;
		for(int i = 0; i < temp.length; i++) {
			seconds *= 60;
			double d = 0;
			try {
				d = Double.parseDouble(temp[i]); //we want this to handle only "." as a decimal separator
			} catch(NumberFormatException e) {
				throw new Exception(StringAccessor.getString("SolveTime.invalidnumerals"));
			}
			if(i != 0 && d >= 60) throw new Exception(StringAccessor.getString("SolveTime.toolarge"));
			seconds += d;
		}
		seconds -= (isType(SolveType.PLUS_TWO) ? 2 : 0);
		if(seconds < 0) {
			throw new Exception(StringAccessor.getString("SolveTime.nonpositive"));
		}
		if(seconds > 21000000) {
			throw new Exception(StringAccessor.getString("SolveTime.toolarge"));
		}
		this.time = Duration.ofMillis(10 * (int)(100 * seconds + .5));
	}
	static String toUSFormatting(String time) {
		return time.replaceAll(Pattern.quote(Utils.getDecimalSeparator()), ".");
	}
	
	public void setScramble(String scramble) {
		this.scramble = scramble;
	}

	public String getScramble() {
		return scramble == null ? "" : scramble;
	}

	public Duration getTime() {
		return time;
	}

	//this is for display by CCT
	public String toString() {
		if(time == null || time.isNegative()) {
			return "N/A";
		}
		return types.stream()
				.filter(t -> !t.isSolved())
				.findFirst()
				.map(Object::toString)
				.orElseGet(() -> Utils.formatTime(this, configuration.getBoolean(VariableKey.CLOCK_FORMAT, false) ) + (isType(SolveType.PLUS_TWO) ? "+" : ""));
	}

	//this is for use by the database, and will save the raw time if the solve was a POP or DNF
	public String toExternalizableString() {
		String time = "" + (value() / 100.); //this must work for +2 and DNF
		String typeString = "";
		boolean plusTwo = false;
		for(SolveType t : types) {
			if(t == SolveType.PLUS_TWO) //no need to append plus two, since we will append + later
				plusTwo = true;
			else
				typeString += t.toString() + ",";
		}
		
		if(plusTwo) time += "+";
		return typeString + time;
	}

	public String toSplitsString() {
		return Joiner.on(", ").join(splits);
	}
	
	//this follows the same formatting as the above method spits out
	public void setSplitsFromString(String splitsString) {
		this.splits = new ArrayList<>();
		for(String s : splitsString.split(", *")) {
			try {
				this.splits.add(new SolveTime(s, null, configuration));
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	public double secondsValue() {
		if(isInfiniteTime()) return Double.POSITIVE_INFINITY;
		return value() / 100.;
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
		return obj == this;
	}

	@Override
	public int compareTo(SolveTime o) {
		if(o == WORST)
			return -1;
		if(this == WORST)
			return 1;
		if(o.isInfiniteTime())
			return -1;
		if(this.isInfiniteTime())
			return 1;
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
		types = new HashSet<>(newTypes);
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

	public List<SolveTime> getSplits() {
		return splits;
	}
}
