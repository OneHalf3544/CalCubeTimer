package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.Statistics.AverageType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicString{

	private static final char RAW_TEXT = 'a';
	private static final char I18N_TEXT = 'b';
	private static final char STAT = 'c';
	private static final String CONF = "configuration";

	private static final Pattern argPattern = Pattern.compile("^\\s*\\(([^)]*)\\)\\s*(.*)$");

	private String rawString;
	private String[] splitText;
	private CurrentSessionSolutionsTableModel statsModel;
	private MessageAccessor accessor;
	private final Configuration configuration;

	public DynamicString(String s, CurrentSessionSolutionsTableModel statsModel, MessageAccessor accessor, Configuration configuration){
		rawString = s;
		this.statsModel = statsModel;
		this.accessor = accessor;
		this.configuration = configuration;
		ArrayList<String> splitUp = new ArrayList<>();
		splitText = s.split("\\$\\$");
		for(int i = 0; i < splitText.length; i++) {
			splitText[i] = splitText[i].replaceAll("\\\\\\$", "\\$");
			if(i % 2 != 0) {
				splitText[i] = splitText[i].trim();
				if(!splitText[i].isEmpty()) {
					splitUp.add(STAT + splitText[i]);
				}
			} else if(!splitText[i].isEmpty()) {
				String[] text = splitText[i].split("%%");
				for(int ch = 0; ch < text.length; ch++) {
					text[ch] = text[ch].replaceAll("\\\\%", "%");
					if(ch % 2 != 0) {
						text[ch] = text[ch].trim();
						if(!text[ch].isEmpty())
							splitUp.add(I18N_TEXT + text[ch]);
					} else {
						splitUp.add(RAW_TEXT + text[ch]);
					}
				}
			}
		}
		splitText = splitUp.toArray(splitText);
	}

	public CurrentSessionSolutionsTableModel getStatisticsModel() {
		return statsModel;
	}

	public String toString(){
		return toString(-1);
	}

	public String toString(int num) {
		StringBuilder stringBuilder = new StringBuilder();

		for (String aSplitText : splitText) {
			if (aSplitText == null) {
				break;
			}
			char firstChar = aSplitText.charAt(0);
			String t = aSplitText.substring(1);
			switch (firstChar) {
				case I18N_TEXT:
					if (accessor != null) {
						stringBuilder.append(accessor.getString(t));
						break;
					}
				case STAT:
					if (statsModel != null) {
						stringBuilder.append(getReplacement(t, num));
						break;
					}
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

	private String getReplacement(String s, int num){
		String sorig = s;

		String r = "";

		//Configuration section
		if(s.startsWith(CONF.toLowerCase() + "(")) {
			if(s.endsWith(")")) {
				s = s.substring(CONF.length());
				return configuration.getString(s.substring(1, s.length() - 1));
			}
		}

		s = s.toLowerCase();

		Statistics stats = statsModel.getCurrentSession().getStatistics();
		if(stats == null)
			return r;

		Pattern p = Pattern.compile("^\\s*(global|session|ra|date)\\s*(.*)$");
		Matcher m = p.matcher(s);

		if(m.matches()){
			s = m.group(1);
		}
		else return "Unimplemented: " + sorig;

		Pattern progressPattern = Pattern.compile("^\\s*\\.\\s*(progress)\\s*(.*)$");

		switch (s) {
			case "global":
				//Database queries for current scramble customization
				SessionsListAndPuzzleStatistics ps = statsModel.getCurrentSession().getSessionsListAndPuzzleStatistics();
				Pattern globalPattern = Pattern.compile("^\\s*\\.\\s*(time|ra|average|solvecount)\\s*(.*)$");
				Matcher globalMatcher = globalPattern.matcher(m.group(2));
				if (globalMatcher.matches()) {
					String t = globalMatcher.group(1);
					switch (t) {
						case "time":
							Matcher timeMatcher = argPattern.matcher(globalMatcher.group(2));
							if (timeMatcher.matches()) {
								String u = timeMatcher.group(1);
								if (u.equals("best")) {
									r = ps.getBestTime().toString(configuration);
								}
								else {
									r = "Unimplemented: " + u + " : " + sorig;
								}
							} else {
								r = "Unimplemented: " + sorig;
							}
							break;
						case "ra":
							Matcher raMatcher = argPattern.matcher(globalMatcher.group(2));
							String[] args;
							if (!raMatcher.matches()) {
								return "Unimplemented: " + sorig;
							}

							args = raMatcher.group(1).split(",");

							if (num == -1) {
								try {
									num = Integer.parseInt(args[0]);
								} catch (NumberFormatException e) {
									return "Invalid argument: " + args[0] + " : " + sorig;
								}
							}

							if (args.length < 2) r = "Invalid number of arguments: " + sorig;
							else {
								String avg = args[1].trim();
								if (avg.equals("best"))
									r = Utils.formatTime(ps.getBestRA(num), configuration.useClockFormat());
								else {
									r = "Unimplemented: " + avg + " : " + sorig;
								}
							}
							break;
						case "average":
							r = Utils.formatTime(ps.getGlobalAverage(), configuration.useClockFormat());
							break;
						case "solvecount":
							r = handleSolveCount(globalMatcher.group(2), ps);
							if (r == null) r = "Unimplemented: " + sorig;
							break;
						default:
							r = "Unimplemented: " + sorig;
							break;
					}
				} else {
					r = "Unimplemented: " + sorig;
				}
				break;
			case "session":
				Pattern sessionPattern = Pattern.compile("^\\s*\\.\\s*(solvecount|average|sd|list|stats|time)\\s*(.*)$");
				Matcher sessionMatcher = sessionPattern.matcher(m.group(2));
				String t;
				if (sessionMatcher.matches()) {
					t = sessionMatcher.group(1);
				} else return "Unimplemented: " + sorig;

				switch (t) {
					case "solvecount":
						r = handleSolveCount(sessionMatcher.group(2), stats);
						if (r == null) r = "Unimplemented: " + sorig;
						break;
					case "average":
						if (sessionMatcher.group(2).isEmpty()) {
							SolveTime ave = stats.getSessionAvg(); //this method returns zero if there are no solves to allow the global stats to be computed nicely
							if (ave.isZero()) {
								ave = SolveTime.NA;
							}
							r = Utils.formatTime(ave, configuration.useClockFormat());
						} else {
							Matcher progressMatcher = progressPattern.matcher(sessionMatcher.group(2));
							if (progressMatcher.matches()) {
								if (progressMatcher.group(1).equals("progress")) {
									boolean parens = hasFilter(progressMatcher.group(2), "parens");
									r = formatProgressTime(stats.getProgressSessionAverage(), parens);
								}
							}
						}
						break;
					case "sd":
						if (sessionMatcher.group(2).isEmpty()) {
							r = Utils.formatTime(stats.getSessionSD(), configuration.useClockFormat());
						} else {
							Matcher progressMatcher = progressPattern.matcher(sessionMatcher.group(2));
							if (progressMatcher.matches()) {
								if (progressMatcher.group(1).equals("progress")) {
									boolean parens = hasFilter(progressMatcher.group(2), "parens");
									r = formatProgressTime(stats.getProgressSessionSD(), parens);
								}
							}
						}
						break;
					case "list":
						r = stats.getSessionAverageList();
						break;
					case "stats":
						boolean splits = hasFilter(sessionMatcher.group(2), "splits");
						r = stats.toStatsString(AverageType.SESSION, splits, 0);
						break;
					case "time":
						Matcher timeMatcher = argPattern.matcher(sessionMatcher.group(2));
						if (timeMatcher.matches()) {
							String u = timeMatcher.group(1);
							switch (u) {
								case "progress":
									boolean parens = hasFilter(timeMatcher.group(2), "parens");
									r = formatProgressTime(stats.getProgressTime(), parens);
									break;
								case "best":
									r = stats.getBestTime().toString(configuration);
									break;
								case "worst":
									r = stats.getWorstTime().toString(configuration);
									break;
								case "recent":
									r = Utils.formatTime(stats.getCurrentTime(), configuration.useClockFormat());
									break;
								case "last":
									r = Utils.formatTime(stats.getLastTime(), configuration.useClockFormat());
									break;
								default:
									r = "Unimplemented: " + u + " : " + sorig;
									break;
							}
						} else r = "Unimplemented: " + sorig;
						break;
					default:
						r = "Unimplemented: " + t + " : " + sorig;
						break;
				}
				break;
			case "ra":
				Matcher raMatcher = argPattern.matcher(m.group(2));
				String[] args;
				if (raMatcher.matches()) {
					args = raMatcher.group(1).split(",");
				} else return "Unimplemented: " + sorig;

				if (num == -1) {
					try {
						num = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						return "Invalid argument: " + args[0] + " : " + sorig;
					}
				}

				if (args.length == 0) r = "Invalid number of arguments: " + sorig;
				else if (args.length == 1) {
					Pattern arg1Pattern = Pattern.compile("^\\s*\\.\\s*(sd|progress|size)\\s*(.*)$");
					Matcher arg1Matcher = arg1Pattern.matcher(raMatcher.group(2));

					if (arg1Matcher.matches()) {
						if (arg1Matcher.group(1).equals("sd")) {
							Matcher sdArgMatcher = argPattern.matcher(arg1Matcher.group(2));
							if (sdArgMatcher.matches()) {
								if (sdArgMatcher.group(1).equals("best"))
									r = Utils.formatTime(stats.getBestSD(num), configuration.useClockFormat());
								else if (sdArgMatcher.group(1).equals("worst"))
									r = Utils.formatTime(stats.getWorstSD(num), configuration.useClockFormat());
							}
						}
						if (arg1Matcher.group(1).equals("progress")) {
							boolean parens = hasFilter(arg1Matcher.group(2), "parens");
							r = formatProgressTime(stats.getProgressAverage(num), parens);
						} else if (arg1Matcher.group(1).equals("size")) r = "" + stats.getRASize(num);
						else r = "Unimplemented: " + sorig;
					} else r = "Unimplemented: " + sorig;
				} else {
					String avg = args[1].trim();
					Pattern raPattern = Pattern.compile("^\\s*\\.\\s*(list|sd|time|stats)\\s*(.*)$");
					Matcher raMatcher2 = raPattern.matcher(raMatcher.group(2));
					String t2 = "";
					if (raMatcher2.matches()) {
						t2 = raMatcher2.group(1);

						switch (t2) {
							case "list":
								switch (avg) {
									case "best":
										r = stats.getBestAverageList(num);
										break;
									case "worst":
										r = stats.getWorstAverageList(num);
										break;
									case "recent":
										r = stats.getCurrentAverageList(num);
										break;
									case "last":
										r = stats.getLastAverageList(num);
										break;
									default:
										r = "Unimplemented: " + avg + " : " + sorig;
										break;
								}
								break;
							case "sd":
								switch (avg) {
									case "best":
										r = Utils.formatTime(stats.getBestAverageSD(num), configuration.useClockFormat());
										break;
									case "worst":
										r = Utils.formatTime(stats.getWorstAverageSD(num), configuration.useClockFormat());
										break;
									case "recent":
										r = Utils.formatTime(stats.getCurrentSD(num), configuration.useClockFormat());
										break;
									case "last":
										r = Utils.formatTime(stats.getLastSD(num), configuration.useClockFormat());
										break;
									default:
										r = "Unimplemented: " + avg + " : " + sorig;
										break;
								}
								break;
							case "time":
								Matcher timeMatcher = argPattern.matcher(raMatcher2.group(2));
								if (timeMatcher.matches()) {
									String time = timeMatcher.group(1);
									switch (avg) {
										case "best":
											switch (time) {
												case "best":
													r = stats.getBestTimeOfBestAverage(num).toString(configuration);
													break;
												case "worst":
													r = stats.getWorstTimeOfBestAverage(num).toString(configuration);
													break;
												default:
													r = "Unimplemented: " + time + " : " + sorig;
													break;
											}
											break;
										case "worst":
											switch (time) {
												case "best":
													r = stats.getBestTimeOfWorstAverage(num).toString(configuration);
													break;
												case "worst":
													r = stats.getWorstTimeOfWorstAverage(num).toString(configuration);
													break;
												default:
													r = "Unimplemented: " + time + " : " + sorig;
													break;
											}
											break;
										case "recent":
											switch (time) {
												case "best":
													r = stats.getBestTimeOfCurrentAverage(num).toString(configuration);
													break;
												case "worst":
													r = stats.getWorstTimeOfCurrentAverage(num).toString(configuration);
													break;
												default:
													r = "Unimplemented: " + time + " : " + sorig;
													break;
											}
											break;
										case "last":
											switch (time) {
												case "best":
													r = stats.getBestTimeOfLastAverage(num).toString(configuration);
													break;
												case "worst":
													r = stats.getWorstTimeOfLastAverage(num).toString(configuration);
													break;
												default:
													r = "Unimplemented: " + time + " : " + sorig;
													break;
											}
											break;
										default:
											r = "Unimplemented: " + avg + " : " + sorig;
											break;
									}
								} else r = "Unimplemented: " + sorig;
								break;
							case "stats":
								boolean splits = hasFilter(raMatcher2.group(2), "splits");
								switch (avg) {
									case "best":
										r = stats.toStatsString(AverageType.RA, splits, num);
										break;
									case "recent":
										r = stats.toStatsString(AverageType.CURRENT, splits, num);
										break;
									default:
										r = "Unimplemented: " + avg + " : " + sorig;
										break;
								}
								break;
							default:
								r = "Unimplemented: " + t2 + " : " + sorig;
								break;
						}
					} else {
						switch (avg) {
							case "best":
								r = Utils.formatTime(stats.getBestAverage(num), configuration.useClockFormat());
								break;
							case "worst":
								r = Utils.formatTime(stats.getWorstAverage(num), configuration.useClockFormat());
								break;
							case "recent":
								r = Utils.formatTime(stats.getCurrentAverage(num), configuration.useClockFormat());
								break;
							case "last":
								r = Utils.formatTime(stats.getLastAverage(num), configuration.useClockFormat());
								break;
							default:
								r = "Unimplemented: " + avg + " : " + sorig;
								break;
						}
					}
				}
				break;
			case "date":
				r = configuration.getDateFormat().format(LocalDateTime.now());
				break;
			default:
				r = "Unimplemented: " + sorig;
				break;
		}

		return r;
	}

	private static boolean hasFilter(String s, String filter){
		return s.matches("\\|\\s*" + filter);
	}

	private static String handleSolveCount(String s, SolveCounter stats){
		Matcher solvecountMatcher = argPattern.matcher(s);
		if(solvecountMatcher.matches()){
			String u = solvecountMatcher.group(1);
			boolean percent = u.startsWith("%");
			if(percent) u = u.substring(1);
			int val;
			switch (u) {
				case "solved":
					val = stats.getSolveCount();
					break;
				case "attempt":
					val = stats.getAttemptCount();
					break;
				default:
					val = stats.getSolveTypeCount(SolveType.getSolveType(u));
					break;
			}
			if(percent) {
				return Utils.format(100. * val / stats.getAttemptCount());
			}
			else {
				return "" + val;
			}
		}
		else return null;
	}
}
