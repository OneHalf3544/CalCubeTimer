package net.gnehzr.cct.umts.cctbot;

import com.google.common.collect.ImmutableMap;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.main.Main;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.umts.IRCListener;
import net.gnehzr.cct.umts.KillablePircBot;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CCTBot {

	private static final Logger LOGGER = Logger.getLogger(CCTBot.class);

	//max message length: 470 characters
	static final int MAX_MESSAGE = 470;

	private static final String CMD_RELOAD = "reload";
	private static final String CMD_LSVARIATIONS = "variations";
	private static final String CMD_CHANNELS = "channels";
	private static final String CMD_SERVER = "server";
	private static final String CMD_JOIN = "join";
	private static final String CMD_PART = "part";
	private static final String CMD_NICK = "nick";
	private static final String CMD_QUIT = "quit";
	private static final String CMD_MAX_SCRAMBLES = "maxscrambles";
	private static final String CMD_COMM_CHANNEL = "commchannel";
	private static final String CMD_PREFIX = "prefix";
	private static final String CMD_HELP = "help";

	private static final Map<String, String> COMMANDS = ImmutableMap.<String, String>builder()
			.put(CMD_RELOAD, "reload\n\tReloads scramble plugins from directory.")
			.put(CMD_LSVARIATIONS, "variations\n\tPrints available variations")
			.put(CMD_CHANNELS, "channels\n\tPrints channels cctbot is connected to.")
			.put(CMD_SERVER, "server\n\tPrints the status of the server cctbot is connected to.")
			.put(CMD_JOIN, "join #CHANNEL\n\tAttempts to join the specified channel")
			.put(CMD_PART, "part #CHANNEL (REASON)\n\tLeaves #CHANNEL with an optional REASON")
			.put(CMD_NICK, "nick NEWNICK\n\tChanges cctbots nickname.")
			.put(CMD_QUIT, "quit (REASON)\n\tDisconnects from server with optional REASON and shuts down cctbot.")
			.put(CMD_MAX_SCRAMBLES, "maxscrambles (#CHANNEL (COUNT))\n\tSets the maximum number of scrambles cctbot will give at a time on #CHANNEL to COUNT (-1 to remove the entry for #CHANNEL).\n" +
				"\tIf #CHANNEL is not specified, then the default max scrambles for any channel is set to COUNT.\n" +
				"\tIf neither #CHANNEL nor COUNT is specified, you see the max scrambles for each channel.")
			.put(CMD_COMM_CHANNEL, "commchannel (#CHANNEL)\n\tSets the comm channel that cctbot will respond with when users type !cct on a channel.\n" +
				"\tIf #CHANNEL is omitted, will display the current comm channel.")
			.put(CMD_PREFIX, "prefix (CHAR)\n\tSets the prefix cctbot will respond to to CHAR." +
				"\tIf CHAR is omitted, will display the current prefix. CHAR must be exactly one character long.")
			.put(CMD_HELP, "help (COMMAND)\n\tPrints available variations")
			.build();

	int MAX_SCRAMBLES = 12;
	public String PREFIX = "!";
	String cctCommChannel = null;
	KillablePircBot bot;
	private final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	private Map<String, Integer> scrambleMaxMap = new HashMap<>();
	private boolean shuttingdown = false;
	private boolean isConnected = false;


	private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	public CCTBot(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		this.scramblePluginManager = scramblePluginManager;
		this.configuration = configuration;
		bot = getKillableBot();
	}
	KillablePircBot getKillableBot() {
		String version = CCTBot.class.getPackage().getImplementationVersion();
		KillablePircBot bot = new KillablePircBot(createIrcListener(), "This is cctbot " + version, configuration);
		bot.setlogin("cctbot");
		bot.setname("cctbot");
		bot.setAutoNickChange(true);
		bot.setversion("CCTBot version " + version);
		return bot;
	}

	String getAvailableVariations() {
		return "Available variations: " + Arrays.toString(scramblePluginManager.getScrambleVariations());
	}

	void printPrompt() {
		System.out.print("cctbot: ");
		System.out.flush();
	}

	public void readEvalPrint() throws IOException {
		while(true) {
			printPrompt();
			while(!in.ready());
			String line = in.readLine();
			
			String[] commandAndArg = line.trim().split(" +", 2);
			String command = commandAndArg[0];
			String arg = commandAndArg.length == 2 ? commandAndArg[1] : null;
			if(command.equalsIgnoreCase(CMD_HELP)) {
				if(arg != null) {
					String usage = COMMANDS.get(arg);
					LOGGER.info(usage == null ? "Command " + arg + " not found." : "USAGE: " + usage);
				}
				if(arg == null) {
					StringBuilder cmds = new StringBuilder();
					for(String c : COMMANDS.keySet())
						cmds.append(", ").append(c);
					LOGGER.info("Available commands:\n\t" + cmds.substring(2));
				}
				continue;
			} else if(command.equalsIgnoreCase(CMD_RELOAD)) {
				LOGGER.info("Reloading scramble plugins...");
				scramblePluginManager.clearScramblePlugins();
				LOGGER.info(getAvailableVariations());
				continue;
			} else if(command.equalsIgnoreCase(CMD_CHANNELS)) {
				LOGGER.info("Connected to: " + Arrays.toString(bot.getChannels()));
				continue;
			} else if(command.equalsIgnoreCase(CMD_JOIN)) {
				if(arg != null && arg.startsWith("#")) {
					LOGGER.info("Attempting to join " + arg);
					bot.joinChannel(arg);
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_PART)) {
				if(arg != null && arg.startsWith("#")) {
					String[] chan_reason = arg.split(" +", 2);
					if(chan_reason.length == 2) {
						LOGGER.info("Leaving " + arg + " (" + chan_reason[1] + ")");
						bot.partChannel(chan_reason[0], chan_reason[1]);
					} else {
						LOGGER.info("Leaving " + arg);
						bot.partChannel(chan_reason[0]);
					}
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_NICK)) {
				if(arg != null) {
					LOGGER.info("/nick " + arg);
					bot.changeNick(arg);
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_LSVARIATIONS)) {
				LOGGER.info(getAvailableVariations());
				continue;
			} else if(command.equalsIgnoreCase(CMD_QUIT)) {
				shuttingdown = true;
				if(arg == null) {
					LOGGER.info("Exiting cctbot");
					bot.quitServer();
				} else {
					LOGGER.info("Exiting cctbot (" + arg + ")");
					bot.quitServer(arg);
				}
				Main.exit(0);
				continue;
			} else if(command.equalsIgnoreCase(CMD_MAX_SCRAMBLES)) {
				if(arg != null) {
					String[] chan_max = arg.split(" +", 2);
					if(chan_max[0].startsWith("#")) {
						try {
							int max = Integer.parseInt(chan_max[1]);
							if(max > 0) {
								scrambleMaxMap.put(chan_max[0], max);
								LOGGER.info("Max scrambles set to " + max + " for " + chan_max[0]);
								continue;
							} else if(max == -1) {
								scrambleMaxMap.remove(chan_max[0]);
								LOGGER.info("Max scramble info removed for " + chan_max[0]);
								continue;
							}
						} catch(NumberFormatException e) {
							LOGGER.info("ignored exception", e);}
					} else {
						try {
							int c = Integer.parseInt(chan_max[0]);
							if(c > 0) {
								MAX_SCRAMBLES = c;
								LOGGER.info("Default max scrambles set to " + MAX_SCRAMBLES);
								continue;
							}
						} catch(NumberFormatException e) {
							LOGGER.info("ignored exception", e);}
					}
				} else {
					LOGGER.info("Default max scrambles is " + MAX_SCRAMBLES);
					for(String chan : scrambleMaxMap.keySet())
						LOGGER.info(chan + " = " + scrambleMaxMap.get(chan));
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_COMM_CHANNEL)) {
				if(arg != null) {
					if(arg.startsWith("#")) {
						cctCommChannel = arg;
						LOGGER.info("CCT comm channel set to " + cctCommChannel);
						continue;
					}
				} else {
					if(cctCommChannel != null)
						LOGGER.info("The current cct comm channel is " + cctCommChannel);
					else
						LOGGER.info("The cct comm channel is not set");
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_SERVER)) {
				if(isConnected)
					LOGGER.info("Connected to " + bot.getServer());
				else
					LOGGER.info("Unconnected to " + bot.getServer());
				continue;
			} else if(command.equalsIgnoreCase(CMD_PREFIX)) {
				if(arg != null) {
					if(arg.length() == 1) {
						PREFIX = arg;
						LOGGER.info("Prefix set to " + PREFIX);
						continue;
					}
				} else {
					LOGGER.info("The current prefix is " + PREFIX);
					continue;
				}
			}
			
			String usage = COMMANDS.get(command);
			LOGGER.info(usage == null ? "Unrecognized command: " + command + ". Try help." : "USAGE: " + usage);
		}
	}


	private IRCListener createIrcListener() {
		return new CctBotIrcListener(this, scramblePluginManager);
	}

	public Map<String, Integer> getScrambleMaxMap() {
		return scrambleMaxMap;
	}

	public void setScrambleMaxMap(HashMap<String, Integer> scrambleMaxMap) {
		this.scrambleMaxMap = scrambleMaxMap;
	}

	public boolean isShuttingdown() {
		return shuttingdown;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
}
