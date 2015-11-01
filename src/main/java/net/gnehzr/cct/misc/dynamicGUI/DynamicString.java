package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple2;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static net.gnehzr.cct.misc.dynamicGUI.DStringPart.Type.*;

public class DynamicString{

	private static final Logger LOG = LogManager.getLogger(DynamicString.class);

	// 1 group - content of parenthesis, 2 group - rest of line
	private static final Pattern ARG_PATTERN = Pattern.compile("^\\s*\\(([^)]*)\\)\\s*(.*)$");

	private final String rawString;
	private final List<DStringPart> dStringParts;
	private final MessageAccessor accessor;
	private final Configuration configuration;
	public static final Pattern PROGRESS_PATTERN = Pattern.compile("^\\.(progress)\\s*(.*)$");

	public DynamicString(@NotNull String rawString,
						 MessageAccessor accessor, Configuration configuration){
		this.rawString = rawString;
		this.accessor = accessor;
		this.configuration = configuration;
		dStringParts = parsePlaceholders(rawString);
	}

	private List<DStringPart> parsePlaceholders(String rawString) {
		return parsePlaceholders(rawString, I18N_TEXT, "i18n[", "]",
				s1 -> parsePlaceholders(s1, STATISTICS_TEXT, "stats[", "]",
						s2 -> parsePlaceholders(s2, CONFIGURATION_TEXT, "config[", "]",
								s3 -> Collections.singletonList(new DStringPart(s3, RAW_TEXT)))));
	}

	static List<DStringPart> parsePlaceholders(String rawString, DStringPart.Type type,
											   String startBorder, String endBorder,
											   Function<String, List<DStringPart>> subparser) {
		if (!rawString.contains(startBorder) || !rawString.contains(endBorder)) {
			return subparser.apply(rawString);
		}

		List<DStringPart> splitUp = new ArrayList<>();

		String before = rawString.substring(0, rawString.indexOf(startBorder, 0));
		String dString = rawString.substring(before.length() + startBorder.length(), rawString.indexOf(endBorder, before.length() + startBorder.length()));
		String after = rawString.substring(before.length() + startBorder.length() + endBorder.length() + dString.length());

		splitUp.addAll(subparser.apply(before));
		splitUp.add(new DStringPart(dString, type));
		splitUp.addAll(parsePlaceholders(after, type, startBorder, endBorder, subparser));

		return splitUp.stream()
				.filter(s -> !s.getString().isEmpty())
				.collect(toList());
	}

	static Tuple2<String[], String> parseArguments(String originalString) {
		Matcher matcher = ARG_PATTERN.matcher(originalString);
		if (!matcher.matches()) {
			throw unimplementedError(originalString);
		}
		String[] arguments = Arrays.stream(matcher.group(1).split(","))
				.map(String::trim)
				.toArray(String[]::new);

		return new Tuple2<>(arguments, matcher.group(2));
	}

	public List<DStringPart> getParts() {
		return dStringParts;
	}

	@Override
	public String toString(){
		return "DynamicString: " + rawString;
	}


	public String toString(SessionsList sessions) {
		return toString(null, sessions);
	}

	public String toString(RollingAverageOf num, SessionsList sessions) {
		LOG.trace("process string {}", this);
		return dStringParts.stream()
				.map(s -> s.toString(this, accessor, num, sessions, configuration))
				.collect(Collectors.joining());
	}

	public String getRawText() {
		return rawString;
	}

	private String formatProgressTime(SolveTime progress, boolean addParens) {
		String result = "";
		if(progress.isInfiniteTime()) {
			if (addParens) {
				return "";
			}
			result = "+\u221E"; //unicode for infinity
		} else {
			result = (progress.isNegative() ? "-" : "+") + Utils.formatTime(progress, configuration.useClockFormat());
		}
		return addParens ? "(" + result + ")" : result;
	}

	String getReplacement(DStringPart dStringPart, RollingAverageOf num, SessionsList sessions){
		if ("date".equalsIgnoreCase(dStringPart.getString())) {
			return configuration.getDateFormat().format(LocalDateTime.now());
		}

		SessionSolutionsStatistics stats = Objects.requireNonNull(sessions.getCurrentSession()).getStatistics();
		Pattern p = Pattern.compile("^\\s*(global|session|ra)\\s*(.*)$");

		Matcher m = p.matcher(dStringPart.getString().toLowerCase());
		String originalString = dStringPart.getString();

		if (!m.matches()) {
			throw unimplementedError(originalString);
		}

		switch (m.group(1)) {
			case "global":
				return getForGlobal(num, sessions, originalString, m.group(2));

			case "session":
				return getForSession(sessions, originalString, stats, m.group(2));

			case "ra":
				return getForRollingAverage(num, originalString, stats, m.group(2));

			default:
				throw unimplementedError(originalString);
		}
	}

	private String getForGlobal(RollingAverageOf num, SessionsList sessions, String originalString, String suffix) {
		//Database queries for current puzzleType
		GlobalPuzzleStatistics globalPuzzleStatistics = sessions.getGlobalPuzzleStatisticsForType(sessions.getCurrentSession().getPuzzleType());
		Pattern globalPattern = Pattern.compile("^\\s*\\.\\s*(time|ra|average|solvecount)\\s*(.*)$");
		Matcher globalMatcher = globalPattern.matcher(suffix);

		if (!globalMatcher.matches()) {
            throw unimplementedError(originalString);
        }

		switch (globalMatcher.group(1)) {
			case "time":
				return getGlobalBestTime(originalString, globalPuzzleStatistics, globalMatcher);

			case "ra":
				return getGlobalRollingAverage(num, originalString, globalPuzzleStatistics, globalMatcher);

			case "average":
				return Utils.formatTime(globalPuzzleStatistics.getGlobalAverage(), configuration.useClockFormat());

			case "solvecount":
				return handleSolveCount(globalMatcher.group(2), globalPuzzleStatistics.getSolveCounter());

			default:
				throw unimplementedError(originalString);
		}
	}

	private String getGlobalRollingAverage(RollingAverageOf num, String originalString, GlobalPuzzleStatistics globalPuzzleStatistics, Matcher globalMatcher) {
		Tuple2<String[], String> arguments = parseArguments(globalMatcher.group(2));
		if (num == null) {
            try {
                num = RollingAverageOf.byCode(arguments.v1[0]);
            } catch (NumberFormatException e) {
                return invalidArgument(arguments.v1[0], originalString);
            }
        }

		if (arguments.v1.length < 2) {
            throw invalidNumberOfArguments(originalString);
        }
        else {
            String avg = arguments.v1[1].trim();
            if (avg.equals("best")) {
				return Utils.formatTime(globalPuzzleStatistics.getBestRA(num), configuration.useClockFormat());
			}
			throw unimplementedError(avg + " : " + originalString);
		}
	}

	private String getForSession(SessionsList sessions, String originalString, SessionSolutionsStatistics stats, String suffix) {
		Pattern sessionPattern = Pattern.compile("^\\.(solvecount|average|sd|list|stats|time|puzzletype)(.*)$");
		Matcher sessionMatcher = sessionPattern.matcher(suffix);
		if (!sessionMatcher.matches()) {
            throw unimplementedError(originalString);
        }

		switch (sessionMatcher.group(1)) {
            case "puzzletype":
				return sessions.getCurrentSession().getPuzzleType().toString();

            case "solvecount":
				return handleSolveCount(sessionMatcher.group(2), stats.getSolveCounter());

            case "average":
                if (sessionMatcher.group(2).isEmpty()) {
                    SolveTime ave = stats.getSessionAvg(); //this method returns zero if there are no solves to allow the global stats to be computed nicely
                    if (ave.isZero()) {
                        ave = SolveTime.NOT_AVAILABLE;
                    }
                    return Utils.formatTime(ave, configuration.useClockFormat());
                } else {
                    Matcher progressMatcher = PROGRESS_PATTERN.matcher(sessionMatcher.group(2));
                    if (progressMatcher.matches()) {
                        if (progressMatcher.group(1).equals("progress")) {
                            boolean parens = hasFilter(progressMatcher.group(2), "parens");
                            return formatProgressTime(stats.getProgressSessionAverage(), parens);
                        }
                    }
                }
				throw unimplementedError(originalString);

			case "sd":
				switch (sessionMatcher.group(2)) {
					case "":
                    	return Utils.formatTime(stats.getWholeSessionAverage().getStandartDeviation(), configuration.useClockFormat());
					case "(best)" :
						// TODO make search value with best standardDeviation.
						return Utils.formatTime(stats.getWholeSessionAverage().getStandartDeviation(), configuration.useClockFormat());
					case "(worst)":
						// TODO make search value with worst standardDeviation.
						return Utils.formatTime(stats.getWholeSessionAverage().getStandartDeviation(), configuration.useClockFormat());
				}
				Matcher progressMatcher = PROGRESS_PATTERN.matcher(sessionMatcher.group(2));
				if (progressMatcher.matches()) {
                    if (progressMatcher.group(1).equals("progress")) {
                        boolean parens = hasFilter(progressMatcher.group(2), "parens");
                        return formatProgressTime(stats.getProgressSessionStandardDeviation(), parens);
                    }
                }
				throw unimplementedError(originalString);

            case "list":
				return stats.getSessionAverageList();

            case "stats":
                boolean splits = hasFilter(sessionMatcher.group(2), "splits");
				return stats.toStatsString(AverageType.SESSION_AVERAGE, splits, RollingAverageOf.OF_5);

            case "time":
                Tuple2<String[], String> timeMatcher = parseArguments(sessionMatcher.group(2));
				String u = timeMatcher.v1[0];
				switch (u) {
                    case "progress":
                        boolean parens = hasFilter(timeMatcher.v2, "parens");
                        return formatProgressTime(stats.getProgressTime(), parens);
                    case "best":
                        return stats.getWholeSessionAverage().getBestTime().toString(configuration);
                    case "worst":
                        return stats.getWholeSessionAverage().getWorstTime().toString(configuration);
                    case "recent":
                        return Utils.formatTime(stats.getCurrentTime(), configuration.useClockFormat());
                    case "last":
                        return Utils.formatTime(stats.getPreviousTime(), configuration.useClockFormat());
                    default:
                        throw unimplementedError(u + " : " + originalString);
                }
			default:
                throw unimplementedError(sessionMatcher.group(1) + " : " + originalString);
        }
	}

	private String invalidArgument(String argument, String originalString) {
		return "Invalid argument: " + argument  + " : " + originalString;
	}

	private String getForRollingAverage(RollingAverageOf num, String originalString, SessionSolutionsStatistics stats, String suffix) {
		Tuple2<String[], String> raMatcher = parseArguments(suffix);
		if (raMatcher.v1.length == 0) {
			throw invalidNumberOfArguments(originalString);
		}
		if (num == null) {
            num = RollingAverageOf.byCode(raMatcher.v1[0]);
        }

		if (raMatcher.v1.length == 1) {
            Pattern arg1Pattern = Pattern.compile("^\\s*\\.\\s*(sd|progress|size)\\s*(.*)$");
            Matcher arg1Matcher = arg1Pattern.matcher(raMatcher.v2);

			if (!arg1Matcher.matches()) {
                throw unimplementedError(originalString);
            }

			if (arg1Matcher.group(1).equals("sd")) {
                Tuple2<String[], String> sdArgMatcher = parseArguments(arg1Matcher.group(2));
                if (sdArgMatcher.v1[0].equals("best")) {
                    return Utils.formatTime(stats.getByBestStandardDeviation(num).getStandartDeviation(), configuration.useClockFormat());
                }
                else if (sdArgMatcher.v1[0].equals("worst")) {
                    return Utils.formatTime(stats.getByWorstStandardDeviation(num), configuration.useClockFormat());
                }
            } else {
                if (arg1Matcher.group(1).equals("progress")) {
                    boolean parens = hasFilter(arg1Matcher.group(2), "parens");
                    return formatProgressTime(stats.getProgressAverage(num), parens);
                } else if (arg1Matcher.group(1).equals("size")) {
					return Integer.toString(stats.getRASize(num));
				}
                else throw unimplementedError(originalString);
            }
		} else {
            String avg = raMatcher.v1[1];
            Pattern raPattern = Pattern.compile("^\\s*\\.\\s*(list|sd|time|stats)\\s*(.*)$");
            Matcher raMatcher2 = raPattern.matcher(raMatcher.v2);

			if (!raMatcher2.matches()) {
                switch (avg) {
                    case "best":
                        return Utils.formatTime(stats.getBestAverage(num).getAverage(), configuration.useClockFormat());
                    case "worst":
                        return Utils.formatTime(stats.getWorstRollingAverage(num).getAverage(), configuration.useClockFormat());
                    case "recent":
                        return Utils.formatTime(stats.getCurrentRollingAverage(num).getAverage(), configuration.useClockFormat());
                    case "last":
                        return Utils.formatTime(stats.getPreviousRollingAverage(num).getAverage(), configuration.useClockFormat());
                    default:
                        throw unimplementedError(avg + " : " + originalString);
                }
            } else {
				switch (raMatcher2.group(1)) {
                    case "list":
                        switch (avg) {
                            case "best":
                                return stats.getBestAverageList(num);
                            case "worst":
                                return stats.getWorstRollingAverage(num).toTerseString();
                            case "recent":
                                return stats.getCurrentAverageList(num);
                            case "last":
                                return stats.getPreviousRollingAverage(num).toTerseString();
                            default:
                                throw unimplementedError(avg + " : " + originalString);
                        }
                    case "sd": {
                        switch (avg) {
                            case "best":
                                return Utils.formatTime(stats.getBestAverageStandardDeviation(num), configuration.useClockFormat());
                            case "worst":
                                return Utils.formatTime(stats.getWorstAverageStandardDeviation(num), configuration.useClockFormat());
                            case "recent":
                                return Utils.formatTime(stats.getCurrentStandardDeviation(num), configuration.useClockFormat());
                            case "last":
                                return Utils.formatTime(stats.getLastStandardDeviation(num), configuration.useClockFormat());
                            default:
                                throw unimplementedError(avg + " : " + originalString);
                        }
                    }
                    case "time":
                        Tuple2<String[], String> timeMatcher = parseArguments(raMatcher2.group(2));

						String time = timeMatcher.v1[0];
						switch (avg) {
                            case "best":
								return getTimeOfRolingAverage(originalString, time, stats.getBestAverage(num));
							case "worst":
								return getTimeOfRolingAverage(originalString, time, stats.getWorstRollingAverage(num));
                            case "recent":
								return getTimeOfRolingAverage(originalString, time, stats.getCurrentRollingAverage(num));
							case "last":
								return getTimeOfRolingAverage(originalString, time, stats.getPreviousRollingAverage(num));
                            default:
                                throw unimplementedError(avg + " : " + originalString);
                        }
                    case "stats":
                        boolean splits = hasFilter(raMatcher2.group(2), "splits");
                        switch (avg) {
                            case "best":
                                return stats.toStatsString(AverageType.BEST_ROLLING_AVERAGE, splits, num);
                            case "recent":
								return stats.toStatsString(AverageType.CURRENT_ROLLING_AVERAGE, splits, num);
                            default:
                                throw unimplementedError(avg + " : " + originalString);
                        }
                    default:
                        throw unimplementedError(raMatcher2.group(1) + " : " + originalString);
                }
            }
		}
		throw unimplementedError(originalString);
	}

	private String getTimeOfRolingAverage(String originalString, String time, RollingAverage rollingAverage) {
		switch (time) {
            case "best":
                return rollingAverage.getBestTime().toString(configuration);
            case "worst":
                return rollingAverage.getWorstTime().toString(configuration);
            default:
                throw unimplementedError(time + " : " + originalString);
        }
	}

	@NotNull
	private IllegalArgumentException invalidNumberOfArguments(String originalString) {
		throw new IllegalArgumentException("Invalid number of arguments: " + originalString);
	}

	private String getGlobalBestTime(String originalString, GlobalPuzzleStatistics globalPuzzleStatistics, Matcher globalMatcher) {
		Tuple2<String[], String> timeMatcher = parseArguments(globalMatcher.group(2));
		String u = timeMatcher.v1[0];
		if (u.equals("best")) {
			return globalPuzzleStatistics.getBestTime().toString(configuration);
		}
		throw unimplementedError(u + " : " + originalString);
	}

	@NotNull
	private static IllegalArgumentException unimplementedError(String originalString) {
		throw new IllegalArgumentException("Unimplemented: " + originalString);
	}

	private static boolean hasFilter(String s, String filter){
		return s.matches("\\|\\s*" + filter);
	}

	@NotNull
	private String handleSolveCount(String s, SolveCounter stats){
		Tuple2<String[], String> solvecountMatcher = parseArguments(s);

		String u = solvecountMatcher.v1[0];
		boolean percent = u.startsWith("%");
		if(percent) {
            u = u.substring(1);
        }
		int val = getCount(stats, u);
		return percent ? toPercent(stats, val) : Integer.toString(val);
	}

	private String toPercent(SolveCounter stats, int val) {
		return Utils.format(100. * val / stats.getAttemptCount());
	}

	private int getCount(SolveCounter stats, String u) {
		switch (u) {
            case "solved":
                return stats.getSolveCount();
            case "attempt":
                return stats.getAttemptCount();
            default:
                return stats.getSolveTypeCount(SolveType.getSolveType(u));
        }
	}

}
