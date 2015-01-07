package net.gnehzr.cct.umts.ircclient;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.umts.cctbot.CCTUser;
import org.jibble.pircbot.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatMessageFrame extends MessageFrame {
	private final Configuration configuration;
	private String channel;
	private DraggableJTable usersTable;
	private CCTUserTableModel usersTableModel;
	private CCTCommChannel commChannel;
	private HashMap<String, CCTUser> cctusers;

	public ChatMessageFrame(MinimizableDesktop desk, Configuration configuration, String channel, ScramblePlugin scramblePlugin, Profile profileDao) {
		super(desk, true, null, scramblePlugin, profileDao);
		this.configuration = configuration;
		this.channel = channel;
		setTitle(channel);
		
		cctusers = new HashMap<>();
		
		usersTableModel = new CCTUserTableModel();
		usersTable = new DraggableJTable(this.configuration, false, true);
		usersTable.setAutoCreateRowSorter(true);
		usersTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		usersTable.setModel(usersTableModel);
		usersTable.computePreferredSizes(new SolveTime(60, null, configuration).toString());
		usersTable.setFocusable(false);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, msgScroller, new JScrollPane(usersTable));
		split.setResizeWeight(.8);
		getContentPane().add(split, BorderLayout.CENTER);
	}

	public void clearCCTUsers() {
		cctusers.clear();
	}
	
	public CCTUser getCCTUser(String nick) {
		return cctusers.get(nick);
	}
	
	public void CCTNickChanged(String oldNick, String newNick) {
		CCTUser cct = cctusers.remove(oldNick);
		if(cct == null)
			return;
		cct.setNick(newNick);
		cctusers.put(newNick, cct);
		usersListChanged(); //unfortunately, it's not good enough to just call usersChanged()
	}
	
	public CCTUser addCCTUser(User irc, String nick) {
		CCTUser cct = cctusers.get(nick);
		if(cct != null) {
			if(irc != null)
				cct.setPrefix(irc.getPrefix());
			else
				cct.setPrefix("");
			return cct;
		}
		cct = new CCTUser(configuration, irc, nick);
		cctusers.put(cct.getNick(), cct);
		return cct;
	}
	
	public void removeCCTUser(String nick) {
		cctusers.remove(nick);
	}
	
	public CCTCommChannel getCommChannel() {
		return commChannel;
	}
	
	public void setCommChannel(CCTCommChannel commChannel) {
		this.commChannel = commChannel;
	}

	public User[] getIRCUsers() {
		return usersTableModel.getIRCUsers();
	}

	public void setIRCUsers(User[] users) {
		usersTableModel.setIRCUsers(users);
	}

	//this is for when the list of cct users is modified
	public void usersListChanged() {
		addAutocompleteStrings(getNickList());
		usersTableModel.setCCTUsers(cctusers.values().toArray(new CCTUser[0]));
	}
	
	//this is for when one or more users have had status changes
	public void usersChanged() {
		addAutocompleteStrings(getNickList());
		usersTableModel.fireTableDataChanged();
	}
	
	private ArrayList<String> getNickList() {
		ArrayList<String> nicks = new ArrayList<String>();
		for(User c : getIRCUsers())
			nicks.add(c.getNick().replaceAll("%", "") + ":"); //TODO - bug with pircbot?
		return nicks;
	}

	public void updateStrings() {
		usersTable.refreshColumnNames();
	}
	
	private boolean isConnected = false;

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		usersTable.setEnabled(isConnected);
	}

	public void setTopic(String topic) {
		setTitle(channel + ": " + topic);
	}

	public String getChannel() {
		return channel;
	}
}
