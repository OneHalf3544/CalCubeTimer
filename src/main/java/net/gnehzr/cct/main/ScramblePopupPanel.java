package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.scrambles.ScrambleViewComponent;
import net.gnehzr.cct.statistics.Profile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Service
public class ScramblePopupPanel extends JPanel {

	private final Configuration configuration;
	private JTextArea scrambleInfoTextArea;
	private JScrollPane scrambleInfoScroller;
	private ScrambleViewComponent incrementalScrambleView;
	private ScrambleViewComponent scrambleFinalView;

	@Autowired
	private ToggleScramblePopupAction visibilityAction;

	@Autowired
	public ScramblePopupPanel(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		super(new GridLayout(1, 0));
		this.configuration = configuration;
		incrementalScrambleView = new ScrambleViewComponent(configuration, scramblePluginManager);
		scrambleFinalView       = new ScrambleViewComponent(configuration, scramblePluginManager);

		// TODO add toggle view checkboxes
		add(incrementalScrambleView);
		scrambleInfoTextArea = new JTextArea();
		scrambleInfoScroller = new JScrollPane(scrambleInfoTextArea);

		this.configuration.addConfigurationChangeListener(this::configurationChanged);
		addMouseListener(createMouseListener());
	}

	public void refreshPopup() {
		validate();
		setVisible(incrementalScrambleView.scrambleHasImage() && configuration.getBoolean(VariableKey.SCRAMBLE_POPUP));
	}

	@Override
	public void setVisible(boolean c) {
		//this is here to prevent calls to setVisible(true) when the popup is already visible
		//if we were to allow these, then the main gui could pop up on top of our fullscreen panel
		if(isVisible() == c) {
			return;
		}
		if(incrementalScrambleView.scrambleHasImage()) {
			configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, c);
			visibilityAction.putValue(Action.SELECTED_KEY, c);
		}
		super.setVisible(c);
	}

	public void configurationChanged(Profile profile) {
		setFinalViewVisible(configuration.getBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE));
		incrementalScrambleView.syncColorScheme(false);
		refreshPopup();
	}

	public boolean isScrambleVisible() {
		return incrementalScrambleView.scrambleHasImage() && configuration.getBoolean(VariableKey.SCRAMBLE_POPUP);
	}

	public void setScramble(ScrambleString incrementalScramble, ScrambleString fullScramble) {
		incrementalScrambleView.setScramble(incrementalScramble);
		scrambleFinalView.setScramble(fullScramble);
		String info = incrementalScramble.getTextComments();
		if(info == null) {
			remove(scrambleInfoScroller);
		} else {
			scrambleInfoTextArea.setText(incrementalScramble.getTextComments());
			scrambleInfoScroller.setPreferredSize(incrementalScrambleView.getPreferredSize()); //force scrollbars if necessary
			scrambleInfoTextArea.setCaretPosition(0); //force scroll to the top
			add(scrambleInfoScroller);
		}
		refreshPopup();
	}


	private MouseAdapter createMouseListener() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				maybeShowPopup(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent mouseEvent) {
				if (mouseEvent.isPopupTrigger()) {
					JCheckBoxMenuItem showFinal = new JCheckBoxMenuItem(StringAccessor.getString("ScrambleFrame.showfinalview"), isFinalViewVisible());
					showFinal.addActionListener(e1 -> {
                        JCheckBoxMenuItem src = (JCheckBoxMenuItem) e1.getSource();
                        setFinalViewVisible(src.isSelected());
                    });

					JPopupMenu popup = new JPopupMenu();
					popup.add(showFinal);
					popup.show(ScramblePopupPanel.this, mouseEvent.getX(), mouseEvent.getY());
				}
			}
		};
	}
	
	private boolean isFinalViewVisible() {
		return scrambleFinalView.getParent() == this;
	}

	public void setFinalViewVisible(boolean visible) {
		configuration.setBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, visible);
		if(isFinalViewVisible() == visible) {
			return;
		}
		if(visible) {
			add(scrambleFinalView, 1);
		}
		else {
			remove(scrambleFinalView);
		}
		validate();
	}
}
