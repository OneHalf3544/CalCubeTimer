package net.gnehzr.cct.umts.ircclient;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.ActionMap;
import net.gnehzr.cct.main.*;
import net.gnehzr.cct.scrambles.Scramble;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.umts.IRCUtils;
import net.gnehzr.cct.umts.KillablePircBot;
import net.gnehzr.cct.umts.cctbot.CCTUser;
import net.gnehzr.cct.umts.ircclient.MessageFrame.CommandListener;
import org.apache.log4j.Logger;
import org.jibble.pircbot.ReplyConstants;
import org.jibble.pircbot.User;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.sql.Date;
import java.time.Duration;
import java.util.*;
import java.util.List;

@Singleton
public class IRCClientGUI implements CommandListener, DocumentListener, IRCClient {

	private static final Logger LOG = Logger.getLogger(IRCClientGUI.class);

	private static final String VERSION = IRCClientGUI.class.getPackage().getImplementationVersion();
	private static final String FINGER_MSG = "This is the cct/irc client " + VERSION;
	public static final Boolean WATERMARK = false;
	private static final Image IRC_IMAGE = new ImageIcon(IRCClientGUI.class.getResource("cube-irc.png")).getImage();
	public static final Duration SYNC_STATE_TIMEOUT = Duration.ofSeconds(1);
	private final ScramblePluginManager scramblePluginManager;
	private final ScrambleImporter scrambleImporter;

	// TODO - disable ctrl(+shift)+tab for swing components
	// TODO - how to save state of the user tables for each message frame? synchronize them somehow?

	private MinimizableDesktop desk;
	private JInternalFrame login;
	private MessageFrame serverFrame;
	private JFrame clientFrame;
	private JLabel statusBar;
	private KillablePircBot bot;
	private final Configuration configuration;
	private final ProfileDao profileDao;
	private CALCubeTimerFrame calCubeTimer;
	@Inject
	private StatisticsTableModel statsModel;
	@Inject
	private CalCubeTimerModel calCubeTimerModel;


	JTextField nameField;
	JTextField nickField;
	URLHistoryBox server;
	JButton connectButton;
	JLabel nameLabel, nickLabel, serverLabel;


	//This is going to just stay in English

	private static final String CMD_JOIN = "/join";
	private static final String CMD_QUIT = "/quit";
	private static final String CMD_PART = "/part";
	private static final String CMD_CONNECT = "/connect";
	private static final String CMD_MESSAGE = "/msg";
	private static final String CMD_NICK = "/nick";
	private static final String CMD_CLEAR = "/clear";
	private static final String CMD_WHOIS = "/whois";
	private static final String CMD_ME = "/me";
	private static final String CMD_CHANNELS = "/channels";
	private static final String CMD_CCTSTATS = "/cctstats";
	private static final String CMD_HELP = "/help";


	private HashMap<String, PMMessageFrame> pmFrames = new HashMap<>();
	private HashMap<String, ChatMessageFrame> channelFrames = new HashMap<>();
	private HashMap<String, CCTCommChannel> commChannelMap = new HashMap<>();

	private Thread connectThread;

	private CCTUser myself = new CCTUser(null, null, null); // prefix and nick don't matter here
	private final Timer sendStateTimer;


	@Inject
	public IRCClientGUI(ActionMap actionMap, ScramblePluginManager scramblePluginManager,
						Configuration configuration, ProfileDao profileDao, ScrambleImporter scrambleImporter) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.scrambleImporter = scrambleImporter;
		bot = new KillablePircBot(this, FINGER_MSG, configuration);

		login = new JInternalFrame("", false, false, false, true) {
			@Override
			public void setVisible(boolean visible) {
				if(visible) {
					setConnectDefault();
				}
				super.setVisible(visible);
			}
		};
		login.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, IRCClientGUI.WATERMARK);
		login.addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameActivated(InternalFrameEvent e) {
				setConnectDefault();
				loginChanged();
			}
		});
		login.setFrameIcon(null);
		login.setLayer(JLayeredPane.DEFAULT_LAYER + 1);
		login.setContentPane(getLoginPanel());

		desk = new MinimizableDesktop();
		statusBar = new JLabel();
		statusBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		
		JPanel pane = new JPanel(new BorderLayout());
		pane.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, IRCClientGUI.WATERMARK);
		pane.add(desk.getWindowsToolbar(), BorderLayout.PAGE_START);
		pane.add(desk, BorderLayout.CENTER);
		pane.add(statusBar, BorderLayout.PAGE_END);

		clientFrame = new JFrame() {
			@Override
			public void dispose() {
				if(connecting) {
					cancelConnecting();
				}
				if(isConnected()) {
					forkDisconnect();
				}

				actionMap.getActionIfExist(ActionMap.CONNECT_TO_SERVER_ACTION).get().actionPerformed(null);
				super.dispose();
			}
		};
		clientFrame.setIconImage(IRC_IMAGE);
		clientFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		clientFrame.setPreferredSize(new Dimension(500, 450));
		clientFrame.setContentPane(pane);

		serverFrame = new MessageFrame(desk, false, null, this.scramblePluginManager, profileDao.getSelectedProfile());
		serverFrame.addCommandListener(this);
		serverFrame.pack();
		desk.add(serverFrame);

		clientFrame.pack();
		onDisconnect(); // this will add and set login visible & the title of serverFrame
		configuration.addConfigurationChangeListener(this);
		configurationChanged(profileDao.getSelectedProfile());
		updateStrings();
		this.profileDao = profileDao;
		sendStateTimer = new Timer((int)SYNC_STATE_TIMEOUT.toMillis(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncUserStateNOW(calCubeTimer, getMyUserstate());
                broadcastUserstate();
                sendStateTimer.stop();
            }
        });
	}

	public void updateStrings() {
		login.setTitle(StringAccessor.getString("IRCClientGUI.connect"));
		nameLabel.setText(StringAccessor.getString("IRCClientGUI.name"));
		nameField.setText(configuration.getString(VariableKey.IRC_NAME, false));
		nickLabel.setText(StringAccessor.getString("IRCClientGUI.nick"));
		nickField.setText(configuration.getString(VariableKey.IRC_NICK, false));
		serverLabel.setText(StringAccessor.getString("IRCClientGUI.server"));
		
		clientFrame.setTitle(StringAccessor.getString("IRCClientGUI.title") + " " + IRCClientGUI.class.getPackage().getImplementationVersion());
		updateStatusBar();

		pmFrames.values().forEach(PMMessageFrame::updateTitle);
		channelFrames.values().forEach(ChatMessageFrame::updateStrings);
	}

	@Override
	public void configurationChanged(Profile profile) {
		Dimension dimension = configuration.getDimension(VariableKey.IRC_FRAME_DIMENSION, false);
		Point location = configuration.getPoint(VariableKey.IRC_FRAME_LOCATION, false);
		if (location != null && dimension != null) {
			clientFrame.setSize(dimension);
			clientFrame.setLocation(location);
		}
	}
	public void saveToConfiguration() {
		configuration.setDimension(VariableKey.IRC_FRAME_DIMENSION, clientFrame.getSize());
		configuration.setPoint(VariableKey.IRC_FRAME_LOCATION, clientFrame.getLocation());
	}

	private void setConnectDefault() {
		login.getRootPane().setDefaultButton(connectButton); // apparently this setting gets lost when the dialog is removed
	}

	public void setVisible(boolean visible) {
		clientFrame.setVisible(visible);
		if(visible && !bot.isConnected()) {
			try {
				setConnectDefault();
				login.setSelected(true);
			} catch(PropertyVetoException ignored) {}
		}
	}

	private JPanel getLoginPanel() {
		JPanel login = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.fill = GridBagConstraints.BOTH;
		c.ipady = 5;

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		login.add(nameLabel = new JLabel(), c);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		login.add(nameField = new JTextField(20), c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		login.add(nickLabel = new JLabel(), c);
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1;
		login.add(nickField = new JTextField(), c);

		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0;
		login.add(serverLabel = new JLabel(), c);
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 1;
		login.add(server = new URLHistoryBox(VariableKey.IRC_SERVERS, configuration), c);
		
		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 1;
		c.gridwidth = 2;
		login.add(connectButton = new JButton(), c);
		connectButton.addActionListener(e -> {
            Object src = e.getSource();
            if(src == connectButton) {
                if (!connecting) {
                    setConnecting(true);
                    serverFrame.setVisible(true);
                    String url = (String) server.getSelectedItem();

                    forkConnect(url);
                }
            }
        });
		
		nickField.getDocument().addDocumentListener(this);
		nameField.getDocument().addDocumentListener(this);
		loginChanged();
		return login;
	}

	@Override
	public void changedUpdate(DocumentEvent e) {}

	@Override
	public void insertUpdate(DocumentEvent e) {
		loginChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		loginChanged();
	}

	private void loginChanged() {
		connectButton.setEnabled(!nickField.getText().isEmpty() && !nameField.getText().isEmpty());
	}

	private boolean connecting = false;
	private void setConnecting(boolean connecting) {
		this.connecting = connecting;
		connectButton.setText(StringAccessor.getString(connecting ? "IRCClientGUI.cancel" : "IRCClientGUI.connect"));
		nickField.setEnabled(!connecting);
		nameField.setEnabled(!connecting);
		server.setEnabled(!connecting);
	}
	
	private void forkDisconnect() {
		new Thread() {
			@Override
			public void run() {
				bot.disconnect();
			}
		}.start();
	}

	private void cancelConnecting() {
		connectThread.stop();
		setConnecting(false);
		forkDisconnect();
	}

	@Override
	public void windowClosed(MessageFrame src) {
		if(src instanceof PMMessageFrame) {
			pmFrames.remove(((PMMessageFrame) src).getBuddyNick());
		} else if(src instanceof ChatMessageFrame) {
			ChatMessageFrame channelFrame = (ChatMessageFrame) src;
			assert channelFrames.containsKey(channelFrame.getChannel());
			channelFrames.remove(channelFrame.getChannel());
			commChannelMap.remove(channelFrame.getCommChannel().getChannel());
			if(channelFrame.isConnected()) {
				bot.partChannel(channelFrame.getChannel());
				bot.partChannel(channelFrame.getCommChannel().getChannel());
			}
			updateStatusBar();
		}
	}

	@Override
	public void commandEntered(MessageFrame src, String cmd) {
		if(cmd.startsWith("//"))
			cmd = cmd.substring(1);
		else if(cmd.startsWith("/")) {
			String[] commandAndArg = cmd.trim().split(" +", 2);
			String command = commandAndArg[0];
			String arg = commandAndArg.length == 2 ? commandAndArg[1] : null;
			if(command.equalsIgnoreCase(CMD_HELP)) {
				if(arg != null) {
					if(!arg.startsWith("/"))
						arg = "/" + arg;
					String usage = cmdHelp.get(arg);
					src.appendInformation(usage == null ? "Command " + arg + " not found." : "USAGE: " + usage);
				}
				if(arg == null) {
					String cmds = "";
					for(String c : new TreeSet<String>(cmdHelp.keySet()))
						cmds += ", " + c;
					cmds = cmds.substring(2);
					src.appendInformation("Available commands:\n" + cmds);
				}
				return;
			} else if(command.equalsIgnoreCase(CMD_JOIN)) {
				if(arg != null) {
					if(!arg.startsWith("#"))
						arg = "#" + arg;
					bot.joinChannel(arg);
					return;
				} else if(src instanceof ChatMessageFrame) {
					bot.joinChannel(((ChatMessageFrame) src).getChannel());
					return;
				}
			} else if(command.equalsIgnoreCase(CMD_QUIT)) {
				if(arg == null)
					bot.quitServer();
				else
					bot.quitServer(arg);
				return;
			} else if(command.equalsIgnoreCase(CMD_CONNECT)) {
				if(arg != null)
					server.setSelectedItem(arg);
				if(bot.isConnected())
					bot.quitServer();
				return;
			} else if(command.equalsIgnoreCase(CMD_MESSAGE)) {
				if(arg != null) {
					String[] userMsg = arg.split(" +", 2);
					if(userMsg.length == 2) {
						src.appendInformation("\"" + userMsg[1] + "\" -> " + userMsg[0]);
						privateMessage(userMsg[0], userMsg[1]);
						return;
					}
				}
			} else if(command.equalsIgnoreCase(CMD_PART)) {
				String channel = null, reason = null;
				if(arg != null && arg.startsWith("#")) {
					String[] chan_reason = arg.split(" +", 2);
					if(chan_reason.length == 2) {
						channel = chan_reason[0];
						reason = chan_reason[1];
					} else { //length == 1
						channel = chan_reason[0];
					}
				}
				if(channel == null && src instanceof ChatMessageFrame) {
					channel = ((ChatMessageFrame) src).getChannel();
					reason = arg;
				}
				if(channel != null) {
					if(reason == null)
						bot.partChannel(channel);
					else
						bot.partChannel(channel, reason);
					return;
				}
			} else if(command.equalsIgnoreCase(CMD_NICK)) {
				if(arg != null) {
					bot.changeNick(arg);
					return;
				}
			} else if(command.equalsIgnoreCase(CMD_CLEAR)) {
				src.resetMessagePane();
				return;
			} else if(command.equalsIgnoreCase(CMD_WHOIS)) {
				if(arg != null) {
					bot.sendRawLineViaQueue("WHOIS " + arg);
					return;
				}
			} else if(command.equalsIgnoreCase(CMD_ME)) {
				if(arg != null) {
					if(src instanceof ChatMessageFrame) {
						bot.sendAction(((ChatMessageFrame) src).getChannel(), arg);
						_onAction(bot.getNick(), arg);
						return;
					} else if(src instanceof PMMessageFrame) {
						bot.sendAction(((PMMessageFrame) src).getBuddyNick(), arg);
						_onAction(bot.getNick(), arg);
						return;
					}
				}
			} else if(command.equalsIgnoreCase(CMD_CCTSTATS)) {
				if(arg != null && arg.startsWith("#") && src instanceof ChatMessageFrame) {
					if(!IRCUtils.isConnectedToChannel(bot, arg)) {
						setCommChannel((ChatMessageFrame) src, arg);
						return;
					}
					src.appendInformation("You are already connected to: " + arg + ". Please specify a channel you aren't connected to.");
				}
			} else if(command.equalsIgnoreCase(CMD_CHANNELS)) {
				src.appendInformation("Connected to: " + Arrays.toString(bot.getChannels()));
				return;
			}
			String usage = cmdHelp.get(command);
			src.appendInformation(usage == null ? "Unrecognized command: " + command : "USAGE: " + usage);
			return;
		}
		if(src instanceof PMMessageFrame) {
			privateMessage(((PMMessageFrame) src).getBuddyNick(), cmd);
		} else if(src instanceof ChatMessageFrame && ((ChatMessageFrame) src).isConnected()) {
			String channel = ((ChatMessageFrame) src).getChannel();
			messageReceived(bot.getNick(), cmd, channel);
			bot.sendMessage(channel, cmd);
		} else {
			src.appendError(StringAccessor.getString("IRCClientGUI.connecttochannel"));
		}
	}

	public static final Map<String, String> cmdHelp = ImmutableMap.<String, String>builder()
			.put(CMD_JOIN, "/join (#CHANNEL)" + "\nJoins #CHANNEL (optional if you're typing " +
					"the command from the channel you wish to join). The # sign is optional.")
			.put(CMD_QUIT, "/quit (REASON)")
			.put(CMD_CONNECT, "/server (SERVER)")
			.put(CMD_MESSAGE, "/msg NICK MESSAGE")
			.put(CMD_PART, "/part (#CHANNEL) (REASON)" + "\nLeaves #CHANNEL (optional if you're typing " +
				"the command from the channel you wish to part). The # sign is required. You may also " +
				"specify a REASON for people to see when you leave." )
			.put(CMD_NICK, "/nick NEWNICK")
			.put(CMD_CLEAR, "/clear")
			.put(CMD_WHOIS, "/whois NICK")
			.put(CMD_ME, "/me ACTION")
			.put(CMD_CHANNELS, "/channels")
			.put(CMD_CCTSTATS, "/cctstats #COMMCHANNEL" + "\n" + "Sets the channel used for communication of CCT status between CCT users. "
					+ "Only use this if the default channel isn't working, perhaps because everyone else is connected to a different channel. "
					+ "You must type this command from a channel which you want to display everyone's CCT status.")
			.put(CMD_HELP, "/help (COMMAND)")
			.build();

	@Override
	public void scramblesImported(final MessageFrame src, ScrambleVariation sv, List<Scramble> scrambles, Profile profile) {
		scrambleImporter.importScrambles(sv, scrambles, profile, /*todo scramblesList*/null);
		// this is to keep the scramble frame from stealing focus
		SwingUtilities.invokeLater(clientFrame::toFront);
	}

	private void messageReceived(final String nick, final String msg, final String channel) {
		SwingUtilities.invokeLater(() -> {
            if(channelFrames.containsKey(channel))
                channelFrames.get(channel).appendMessage(nick, msg);
            else if(commChannelMap.containsKey(channel))
                cctStatusUpdate(nick, msg, commChannelMap.get(channel));
            else
                assert false : channel; //channel must be either a chat or comm channel!
        });
	}

	private void privateMessage(String nick, String msg) {
		MessageFrame f = getPMFrame(nick);
		f.appendMessage(bot.getNick(), msg);
		bot.sendMessage(nick, msg);
	}

	@Override
	public void onPrivateMessage(final String sender, String login, String hostname, final String message) {
		SwingUtilities.invokeLater(() -> {
            JInternalFrame old = desk.getSelectedFrame();
            MessageFrame f = getPMFrame(sender);
            desk.add(f);
            f.setVisible(true);
            if(old != null) {
                try {
                    old.setSelected(true);
                } catch(PropertyVetoException e) {LOG.info("ignored exception", e);}
            }
            f.appendMessage(sender, message);
        });
	}
	
	private PMMessageFrame getPMFrame(String nick) {
		PMMessageFrame f = pmFrames.get(nick);
		if(f == null) {
			f = new PMMessageFrame(desk, nick, scramblePluginManager, profileDao.getSelectedProfile());
			f.addCommandListener(this);
			pmFrames.put(nick, f);
		}
		return f;
	}

	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		messageReceived(sender, message, channel);
	}

	@Override
	public void onAction(final String sender, String login, String hostname, String target, final String action) {
		SwingUtilities.invokeLater(() -> _onAction(sender, action));
	}
	//must be invoked from EDT!
	private void _onAction(String sender, String action) {
		for(ChatMessageFrame f : channelFrames.values()) {
			f.appendInformation("* " + sender + " " + action);
			f.setIRCUsers(bot.getUsers(f.getChannel()));
		}
		for(PMMessageFrame f : pmFrames.values()) {
			if (f.getBuddyNick().equals(sender)) {
				f.appendInformation("* " + sender + " " + action);
			}
		}
	}
	
	@Override
	public void onJoin(final String channel, final String sender, String login, String hostname) {
		SwingUtilities.invokeLater(() -> {
            boolean weJoined = sender.equals(bot.getNick());
            if(commChannelMap.containsKey(channel)) {
                CCTCommChannel c = commChannelMap.get(channel);
                sendUserstate(c, IRCUtils.createMessage(IRCUtils.CLIENT_USERSTATE, myself.getUserState())); //whenever we or another user connects to a comm channel, we send our userstate immediately
                c.getChatFrame().addCCTUser(getUser(c.getChatFrame().getChannel(), sender), sender);
                c.getChatFrame().usersListChanged();
            } else { //we or someone else joined a chat channel
                ChatMessageFrame f = channelFrames.get(channel);
                if(weJoined) {
                    String commChannel;
                    if(f == null) {
                        f = new ChatMessageFrame(desk, configuration, channel, scramblePluginManager, profileDao.getSelectedProfile());
                        channelFrames.put(channel, f);

                        desk.add(f);
                        f.addCommandListener(IRCClientGUI.this);
                        f.setLocation(20, 20); // this may help make it easier to see the new window
                        f.setVisible(true);
                        commChannel = channel + "-cct";
                    } else {
                        commChannel = f.getCommChannel().getChannel();
                        f.setCommChannel(null); //make this guy look like a new frame, so setcommchannel will work
                    }
                    f.appendInformation(StringAccessor.getString("IRCClientGUI.connected") + ": " + channel);
                    try {
                        f.setSelected(true);
                    } catch(PropertyVetoException e) {LOG.info("ignored exception", e);}
                    setCommChannel(f, commChannel);
                    f.setConnected(true);
                }
                f.appendInformation(StringAccessor.format("IRCClientGUI.joined", sender, channel));
                f.setIRCUsers(bot.getUsers(channel));
            }
            if(weJoined)
                updateStatusBar();
        });
	}

	@Override
	public void onUserList(final String channel, final User[] users) {
		SwingUtilities.invokeLater(() -> {
            if(channelFrames.containsKey(channel))
                channelFrames.get(channel).setIRCUsers(users);
            else if(commChannelMap.containsKey(channel)) {
                ChatMessageFrame c = commChannelMap.get(channel).getChatFrame();
                User[] ircusers = bot.getUsers(c.getChannel());
                for(User cctuser : bot.getUsers(channel)) {
                    CCTUser u = c.addCCTUser(null, cctuser.getNick());
                    for(User ircuser : ircusers)
                        if(ircuser.equals(cctuser))
                            u.setPrefix(ircuser.getPrefix());
                }
                c.usersListChanged();
            } else
                assert false : channel;
        });
	}

	@Override
	public void onQuit(final String sourceNick, String sourceLogin, String sourceHostname, final String reason) {
		SwingUtilities.invokeLater(() -> {
            for(String channel : bot.getChannels()) {
                if(channelFrames.containsKey(channel)) {
                    ChatMessageFrame f = channelFrames.get(channel);
                    for(User u : f.getIRCUsers()) {
                        if(u.getNick().equals(sourceNick)) {
                            f.setIRCUsers(bot.getUsers(channel));
                            f.appendInformation(StringAccessor.format("IRCClientGUI.quit", sourceNick, reason));
                            generalizedLeftChannel(channel, sourceNick, null);
                            break;
                        }
                    }
                } else if(commChannelMap.containsKey(channel)) {
                    CCTCommChannel chatChannel = commChannelMap.get(channel);
                    chatChannel.getChatFrame().removeCCTUser(sourceNick);
                    chatChannel.getChatFrame().usersListChanged();
                } else
                    assert false : channel; //all channels are chat channels or comm channels
            }
        });
	}

	@Override
	public void onPart(String channel, String sender, String login, String hostname) {
		generalizedLeftChannel(channel, sender, null);
	}

	@Override
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
		generalizedLeftChannel(channel, recipientNick, kickerNick);
	}

	private void generalizedLeftChannel(final String channel, final String user, final String kicker) {
		SwingUtilities.invokeLater(() -> {
            boolean iLeft = user.equals(bot.getNick());
            if(channelFrames.containsKey(channel)) {
                ChatMessageFrame f = channelFrames.get(channel);
                if(iLeft) {
                    f.appendInformation(StringAccessor.format(kicker != null ? "IRCClientGUI.youkicked" : "IRCClientGUI.youleft", channel, kicker));
                    f.setConnected(false);

                    if(f.isClosed())
                        channelFrames.remove(f.getChannel());

                    //since we left this chat channel, we should attempt to leave the associated comm channel
                    bot.partChannel(f.getCommChannel().getChannel(), StringAccessor.format("IRCClientGUI.alsoleft", channel));
                } else {
                    f.appendInformation(StringAccessor.format(kicker != null ? "IRCClientGUI.someonekicked" : "IRCClientGUI.someoneleft", channel, user,
                            kicker));
                    f.setIRCUsers(bot.getUsers(channel));
                }
            } else if(commChannelMap.containsKey(channel)) {
                CCTCommChannel c = commChannelMap.get(channel);
                if(c.getChatFrame().isConnected()) {
                    if(iLeft) {
                        verifyCommChannels.start();
                        updateStatusBar();
                    }
                } else
                    commChannelMap.remove(c.getChannel()); //we don't want to attempt to reconnect when onPart() is called

                c.getChatFrame().removeCCTUser(user);
                c.getChatFrame().usersListChanged();
            } else {} //this must be a comm channel that we intentionally left via /cctstats, or a chat channel we x-ed close

            if(iLeft)
                updateStatusBar();
        });
	}

	@Override
	public void onNickChange(final String oldNick, String login, String hostname, final String newNick) {
		SwingUtilities.invokeLater(() -> {
            String msg = StringAccessor.format("IRCClientGUI.nickchange", oldNick, newNick);
            for(ChatMessageFrame f : channelFrames.values()) {
                if(getUser(f.getChannel(), newNick) != null) { //only update that channel if the user was in it
                    f.appendInformation(msg);
                    f.setIRCUsers(bot.getUsers(f.getChannel()));
                }
            }
            for(CCTCommChannel commChan : commChannelMap.values())
                commChan.getChatFrame().CCTNickChanged(oldNick, newNick);

            if(newNick.equals(bot.getNick()))
                serverFrame.appendInformation(msg);
        });
	}
	
	@Override
	public boolean isConnected() {
		return bot.isConnected();
	}
	
	@Override
	public void onConnect() {
		SwingUtilities.invokeLater(() -> {
            server.commitCurrentItem(); //current server was successful, so remember it
            //if we hadn't set a nick and namme before, we'll do so now
            String c;
            if((c = configuration.getString(VariableKey.IRC_NAME, false)) == null || c.isEmpty())
                configuration.setString(VariableKey.IRC_NAME, nameField.getText());
            if((c = configuration.getString(VariableKey.IRC_NICK, false)) == null || c.isEmpty())
                configuration.setString(VariableKey.IRC_NICK, nickField.getText());

            login.setVisible(false);
            desk.remove(login);
            serverFrame.setTitle(StringAccessor.getString("IRCClientGUI.connected") + ": " + bot.getInetAddress().getHostName() + ":" + bot.getPort());

            updateStatusBar();
        });
	}

	@Override
	public void onTopic(final String channel, final String topic, final String setBy, final long date, boolean changed) {
		SwingUtilities.invokeLater(() -> {
            if(channelFrames.containsKey(channel))
                channelFrames.get(channel).setTopic(topic + " - " + setBy + " (" + new Date(date) + ")");
        });
	}

	@Override
	public void onDisconnect() {
		SwingUtilities.invokeLater(() -> {
            for(ChatMessageFrame f : channelFrames.values()) {
                f.setConnected(false);
                f.appendInformation(StringAccessor.getString("IRCClientGUI.disconnected"));
            }
            serverFrame.setTitle(StringAccessor.getString("IRCClientGUI.disconnected"));
            setConnecting(false);
            updateStatusBar();

            if(!login.isVisible()) { //the login may be visible if the cancel button was clicked while trying to connect to a server
                login.setVisible(true);
                desk.add(login);
                login.pack();
                // center login frame
                login.setLocation((desk.getWidth() - login.getWidth()) / 2, (desk.getHeight() - login.getHeight()) / 2);
                try {
                    login.setSelected(true);
                } catch(PropertyVetoException e) {LOG.info("ignored exception", e);}
            }
        });
	}

	@Override
	public void log(String line) {
//		serverFrame.appendInformation(line);
	}

	private void forkConnect(String hostname) {
		final String[] urlAndPort = hostname.split(":");
		int port = -1;
		try {
			port = Integer.parseInt(urlAndPort[1]);
		} catch(Exception e) {
			LOG.info("ignored exception", e);
		}
		final int PORT = port;
		serverFrame.setTitle(StringAccessor.getString("IRCClientGUI.connecting") + ": " + urlAndPort[0] + ":" + port);
		connectThread = new Thread() {
			@Override
			public void run() {
				try {
					if(bot.isConnected()) {
						bot.disconnect();
						try {
							bot.dispose();
						} catch(NullPointerException e) {LOG.info("ignored exception", e);}
						bot = new KillablePircBot(IRCClientGUI.this, FINGER_MSG, configuration);
					}
					bot.setAutoNickChange(true);
					bot.setlogin(nameField.getText());
					bot.setname(nickField.getText());
					bot.setversion(StringAccessor.getString("IRCClientGUI.title") + " " + VERSION);
					
					if(PORT != -1)
						bot.connect(urlAndPort[0], PORT);
					else
						bot.connect(urlAndPort[0]);
					connecting = false;
				} catch(final Exception e) {
					LOG.info("unexpected exception", e);
					if(bot.isConnected())
						bot.disconnect();
					else
						onDisconnect();
					SwingUtilities.invokeLater(() -> serverFrame.appendError(e.toString()));
				}
			}
		};
		connectThread.start();
	}

	/*** What follows is code to connect to a secondary channel for CCT status messages ***/

	@Override
	public void onServerResponse(int code, String response) {
		serverFrame.appendInformation(code + " " + response);
		if(code == ReplyConstants.ERR_CANNOTSENDTOCHAN) {
			final String[] nick_chan_msg = response.split(" ", 3);
			SwingUtilities.invokeLater(() -> {
                if(channelFrames.containsKey(nick_chan_msg[1])) {
                    ChatMessageFrame c = channelFrames.get(nick_chan_msg[1]);
                    c.appendError(nick_chan_msg[2]);
                } else if(commChannelMap.containsKey(nick_chan_msg[1])) {
                    // if we've been silenced on a comm channel,
                    // we'll have to try to connect to a different channel
                    bot.partChannel(nick_chan_msg[1]);
                } else {
                    //this must be a channel we parted from before our outgoing queue emptied
                }
            });
		}
	}
	
	//if newCommChannel == null, we'll just add an attempt and try again
	//otherwise, we'll use newCommChannel as the new comm channel
	private void setCommChannel(ChatMessageFrame chatChannel, String newCommChannel) {
		CCTCommChannel commChannel = chatChannel.getCommChannel();
		if(commChannel != null) {
			assert commChannelMap.containsKey(commChannel.getChannel()) : commChannel.getChannel();
			commChannelMap.remove(commChannel.getChannel());
			
			if(IRCUtils.isConnectedToChannel(bot, commChannel.getChannel()))
				bot.partChannel(commChannel.getChannel());
			
			if(newCommChannel != null)
				commChannel.setCommChannel(newCommChannel);
			else
				commChannel.addAttempt();
		} else {
			assert newCommChannel != null;
			commChannel = new CCTCommChannel(newCommChannel, chatChannel);
			chatChannel.setCommChannel(commChannel);
		}
		
		//we don't want to turn a channel we're already connected to into a comm channel
		while(IRCUtils.isConnectedToChannel(bot, commChannel.getChannel()))
			commChannel.addAttempt();
		
		commChannelMap.put(commChannel.getChannel(), commChannel);
		chatChannel.clearCCTUsers();
		bot.joinChannel(commChannel.getChannel());
		verifyCommChannels.start();
	}
	
	// TODO - what if attempt to connect to channel takes a while, and the timer is fired, or the user overrides it with /cctstats?
	// TODO - if the server lag is > 10 seconds, we'll connect to many, many channels
	// this timer will check every 10 seconds for unconnected comm channels
	private Timer verifyCommChannels = new Timer(10000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean alliswell = true;
			for(ChatMessageFrame c : channelFrames.values()) {
				if(c.isConnected()) {
					CCTCommChannel commChannel = c.getCommChannel();
					if(!IRCUtils.isConnectedToChannel(bot, commChannel.getChannel())) {
						alliswell = false;
						setCommChannel(c, null);
					}
				}
			}
			if(alliswell) {
				verifyCommChannels.stop();
			}
		}
	});

	private void cctStatusUpdate(String nick, String msg, CCTCommChannel commChannel) {
		String[] type_msg = IRCUtils.splitMessage(msg);
		if(type_msg[0].equals(IRCUtils.CLIENT_USERSTATE)) {
			ChatMessageFrame chatChannel = commChannel.getChatFrame();
			CCTUser c = chatChannel.getCCTUser(nick);
			if(c == null)
				c = chatChannel.addCCTUser(getUser(chatChannel.getChannel(), nick), nick);

			c.setUserState(type_msg[1]);
			chatChannel.usersListChanged();
		}
	}

	private void updateCCTUserModes(ChatMessageFrame c) {
		for(User u : bot.getUsers(c.getChannel())) {
			CCTUser cct = c.getCCTUser(u.getNick());
			if(cct != null)
				cct.setPrefix(u.getPrefix());
		}
		c.usersChanged();
	}

	@Override
	public void onMode(final String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		SwingUtilities.invokeLater(() -> {
            if(channelFrames.containsKey(channel)) {
                ChatMessageFrame c = channelFrames.get(channel);
                c.setIRCUsers(bot.getUsers(channel));
                updateCCTUserModes(c);
            } else if(commChannelMap.containsKey(channel)) {
                updateCCTUserModes(commChannelMap.get(channel).getChatFrame());
            } else
                assert false : channel;
        });
	}

	private User getUser(String channel, String user) {
		for(User u : bot.getUsers(channel))
			if(u.equals(user))
				return u;
		return null;
	}
	
	private String getHTMLForChannel(String channel) {
		if(IRCUtils.isConnectedToChannel(bot, channel))
			return channel;
		
		return "<strike>" + channel + "</strike>";
	}
	private void updateStatusBar() {
		String text;
		if(!bot.isConnected())
			text = StringAccessor.getString("IRCClientGUI.disconnected");
		else {
			StringBuilder status = new StringBuilder();
			for(ChatMessageFrame chatChannel : channelFrames.values())
				status.append(", ").append(getHTMLForChannel(chatChannel.getChannel())).append("->").append(getHTMLForChannel(chatChannel.getCommChannel().getChannel()));
			if(status.length() == 0)
				text = StringAccessor.getString("IRCClientGUI.nochannels");
			else
				text = status.substring(2);
		}
		statusBar.setText("<html>" + StringAccessor.getString("IRCClientGUI.status") + ": " + text + "</html>");
	}

	@Override
	public CCTUser getMyUserstate() {
		return myself;
	}
	
	private void sendUserstate(CCTCommChannel c, String msg) {
		//it's not that big a deal if we're not connected to c yet,
		//we can try to send the userstate anyways
		bot.sendMessage(c.getChannel(), msg);
		cctStatusUpdate(bot.getNick(), msg, c);
	}

	@Override
	public void broadcastUserstate() {
		String msg = IRCUtils.createMessage(IRCUtils.CLIENT_USERSTATE, myself.getUserState());
		for(CCTCommChannel c : commChannelMap.values()) {
			sendUserstate(c, msg);
		}
	}

	//this will start a timer to transmit the cct state every one second, if there's new information
	@Override
	public void sendUserstate() {
		if(!isConnected()) {
			sendStateTimer.stop();
			return;
		}
		if(!sendStateTimer.isRunning()) {
			sendStateTimer.start();
		}
	}


	//this will sync the cct state with the client, but will not transmit the data to other users
	void syncUserStateNOW(CALCubeTimerFrame calCubeTimerFrame, CCTUser myself) {
		myself.setCustomization(calCubeTimerFrame.getScrambleCustomizationComboBox().getSelectedItem().toString());

		myself.setLatestTime(statsModel.getCurrentStatistics().get(-1));

		TimerState state = calCubeTimerFrame.getTimeLabel().getTimerState();
		if(!calCubeTimerModel.isTiming()) {
			state = null;
		}
		myself.setTimingState(calCubeTimerModel.isInspecting(), state);

		Statistics stats = statsModel.getCurrentStatistics();
		myself.setCurrentRA(stats.average(Statistics.AverageType.CURRENT, 0), stats.toTerseString(Statistics.AverageType.CURRENT, 0, true));
		myself.setBestRA(stats.average(Statistics.AverageType.RA, 0), stats.toTerseString(Statistics.AverageType.RA, 0, false));
		myself.setSessionAverage(new SolveTime(stats.getSessionAvg(), null, configuration));

		myself.setSolvesAttempts(stats.getSolveCount(), stats.getAttemptCount());

		myself.setRASize(stats.getRASize(0));
	}

	public void setCalCubeTimer(CALCubeTimerFrame calCubeTimer) {
		this.calCubeTimer = calCubeTimer;
	}
}
