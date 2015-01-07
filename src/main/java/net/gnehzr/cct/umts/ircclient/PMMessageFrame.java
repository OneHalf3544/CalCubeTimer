package net.gnehzr.cct.umts.ircclient;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.statistics.Profile;

public class PMMessageFrame extends MessageFrame {
	private String nick;
	public PMMessageFrame(MinimizableDesktop desk, String nick, ScramblePlugin scramblePlugin, Profile profileDao) {
		super(desk, true, null, scramblePlugin, profileDao);
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
