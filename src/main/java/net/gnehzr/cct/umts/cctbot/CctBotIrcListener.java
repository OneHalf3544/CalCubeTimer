package net.gnehzr.cct.umts.cctbot;

import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.umts.IRCListener;
import net.gnehzr.cct.umts.KillablePircBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jibble.pircbot.User;

/**
 * <p>
 * <p>
 * Created: 31.01.2015 11:41
 * <p>
 *
 * @author OneHalf
 */
class CctBotIrcListener implements IRCListener {

    private static final Logger LOGGER = LogManager.getLogger(CctBotIrcListener.class);

    private final CCTBot cctBot;
    private final ScramblePluginManager scramblePluginManager;

    public CctBotIrcListener(CCTBot cctBot, ScramblePluginManager scramblePluginManager) {
        this.cctBot = cctBot;
        this.scramblePluginManager = scramblePluginManager;
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.startsWith(cctBot.PREFIX)) {
            if (message.substring(1).equalsIgnoreCase("cct")) {
                if (cctBot.cctCommChannel != null)
                    cctBot.bot.sendMessage(sender, "The current CCT comm channel is " + cctBot.cctCommChannel);
                else
                    cctBot.bot.sendMessage(sender, "Sorry, I don't know what comm channel people are using with CCT!");
            } else {
                String[] varAndCount = message.substring(1).split("\\*");
                int maxCount = cctBot.MAX_SCRAMBLES;
                try {
                    maxCount = cctBot.getScrambleMaxMap().get(channel);
                } catch (NullPointerException e) {
                    LOGGER.info("ignored exception", e);
                }
                int count = 1;
                if (varAndCount.length == 2) {
                    try {
                        count = Math.min(Integer.parseInt(varAndCount[1]), maxCount);
                    } catch (NumberFormatException e) {
                        LOGGER.info("ignored exception", e);
                    }
                }

                ScrambleVariation sv = scramblePluginManager.getBestMatchVariation(varAndCount[0]);
                if (sv != null) {
                    while (count-- > 0) {
//TODO - add generator support
//						String msg = sv.generateScrambleFromGroup("(0, x) /").toString().trim();
                        String msg = sv.generateScramble().toString().trim();
                        String prefix = "cct://#" + count + ":" + sv.toString() + ":";
                        String fragmentation = "cct://*#" + count + ":" + sv.toString() + ":";
                        while (msg.length() > 0) {
                            int length = Math.min(msg.length(), CCTBot.MAX_MESSAGE - prefix.length());
                            cctBot.bot.sendMessage(channel, prefix + msg.substring(0, length));
                            msg = msg.substring(length);
                            prefix = fragmentation; //the asterisk is used to indicate fragmentation of the scramble
                        }
                    }
                } else
                    cctBot.bot.sendMessage(channel, "Couldn't find scramble variation corresponding to: " + varAndCount[0] + ". " +
                            cctBot.getAvailableVariations());
            }
        }
    }

    @Override
    public void log(String line) {
        LOGGER.trace(line);
    }

    @Override
    public void onDisconnect() {
        cctBot.setConnected(false);
        final String[] oldChannels = cctBot.bot.getChannels();
        LOGGER.info("Disconnected from " + cctBot.bot.getServer());
        while (!cctBot.isConnected() && !cctBot.isShuttingdown()) {
            try {
                LOGGER.info("Attempting to reconnect to " + cctBot.bot.getServer());
                KillablePircBot newBot = cctBot.getKillableBot();
                newBot.connect(cctBot.bot.getServer(), cctBot.bot.getPort());
                cctBot.bot = newBot;
                for (String c : oldChannels)
                    newBot.joinChannel(c);
            } catch (Exception e) {
                LOGGER.info("Couldn't connect to " + cctBot.bot.getServer(), e);
// Couldn't reconnect!
// Pause for a short while...?
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    LOGGER.info("ignored exception", e1);
                }
            }
        }
        LOGGER.info("Done reconnecting!");
    }

    @Override
    public void onConnect() {
        cctBot.setConnected(true);
        LOGGER.info("Connected to " + cctBot.bot.getServer());
        LOGGER.info("CCTBot name: " + cctBot.bot.getName());
        LOGGER.info("CCTBot nick: " + cctBot.bot.getNick());
        LOGGER.info("CCTBot version: " + cctBot.bot.getVersion());
    }

    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equals(cctBot.bot.getNick())) {
            LOGGER.info("You have been kicked from " + channel + " by " + kickerNick);
            cctBot.printPrompt();
        }
    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname) {
        if (sender.equals(cctBot.bot.getNick())) {
            LOGGER.info("You have parted " + channel);
            cctBot.printPrompt();
        }
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
        if (sender.equals(cctBot.bot.getNick())) {
            LOGGER.info("You have joined " + channel);
            cctBot.printPrompt();
        }
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
        if (newNick.equals(cctBot.bot.getNick())) {
            LOGGER.info("You (formerly: " + oldNick + ") are now known as " + newNick);
            cctBot.printPrompt();
        }
    }

    @Override
    public void onAction(String sender, String login, String hostname, String target, String action) {
    }

    @Override
    public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    }

    @Override
    public void onServerResponse(int code, String response) {
    }

    @Override
    public void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
    }

    @Override
    public void onUserList(String channel, User[] users) {
    }
}
