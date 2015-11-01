package net.gnehzr.cct.help;

import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;


public class AboutScrollFrame extends JFrame {
	private JScrollPane editorScrollPane;
	private Timer autoscrollTimer;

	public AboutScrollFrame(URL helpURL, Image icon) throws Exception {
		this.setIconImage(icon);
		JTextPane pane = new JTextPane();
		pane.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		pane.setOpaque(false);
		pane.setEditable(false);
		if(helpURL != null) {
			try {
				pane.setPage(helpURL);
			} catch (IOException e) {
				throw new Exception("Could not find: " + helpURL); 
			}
		} else {
			throw new Exception("Couldn't find help file"); 
		}

		editorScrollPane = new JScrollPane(pane);
		editorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		editorScrollPane.setPreferredSize(new Dimension(250, 145));
		editorScrollPane.setMinimumSize(new Dimension(10, 10));
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				autoscrollTimer.start();
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
				autoscrollTimer.stop();
			}
		});
		this.add(editorScrollPane);
		this.setSize(600, 300);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		autoscrollTimer = new Timer(100, this::scrollByTimer);
	}

	public void scrollByTimer(ActionEvent e) {
		JScrollBar vert = editorScrollPane.getVerticalScrollBar();
		vert.setValue(vert.getValue() + 1);
	}
}
