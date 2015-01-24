package net.gnehzr.cct.umts.cctbot;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.log4j.Logger;
import org.jibble.pircbot.IrcException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CCTBotTest {

    private static final Logger LOGGER = Logger.getLogger(CCTBotTest.class);

    @Test
    public void testMain() throws IOException, InstantiationException, IllegalAccessException {
        String[] args = new String[0];

        LOGGER.info("CCTBot " + CCTBot.class.getPackage().getImplementationVersion());
        LOGGER.info("Arguments " + Arrays.toString(args));
        LOGGER.info("Running on " + System.getProperty("java.version"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Shutting down");
            }
        });

        Map<String, String> argMap;
        try {
            argMap = parseArguments(args);
        } catch(Exception e) {
            printUsage();
            return;
        }

        URI u = null;
        try {
            if(argMap.containsKey("u"))
                u = new URI(argMap.get("u"));
        } catch(Exception e1) {
            LOGGER.info("unexpected exception", e1);
        }
        if(u == null || u.getHost() == null) {
            LOGGER.info("Invalid URI");
            printUsage();
            return;
        }
        if(u.getFragment() == null) {
            LOGGER.info("No channel specified");
            printUsage();
            return;
        }
        String commChannel = argMap.get("c");
        Integer max = null;
        if(argMap.containsKey("m"))
            try {
                max = Integer.parseInt(argMap.get("m"));
            } catch(NumberFormatException e) {
                printUsage();
                return;
            }

        Configuration configuration = new Configuration(Configuration.getRootDirectory());
        CCTBot cctbot = new CCTBot(new ScramblePluginManager(configuration), configuration);

        if(argMap.containsKey("p"))
            if(argMap.get("p").length() == 1)
                cctbot.PREFIX = argMap.get("p");

        if(commChannel != null)
            cctbot.cctCommChannel = commChannel;
        if(max != null)
            cctbot.MAX_SCRAMBLES = max;
        LOGGER.info("CCTBot name: " + cctbot.bot.getName());
        LOGGER.info("CCTBot nick: " + cctbot.bot.getNick());
        LOGGER.info("CCTBot version: " + cctbot.bot.getVersion());

        LOGGER.info("CCTBot prefix: " + cctbot.PREFIX);
        LOGGER.info("CCTBot comm channel: " + cctbot.cctCommChannel);
        LOGGER.info("CCTBot scramble max: " + cctbot.MAX_SCRAMBLES);
        try {
            LOGGER.info("Connecting to " + u.getHost());
            if(u.getPort() == -1)
                cctbot.bot.connect(u.getHost());
            else {
                LOGGER.info("On port " + u.getPort());
                cctbot.bot.connect(u.getHost(), u.getPort());
            }
            LOGGER.info("Attempting to join #" + u.getFragment());
            cctbot.bot.joinChannel("#" + u.getFragment());
            cctbot.readEvalPrint();
        } catch(IOException | IrcException e) {
            LOGGER.info("unexpected exception", e);
        }
    }


    private static void printUsage() {
        LOGGER.info("USAGE: CCTBot (-c COMMCHANNEL) (-m SCRAMBLEMAX_DEFAULT) (-p PREFIX) -u irc://servername.tld(:port)#channel");
    }

    private static Map<String, String> parseArguments(String[] args) throws Exception {
        Map<String, String> argMap = new HashMap<>();
        for(int c = 0; c < args.length; c += 2) {
            if (args[c].startsWith("-")) {
                argMap.put(args[c].substring(1), args[c + 1]);
            } else {
                throw new Exception();
            }
        }
        return argMap;
    }

}