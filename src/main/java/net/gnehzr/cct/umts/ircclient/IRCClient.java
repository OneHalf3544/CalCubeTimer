package net.gnehzr.cct.umts.ircclient;

import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.umts.IRCListener;
import net.gnehzr.cct.umts.cctbot.CCTUser;
import org.jibble.pircbot.User;

/**
 * <p>
 * <p>
 * Created: 22.01.2015 1:54
 * <p>
 *
 * @author OneHalf
 */
public interface IRCClient extends ConfigurationChangeListener, IRCListener {
	@Override
	void onPrivateMessage(String sender, String login, String hostname, String message);

	@Override
	void onMessage(String channel, String sender, String login, String hostname, String message);

	@Override
	void onAction(String sender, String login, String hostname, String target, String action);

	@Override
	void onJoin(String channel, String sender, String login, String hostname);

	@Override
	void onUserList(String channel, User[] users);

	@Override
	void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason);

	@Override
	void onPart(String channel, String sender, String login, String hostname);

	@Override
	void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason);

	@Override
	void onNickChange(String oldNick, String login, String hostname, String newNick);

	boolean isConnected();

	@Override
	void onConnect();

	@Override
	void onTopic(String channel, String topic, String setBy, long date, boolean changed);

	@Override
	void onDisconnect();

	@Override
	void log(String line);

	@Override
	void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode);

	CCTUser getMyUserstate();

	void broadcastUserstate();

	//this will start a timer to transmit the cct state every one second, if there's new information
	void sendUserstate();
}
