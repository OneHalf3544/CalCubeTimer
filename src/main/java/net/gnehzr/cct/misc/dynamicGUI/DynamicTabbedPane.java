package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DynamicTabbedPane extends JTabbedPane implements DynamicStringSettable {

	private final Configuration configuration;

	private List<DynamicString> tabNames = new ArrayList<>();
	private final XMLGuiMessages xmlGuiMessages;
	private final SessionsList sessionList;

	public DynamicTabbedPane(Configuration configuration, XMLGuiMessages xmlGuiMessages, SessionsList sessionList) {
		this.configuration = configuration;
		this.xmlGuiMessages = xmlGuiMessages;
		this.sessionList = sessionList;
	}
	
	@Override
	public void addTab(String title, Component component) {
		DynamicString s = new DynamicString(title, xmlGuiMessages, configuration);
		tabNames.add(s);
		super.addTab(s.toString(sessionList), component);
	}
	
	@Override
	public void removeTabAt(int index) {
		tabNames.remove(index);
		super.removeTabAt(index);
	}

	@Override
	public void setDynamicString(DynamicString s) {
		// todo
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTextFromDynamicString(Configuration configuration, SessionsList sessionsList) {
		for(int tabIndex = 0; tabIndex < tabNames.size(); tabIndex++) {
			setTitleAt(tabIndex, tabNames.get(tabIndex).toString(sessionsList));
		}
	}
}
