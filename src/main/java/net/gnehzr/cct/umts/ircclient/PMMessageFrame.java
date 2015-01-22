package net.gnehzr.cct.umts.ircclient;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;

public class PMMessageFrame extends MessageFrame {
	private String nick;
	public PMMessageFrame(MinimizableDesktop desk, String nick, ScramblePluginManager scramblePluginManager, Profile profileDao) {
		super(desk, true, null, scramblePluginManager, profileDao);
		this.nick = nick;
		updateTitle();
	}

	public void updateTitle() {
		setTitle(StringAccessor.getString("IRCClientGUI.pm") + ": " + nick);
	}

	public String getBuddyNick() {
		return nick;
	}
}
