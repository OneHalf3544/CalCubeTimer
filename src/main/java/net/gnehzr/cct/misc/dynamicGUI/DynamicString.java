package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.AverageType;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple2;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.gnehzr.cct.misc.dynamicGUI.DStringPart.Type.*;

public class DynamicString{

	// 1 group - content of parenthesis, 2 group - rest of line
	private static final Pattern ARG_PATTERN = Pattern.compile("^\\s*\\(([^)]*)\\)\\s*(.*)$");

	private final String rawString;
	private final List<DStringPart> splitText;
	private final MessageAccessor accessor;
	private final Configuration configuration;

	public DynamicString(@NotNull String rawString,
						 MessageAccessor accessor, Configuration configuration){
		this.rawString = rawString;
		this.accessor = accessor;
		this.configuration = configuration;
		splitText = parsePlaceholders(rawString);
	}

	private List<DStringPart> parsePlaceholders(String rawString) {
		return parsePlaceholders(rawString, I18N_TEXT, "%%",
				s1 -> parsePlaceholders(s1, STATISTICS_TEXT, "$$",
						s2 -> parsePlaceholders(s2, CONFIGURATION_TEXT, "@@",
								s3 -> Collections.singletonList(new DStringPart(s3, RAW_TEXT)))));
	}

	static List<DStringPart> parsePlaceholders(String rawString, DStringPart.Type type, String placeholderBorder,
											   Function<String, List<DStringPart>> subparser) {
		List<DStringPart> splitUp = new ArrayList<>();
		String[] splitText = rawString.split(Matcher.quoteReplacement(placeholderBorder));
		for (int ch = 0; ch < splitText.length; ch++) {
			String string = unescapeString(splitText, ch,
					Matcher.quoteReplacement("\\" + placeholderBorder.charAt(0)),
					placeholderBorder.substring(0, 1));

			if (ch % 2 != 0) {
				// is $$ placeholder name
				string = string.trim();
				if (!string.isEmpty()) {
					splitUp.add(new DStringPart(string, type));
				}
			} else {
				// unprocessed text between placeholders
				if (string.isEmpty()) {
					continue;
				}
				splitUp.addAll(subparser.apply(splitText[ch]));
			}
		}
		return splitUp;
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

	private static String unescapeString(String[] splitText, int i, String regex, String replacement) {
		return splitText[i].replaceAll(regex, replacement);
	}

	public List<DStringPart> getParts() {
		return splitText;
	}

	@Override
	public String toString(){
		return toString(null);
	}


	public String toString(SessionsList sessions) {
		return toString(null, sessions);
	}

	public String toString(RollingAverageOf num, SessionsList sessions) {
		StringBuilder stringBuilder = new StringBuilder();

		for (DStringPart aSplitText : splitText) {
			String t = Objects.requireNonNull(aSplitText).getString();
			switch (aSplitText.getType()) {
				case I18N_TEXT:
					if (accessor != null) {
						stringBuilder.append(accessor.getString(t));
						break;
					}
				case STATISTICS_TEXT:
				case CONFIGURATION_TEXT:
					stringBuilder.append(getReplacement(aSplitText, num, sessions));
					break;

				case RAW_TEXT:
					stringBuilder.append(t);
					break;
			}
		}
		return stringBuilder.toString();
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

	private String getReplacement(DStringPart dStringPart, RollingAverageOf num, SessionsList sessions){
		//Configuration section
		if (dStringPart.getType() == CONFIGURATION_TEXT) {
			return configuration.getString(dStringPart.getString());
		}

		String s = dStringPart.getString().toLowerCase();

		SessionPuzzleStatistics stats = Objects.requireNonNull(sessions.getCurrentSession()).getStatistics();

		Pattern p = Pattern.compile("^\\s*(global|session|ra|date)\\s*(.*)$");
		Matcher m = p.matcher(s);
		String originalString = dStringPart.getString();

		if (m.matches()){
			s = m.group(1);
		}
		else {
			throw unimplementedError(originalString);
		}


		switch (s) {
			case "global":
				return getForGlobal(num, sessions, originalString, m);

			case "session":
				return getForSession(originalString, stats, m);

			case "ra":
				return getForRollingAverage(num, originalString, stats, m);

			case "date":
				return configuration.getDateFormat().format(LocalDateTime.now());

			default:
				throw unimplementedError(originalString);
		}
	}

	private String getForGlobal(RollingAverageOf num, SessionsList sessions, String originalString, Matcher m) {
		//Database queries for current scramble customization
		GlobalPuzzleStatistics globalPuzzleStatistics = sessions.getGlobalPuzzleStatisticsForType(sessions.getCurrentSession().getPuzzleType());
		Pattern globalPattern = Pattern.compile("^\\s*\\.\\s*(time|ra|average|solvecount)\\s*(.*)$");
		Matcher globalMatcher = globalPattern.matcher(m.group(2));

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

	private String getForSession(String originalString, SessionPuzzleStatistics stats, Matcher m) {
		Pattern progressPattern = Pattern.compile("^\\s*\\.\\s*(progress)\\s*(.*)$");
		Pattern sessionPattern = Pattern.compile("^\\s*\\.\\s*(solvecount|average|sd|list|stats|time)\\s*(.*)$");
		Matcher sessionMatcher = sessionPattern.matcher(m.group(2));
		if (!sessionMatcher.matches()) {
            throw unimplementedError(originalString);
        }

		String t = sessionMatcher.group(1);
		switch (t) {
            case "solvecount":
				return handleSolveCount(sessionMatcher.group(2), stats.getSolveCounter());

            case "average":
                if (sessionMatcher.group(2).isEmpty()) {
                    SolveTime ave = stats.getSessionAvg(); //this method returns zero if there are no solves to allow the global stats to be computed nicely
                    if (ave.isZero()) {
                        ave = SolveTime.NA;
                    }
                    return Utils.formatTime(ave, configuration.useClockFormat());
                } else {
                    Matcher progressMatcher = progressPattern.matcher(sessionMatcher.group(2));
                    if (progressMatcher.matches()) {
                        if (progressMatcher.group(1).equals("progress")) {
                            boolean parens = hasFilter(progressMatcher.group(2), "parens");
                            return formatProgressTime(stats.getProgressSessionAverage(), parens);
                        }
                    }
                }
				throw unimplementedError(originalString);

			case "sd":
                if (sessionMatcher.group(2).isEmpty()) {
                    return Utils.formatTime(stats.getWholeSessionAverage().getStandartDeviation(), configuration.useClockFormat());
                }
				Matcher progressMatcher = progressPattern.matcher(sessionMatcher.group(2));
				if (progressMatcher.matches()) {
                    if (progressMatcher.group(1).equals("progress")) {
                        boolean parens = hasFilter(progressMatcher.group(2), "parens");
                        return formatProgressTime(stats.getProgressSessionSD(), parens);
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
                throw unimplementedError(t + " : " + originalString);
        }
	}

	private String invalidArgument(String argument, String originalString) {
		return "Invalid argument: " + argument  + " : " + originalString;
	}

	private String getForRollingAverage(RollingAverageOf num, String originalString, SessionPuzzleStatistics stats, Matcher m) {
		Tuple2<String[], String> raMatcher = parseArguments(m.group(2));
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
                    return Utils.formatTime(stats.getByWorstStandartDeviation(num), configuration.useClockFormat());
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
                                return Utils.formatTime(stats.getBestAverageSD(num), configuration.useClockFormat());
                            case "worst":
                                return Utils.formatTime(stats.getWorstAverageSD(num), configuration.useClockFormat());
                            case "recent":
                                return Utils.formatTime(stats.getCurrentSD(num), configuration.useClockFormat());
                            case "last":
                                return Utils.formatTime(stats.getLastSD(num), configuration.useClockFormat());
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
