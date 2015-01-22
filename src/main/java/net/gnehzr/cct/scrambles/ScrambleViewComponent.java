package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.statistics.Profile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ScrambleViewComponent extends JComponent implements ComponentListener, MouseListener, MouseMotionListener {

	private static final int DEFAULT_GAP = 5;
	static Integer GAP = DEFAULT_GAP;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private boolean fixedSize;

	public ScrambleViewComponent(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		this.scramblePluginManager = scramblePluginManager;
		configuration.addConfigurationChangeListener(new ConfigurationChangeListener() {
			@Override
			public void configurationChanged(Profile profile) {
				GAP = configuration.getInt(VariableKey.POPUP_GAP, false);
				if(GAP == null)
					GAP = DEFAULT_GAP;
			}
		});
		this.configuration = configuration;
	}

	public ScrambleViewComponent(boolean fixedSize, boolean detectColorClicks, Configuration configuration, ScramblePluginManager scramblePluginManager) {
		this.fixedSize = fixedSize;
		this.scramblePluginManager = scramblePluginManager;
		if(!fixedSize)
			addComponentListener(this);
		if(detectColorClicks) {
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		this.configuration = configuration;
	}

	public void syncColorScheme(boolean defaults) {
		if(currentPlugin != null) {
			colorScheme = scramblePluginManager.getColorScheme(currentPlugin, defaults);
			redo();
		}
	}
	
	public void redo() {
		setScramble(currentScram, currentVariation);
	}
	private BufferedImage buffer;
	private Scramble currentScram = null;
	private Scramble currentPlugin = null;
	private ScrambleVariation currentVariation = null;
	private Color[] colorScheme = null;
	private Shape[] faces = null;
	public void setScramble(Scramble scramble, ScrambleVariation variation) {
		currentScram = scramble;
		currentVariation = variation;
		if(colorScheme == null || currentVariation.getPlugin() != currentPlugin) {
			currentPlugin = currentVariation.getPlugin();
			colorScheme = scramblePluginManager.getColorScheme(currentPlugin, false);
		}
		faces = currentPlugin.getFaces(GAP, getUnitSize(false), currentVariation.getVariation());
		buffer = scramblePluginManager.getScrambleImage(currentScram, GAP, getUnitSize(false), colorScheme);
		repaint();	//this will cause the scramble to be drawn
		invalidate(); //this forces the component to fit itself to its layout properly
	}

	public boolean scrambleHasImage() {
		return buffer != null;
	}
	
	private static final Dimension PREFERRED_SIZE = new Dimension(0, 0);
	@Override
	public Dimension getPreferredSize() {
		if(buffer == null)
			return PREFERRED_SIZE;
		return new Dimension(buffer.getWidth(), buffer.getHeight());
	}

	@Override
	public Dimension getMinimumSize() {
		if(buffer == null)
			return PREFERRED_SIZE;
		Dimension d = currentPlugin.getImageSize(GAP, getUnitSize(true), currentVariation.getVariation());
		if(d != null)
			return d;
		return PREFERRED_SIZE;
	}

	@Override
	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	@Override
	protected void paintComponent(Graphics g) {
		int width = getWidth();
		int height = getHeight();
		if(isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, width, height);
		}
		if(buffer != null) {
			if(focusedFace != -1) {
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				((Graphics2D)g).setComposite(ac);
				//first, draw the whole scramble opaque
				g.drawImage(buffer, 0, 0, null);
				
				//now prepare the surface for drawing the selected face in solid
				g.setClip(faces[focusedFace]);
				ac = ac.derive(1.0f);
				((Graphics2D)g).setComposite(ac);
			}
			//if no face is selected, we draw the whole thing solid, otherwise, just the selected face
			g.drawImage(buffer, 0, 0, null);
		}

		g.dispose();
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {}
	@Override
	public void componentMoved(ComponentEvent arg0) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e) {
		if(currentVariation != null) {
			currentVariation.setPuzzleUnitSize(currentPlugin.getNewUnitSize(getWidth(), getHeight(), GAP, currentVariation.getVariation()));
			redo();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(focusedFace != -1) {
			Color c = JColorChooser.showDialog(this,
					StringAccessor.getString("ScrambleViewComponent.choosecolor") + ": " + currentPlugin.getFaceNamesColors()[0][focusedFace],
					colorScheme[focusedFace]);
			if(c != null) {
				colorScheme[focusedFace] = c;
				redo();
			}
			findFocusedFace(getMousePosition());
		}
	}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {
		findFocusedFace(null);
	}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}
	@Override
	public void mouseMoved(MouseEvent e) {
		findFocusedFace(e.getPoint());
	}
	private int focusedFace = -1;
	private void findFocusedFace(Point p) {
		focusedFace = -1;
		for(int c = 0; p != null && faces != null && c < faces.length; c++) {
			if(faces[c] != null && faces[c].contains(p)) {
				focusedFace = c;
				break;
			}
		}
		repaint();
	}

	public void commitColorSchemeToConfiguration() {
		for(int face = 0; face < colorScheme.length; face++) {
			configuration.setColor(VariableKey.PUZZLE_COLOR(currentPlugin, currentPlugin.getFaceNamesColors()[0][face]),
					colorScheme[face]);
		}
	}

	private int getUnitSize(boolean defaults) {
		if(fixedSize)
			return currentPlugin.getDefaultUnitSize();
		return currentVariation.getPuzzleUnitSize(defaults);
	}
}
