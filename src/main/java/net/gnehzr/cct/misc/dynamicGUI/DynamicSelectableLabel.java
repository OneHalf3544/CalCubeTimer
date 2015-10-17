package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import net.gnehzr.cct.statistics.StatisticsUpdateListener;
import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import javax.swing.border.Border;

public class DynamicSelectableLabel extends JEditorPane implements StatisticsUpdateListener, DynamicStringSettable, ConfigurationChangeListener, DynamicDestroyable{
	private final Configuration configuration;
	private DynamicString s = null;

	public DynamicSelectableLabel(Configuration configuration){
		super("text/html", null);
		this.configuration = configuration;
		putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		setEditable(false);
		setBorder(null);
		setOpaque(false);
		this.configuration.addConfigurationChangeListener(this);
	}
	
	@Override
	public void updateUI() {
		Border b = getBorder();
		super.updateUI();
		setBorder(b);
	}

	@Override
	public void setDynamicString(DynamicString s){
		if(this.s != null) {
			this.s.getStatisticsModel().removeStatisticsUpdateListener(this);
		}
		this.s = s;
		if(this.s != null) {
			this.s.getStatisticsModel().addStatisticsUpdateListener(this);
			update(((SessionsList) null));
		}
	}

	@Override
	public void update(SessionsList sessions){
		if(s != null) setText(s.toString(sessions));
	}

	@Override
	public void configurationChanged(Profile profile){
		update(profile.getSessionsListTableModel().getSessionsList());
	}

	@Override
	public void destroy(){
		setDynamicString(null);
		configuration.removeConfigurationChangeListener(this);
	}
}
