package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.Scramble;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.scrambles.ScrambleViewComponent;
import net.gnehzr.cct.statistics.Profile;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@Singleton
public class ScramblePopupFrame extends JDialog implements MouseListener, ActionListener {

	private final Configuration configuration;
	private JPanel pane;
	private JTextArea scrambleInfoTextArea;
	private JScrollPane scrambleInfoScroller;
	private ScrambleViewComponent incrementalScrambleView;
	private ScrambleViewComponent finalView;

	@Inject
	private ToggleScramblePopupAction visibilityAction;

	@Inject
	public ScramblePopupFrame(CALCubeTimerFrame parent,
							  Configuration configuration, ScramblePluginManager scramblePluginManager) {
		super(parent);
		this.configuration = configuration;
		incrementalScrambleView = new ScrambleViewComponent(false, false, configuration, scramblePluginManager);
		finalView = 			  new ScrambleViewComponent(false, false, configuration, scramblePluginManager);
		pane = new JPanel(new GridLayout(1, 0));
		pane.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.FALSE);
		pane.add(incrementalScrambleView);
		scrambleInfoTextArea = new JTextArea();
		scrambleInfoScroller = new JScrollPane(scrambleInfoTextArea);
		scrambleInfoTextArea.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.FALSE);
		scrambleInfoScroller.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.FALSE);
		this.setContentPane(pane);
		this.configuration.addConfigurationChangeListener(this::configurationChanged);
		addMouseListener(this);
		setFinalViewVisible(this.configuration.getBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, false));
	}

	@Inject
	void initialize() {
		this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		this.setIconImage(CALCubeTimerFrame.CUBE_ICON.getImage());
		this.setFocusableWindowState(false);

	}

	public void refreshPopup() {
		pack();
		setVisible(incrementalScrambleView.scrambleHasImage() && configuration.getBoolean(VariableKey.SCRAMBLE_POPUP, false));
	}
	@Override
	public void setVisible(boolean c) {
		//this is here to prevent calls to setVisible(true) when the popup is already visible
		//if we were to allow these, then the main gui could pop up on top of our fullscreen panel
		if(isVisible() == c)
			return;
		if(incrementalScrambleView.scrambleHasImage()) {
			configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, c);
			visibilityAction.putValue(Action.SELECTED_KEY, c);
		}
		super.setVisible(c);
	}

	public void configurationChanged(Profile profile) {
		setFinalViewVisible(configuration.getBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, false));
		incrementalScrambleView.syncColorScheme(false);
		refreshPopup();
		Point location = configuration.getPoint(VariableKey.SCRAMBLE_VIEW_LOCATION, false);
		if(location != null)
			setLocation(location);
	}

	public void setScramble(Scramble incrementalScramble, Scramble fullScramble, ScrambleVariation newVariation) {
		incrementalScrambleView.setScramble(incrementalScramble, newVariation);
		finalView.setScramble(fullScramble, newVariation);
		String info = incrementalScramble.getExtraInfo();
		if(info == null) {
			pane.remove(scrambleInfoScroller);
		} else {
			scrambleInfoTextArea.setText(incrementalScramble.getExtraInfo());
			scrambleInfoScroller.setPreferredSize(incrementalScrambleView.getPreferredSize()); //force scrollbars if necessary
			scrambleInfoTextArea.setCaretPosition(0); //force scroll to the top
			pane.add(scrambleInfoScroller);
		}
		refreshPopup();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		maybeShowPopup(e);
	}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {
		maybeShowPopup(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}
	private void maybeShowPopup(MouseEvent e) {
		if(e.isPopupTrigger()) {
			JPopupMenu popup = new JPopupMenu();
			JCheckBoxMenuItem showFinal = new JCheckBoxMenuItem(StringAccessor.getString("ScrambleFrame.showfinalview"), isFinalViewVisible());
			showFinal.addActionListener(this);
			popup.add(showFinal);
			popup.show(this, e.getX(), e.getY());
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		JCheckBoxMenuItem src = (JCheckBoxMenuItem) e.getSource();
		setFinalViewVisible(src.isSelected());
	}
	
	private boolean isFinalViewVisible() {
		return finalView.getParent() == pane;
	}
	public void setFinalViewVisible(boolean visible) {
		configuration.setBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, visible);
		if(isFinalViewVisible() == visible) return;
		if(visible)
			pane.add(finalView, 1);
		else
			pane.remove(finalView);
		pack();
	}
}
