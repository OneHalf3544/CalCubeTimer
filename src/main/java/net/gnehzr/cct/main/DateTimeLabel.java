package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;

import javax.swing.*;
import java.awt.event.HierarchyEvent;
import java.time.LocalDateTime;

public class DateTimeLabel extends JLabel {
	private Timer updateTimer;
	private final Configuration configuration;

	public DateTimeLabel(Configuration configuration) {
		this.configuration = configuration;
		updateTimer = new Timer(90, (event) -> updateDisplay());
		this.addHierarchyListener(this::hierarchyChanged);
	}
	
	void updateDisplay() {
		this.setText(configuration.getDateFormat().format(LocalDateTime.now()));
	}

	public void hierarchyChanged(HierarchyEvent e) {
		if((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
			if(isDisplayable()) {
				updateTimer.start();
			} else {
				updateTimer.stop();
			}
		}
	}
}
