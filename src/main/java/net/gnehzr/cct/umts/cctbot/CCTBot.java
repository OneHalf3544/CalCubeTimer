package net.gnehzr.cct.umts.cctbot;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.logging.CCTLog;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.umts.IRCListener;
import net.gnehzr.cct.umts.KillablePircBot;
import org.jibble.pircbot.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CCTBot implements IRCListener {

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CCTBot.class);

	int MAX_SCRAMBLES = 12;
	public String PREFIX = "!";
	private HashMap<String, Integer> scrambleMaxMap = new HashMap<String, Integer>();
	String cctCommChannel = null;
	KillablePircBot bot;
	private final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	public CCTBot(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		this.scramblePluginManager = scramblePluginManager;
		this.configuration = configuration;
		bot = getKillableBot();
	}
	private KillablePircBot getKillableBot() {
		String version = CCTBot.class.getPackage().getImplementationVersion();
		KillablePircBot bot = new KillablePircBot(this, "This is cctbot " + version, configuration);
		bot.setlogin("cctbot");
		bot.setname("cctbot");
		bot.setAutoNickChange(true);
		bot.setversion("CCTBot version " + version);
		return bot;
	}
	//max message length: 470 characters
	private static final int MAX_MESSAGE = 470;
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if(message.startsWith(PREFIX)) {
			if(message.substring(1).equalsIgnoreCase("cct")) {
				if(cctCommChannel != null)
					bot.sendMessage(sender, "The current CCT comm channel is " + cctCommChannel);
				else
					bot.sendMessage(sender, "Sorry, I don't know what comm channel people are using with CCT!");
			} else {
				String[] varAndCount = message.substring(1).split("\\*");
				int maxCount = MAX_SCRAMBLES;
				try {
					maxCount = scrambleMaxMap.get(channel);
				} catch(NullPointerException e) {LOG.info("ignored exception", e);}
				int count = 1;
				if(varAndCount.length == 2) {
					try {
						count = Math.min(Integer.parseInt(varAndCount[1]), maxCount);
					} catch(NumberFormatException e) {LOG.info("ignored exception", e);}
				}

				ScrambleVariation sv = scramblePluginManager.getBestMatchVariation(varAndCount[0]);
				if(sv != null) {
					while(count-- > 0) {
						//TODO - add generator support
//						String msg = sv.generateScrambleFromGroup("(0, x) /").toString().trim();
						String msg = sv.generateScramble().toString().trim();
						String prefix = "cct://#" + count + ":" + sv.toString() + ":";
						String fragmentation = "cct://*#" + count + ":" + sv.toString() + ":";
						while(msg.length() > 0) {
							int length = Math.min(msg.length(), MAX_MESSAGE - prefix.length());
							bot.sendMessage(channel, prefix + msg.substring(0, length));
							msg = msg.substring(length);
							prefix = fragmentation; //the asterisk is used to indicate fragmentation of the scramble
						}
					}
				} else
					bot.sendMessage(channel, "Couldn't find scramble variation corresponding to: " + varAndCount[0] + ". " +
							getAvailableVariations());
			}
		}
	}
	
	@Override
	public void log(String line) {
		logger.fine(line);
	}

	private boolean shuttingdown = false;
	private boolean isConnected = false;
	@Override
	public void onDisconnect() {
		isConnected = false;
		final String[] oldChannels = bot.getChannels();
		logger.info("Disconnected from " + bot.getServer());
		while(!isConnected && !shuttingdown) {
			try {
				logger.info("Attempting to reconnect to " + bot.getServer());
				KillablePircBot newBot = getKillableBot();
				newBot.connect(bot.getServer(), bot.getPort());
				bot = newBot;
				for(String c : oldChannels)
					newBot.joinChannel(c);
			} catch(Exception e) {
				LOG.info("unexpected exception", e);
				logger.log(Level.INFO, "Couldn't connect to " + bot.getServer(), e);
				// Couldn't reconnect!
				// Pause for a short while...?
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e1) {LOG.info("ignored exception", e1);}
			}
		}
		LOG.info("Done reconnecting!");
	}
	
	@Override
	public void onConnect() {
		isConnected = true;
		logger.info("Connected to " + bot.getServer());
		logger.info("CCTBot name: " + bot.getName());
		logger.info("CCTBot nick: " + bot.getNick());
		logger.info("CCTBot version: " + bot.getVersion());
	}

	@Override
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
		if(recipientNick.equals(bot.getNick())) {
			logger.info("You have been kicked from " + channel + " by " + kickerNick);
			printPrompt();
		}
	}

	@Override
	public void onPart(String channel, String sender, String login, String hostname) {
		if(sender.equals(bot.getNick())) {
			logger.info("You have parted " + channel);
			printPrompt();
		}
	}

	@Override
	public void onJoin(String channel, String sender, String login, String hostname) {
		if(sender.equals(bot.getNick())) {
			logger.info("You have joined " + channel);
			printPrompt();
		}
	}

	@Override
	public void onNickChange(String oldNick, String login, String hostname, String newNick) {
		if(newNick.equals(bot.getNick())) {
			logger.info("You (formerly: " + oldNick + ") are now known as " + newNick);
			printPrompt();
		}
	}

	private String getAvailableVariations() {
		return "Available variations: " + Arrays.toString(scramblePluginManager.getScrambleVariations());
	}

	static Logger logger = CCTLog.getLogger("net.gnehzr.cct.umts.cctbot.CCTBot");

	private static final HashMap<String, String> commands = new HashMap<String, String>();
	private static final String CMD_RELOAD = "reload";
	{
		commands.put(CMD_RELOAD, "reload\n\tReloads scramble plugins from directory.");
	}
		private static final String CMD_LSVARIATIONS = "variations";
	{
		commands.put(CMD_LSVARIATIONS, "variations\n\tPrints available variations");
	}
	private static final String CMD_CHANNELS = "channels";
	{
		commands.put(CMD_CHANNELS, "channels\n\tPrints channels cctbot is connected to.");
	}
	private static final String CMD_SERVER = "server";
	{
		commands.put(CMD_SERVER, "server\n\tPrints the status of the server cctbot is connected to.");
	}
	private static final String CMD_JOIN = "join";
	{
		commands.put(CMD_JOIN, "join #CHANNEL\n\tAttempts to join the specified channel");
	}
	private static final String CMD_PART = "part";
	{
		commands.put(CMD_PART, "part #CHANNEL (REASON)\n\tLeaves #CHANNEL with an optional REASON");
	}
	private static final String CMD_NICK = "nick";
	{
		commands.put(CMD_NICK, "nick NEWNICK\n\tChanges cctbots nickname.");
	}
	private static final String CMD_QUIT = "quit";
	{
		commands.put(CMD_QUIT, "quit (REASON)\n\tDisconnects from server with optional REASON and shuts down cctbot.");
	}
	private static final String CMD_MAX_SCRAMBLES = "maxscrambles";
	{
		commands.put(CMD_MAX_SCRAMBLES, "maxscrambles (#CHANNEL (COUNT))\n\tSets the maximum number of scrambles cctbot will give at a time on #CHANNEL to COUNT (-1 to remove the entry for #CHANNEL).\n" +
				"\tIf #CHANNEL is not specified, then the default max scrambles for any channel is set to COUNT.\n" +
				"\tIf neither #CHANNEL nor COUNT is specified, you see the max scrambles for each channel.");
	}
	private static final String CMD_COMM_CHANNEL = "commchannel";
	{
		commands.put(CMD_COMM_CHANNEL, "commchannel (#CHANNEL)\n\tSets the comm channel that cctbot will respond with when users type !cct on a channel.\n" +
				"\tIf #CHANNEL is omitted, will display the current comm channel.");
	}
	private static final String CMD_PREFIX = "prefix";
	{
		commands.put(CMD_PREFIX, "prefix (CHAR)\n\tSets the prefix cctbot will respond to to CHAR." +
		"\tIf CHAR is omitted, will display the current prefix. CHAR must be exactly one character long.");
	}
	private static final String CMD_HELP = "help";
	{
		commands.put(CMD_HELP, "help (COMMAND)\n\tPrints available variations");
	}
	
	private void printPrompt() {
		System.out.print("cctbot: ");
		System.out.flush();
	}

	private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
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
					String usage = commands.get(arg);
					LOG.info(usage == null ? "Command " + arg + " not found." : "USAGE: " + usage);
				}
				if(arg == null) {
					StringBuilder cmds = new StringBuilder();
					for(String c : commands.keySet())
						cmds.append(", ").append(c);
					LOG.info("Available commands:\n\t" + cmds.substring(2));
				}
				continue;
			} else if(command.equalsIgnoreCase(CMD_RELOAD)) {
				logger.info("Reloading scramble plugins...");
				scramblePluginManager.clearScramblePlugins();
				logger.info(getAvailableVariations());
				continue;
			} else if(command.equalsIgnoreCase(CMD_CHANNELS)) {
				logger.info("Connected to: " + Arrays.toString(bot.getChannels()));
				continue;
			} else if(command.equalsIgnoreCase(CMD_JOIN)) {
				if(arg != null && arg.startsWith("#")) {
					logger.info("Attempting to join " + arg);
					bot.joinChannel(arg);
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_PART)) {
				if(arg != null && arg.startsWith("#")) {
					String[] chan_reason = arg.split(" +", 2);
					if(chan_reason.length == 2) {
						logger.info("Leaving " + arg + " (" + chan_reason[1] + ")");
						bot.partChannel(chan_reason[0], chan_reason[1]);
					} else {
						logger.info("Leaving " + arg);
						bot.partChannel(chan_reason[0]);
					}
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_NICK)) {
				if(arg != null) {
					logger.info("/nick " + arg);
					bot.changeNick(arg);
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_LSVARIATIONS)) {
				logger.info(getAvailableVariations());
				continue;
			} else if(command.equalsIgnoreCase(CMD_QUIT)) {
				shuttingdown = true;
				if(arg == null) {
					logger.info("Exiting cctbot");
					bot.quitServer();
				} else {
					logger.info("Exiting cctbot (" + arg + ")");
					bot.quitServer(arg);
				}
				System.exit(0);
				continue;
			} else if(command.equalsIgnoreCase(CMD_MAX_SCRAMBLES)) {
				if(arg != null) {
					String[] chan_max = arg.split(" +", 2);
					if(chan_max[0].startsWith("#")) {
						try {
							int max = Integer.parseInt(chan_max[1]);
							if(max > 0) {
								scrambleMaxMap.put(chan_max[0], max);
								logger.info("Max scrambles set to " + max + " for " + chan_max[0]);
								continue;
							} else if(max == -1) {
								scrambleMaxMap.remove(chan_max[0]);
								logger.info("Max scramble info removed for " + chan_max[0]);
								continue;
							}
						} catch(NumberFormatException e) {LOG.info("ignored exception", e);}
					} else {
						try {
							int c = Integer.parseInt(chan_max[0]);
							if(c > 0) {
								MAX_SCRAMBLES = c;
								logger.info("Default max scrambles set to " + MAX_SCRAMBLES);
								continue;
							}
						} catch(NumberFormatException e) {LOG.info("ignored exception", e);}
					}
				} else {
					logger.info("Default max scrambles is " + MAX_SCRAMBLES);
					for(String chan : scrambleMaxMap.keySet())
						logger.info(chan + " = " + scrambleMaxMap.get(chan));
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_COMM_CHANNEL)) {
				if(arg != null) {
					if(arg.startsWith("#")) {
						cctCommChannel = arg;
						logger.info("CCT comm channel set to " + cctCommChannel);
						continue;
					}
				} else {
					if(cctCommChannel != null)
						logger.info("The current cct comm channel is " + cctCommChannel);
					else
						logger.info("The cct comm channel is not set");
					continue;
				}
			} else if(command.equalsIgnoreCase(CMD_SERVER)) {
				if(isConnected)
					logger.info("Connected to " + bot.getServer());
				else
					logger.info("Unconnected to " + bot.getServer());
				continue;
			} else if(command.equalsIgnoreCase(CMD_PREFIX)) {
				if(arg != null) {
					if(arg.length() == 1) {
						PREFIX = arg;
						logger.info("Prefix set to " + PREFIX);
						continue;
					}
				} else {
					logger.info("The current prefix is " + PREFIX);
					continue;
				}
			}
			
			String usage = commands.get(command);
			LOG.info(usage == null ? "Unrecognized command: " + command + ". Try help." : "USAGE: " + usage);
		}
	}

	public void onAction(String sender, String login, String hostname, String target, String action) {}
	public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {}
	public void onPrivateMessage(String sender, String login, String hostname, String message) {}
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {}
	public void onServerResponse(int code, String response) {}
	public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {}
	public void onUserList(String channel, User[] users) {}
}
