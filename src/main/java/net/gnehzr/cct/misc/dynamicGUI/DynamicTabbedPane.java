package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.StatisticsTableModel;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DynamicTabbedPane extends JTabbedPane implements StatisticsUpdateListener, ConfigurationChangeListener, DynamicDestroyable {

	private final StatisticsTableModel statsModel;
	private final Configuration configuration;
	private List<DynamicString> tabNames = new ArrayList<>();
	private final XMLGuiMessages xmlGuiMessages;

	public DynamicTabbedPane(StatisticsTableModel statsModel, Configuration configuration, XMLGuiMessages xmlGuiMessages) {
		this.statsModel = statsModel;
		this.configuration = configuration;
		this.xmlGuiMessages = xmlGuiMessages;
		this.configuration.addConfigurationChangeListener(this);
		this.statsModel.addStatisticsUpdateListener(this);
	}
	
	@Override
	public void addTab(String title, Component component) {
		DynamicString s = new DynamicString(title, statsModel, xmlGuiMessages.XMLGUI_ACCESSOR, configuration);
		tabNames.add(s);
		super.addTab(s.toString(), component);
	}
	
	@Override
	public void removeTabAt(int index) {
		tabNames.remove(index);
		super.removeTabAt(index);
	}
	
	@Override
	public void update() {
		for(int c = 0; c < tabNames.size(); c++) {
			setTitleAt(c, tabNames.get(c).toString());
		}
	}

	@Override
	public void configurationChanged(Profile profile) {
		update();
	}

	@Override
	public void destroy(){
		configuration.removeConfigurationChangeListener(this);
		statsModel.removeStatisticsUpdateListener(this);
	}
}
