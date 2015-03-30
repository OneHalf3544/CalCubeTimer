package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.time.LocalDateTime;

public class DateTimeLabel extends JLabel implements ActionListener, HierarchyListener {
	private Timer updateTimer;
	private final Configuration configuration;

	public DateTimeLabel(Configuration configuration) {
		this.configuration = configuration;
		updateTimer = new Timer(90, this);
		this.addHierarchyListener(this);
	}
	
	void updateDisplay() {
		this.setText(configuration.getDateFormat().format(LocalDateTime.now()));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updateDisplay();
	}

	@Override
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
