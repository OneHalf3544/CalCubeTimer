package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Map;

public class ScrambleViewComponent extends JComponent {

	private static final int DEFAULT_GAP = 5;
	private static final Dimension PREFERRED_SIZE = new Dimension(0, 0);

	private static Integer GAP = DEFAULT_GAP;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private boolean fixedSize;

	private String focusedFaceId = null;
	private BufferedImage buffer;
	private ScramblePlugin currentPlugin = null;
	private PuzzleType puzzleType;

	private ScrambleString scrambleString = null;

	// todo move to scramblePluginManager:
	private Map<String, Color> colorScheme = null;
	private Map<String, Shape> faces = null;

	public ScrambleViewComponent(boolean fixedSize, boolean detectColorClicks, Configuration configuration,
								 ScramblePluginManager scramblePluginManager) {
		this.fixedSize = fixedSize;
		this.scramblePluginManager = scramblePluginManager;
		if(!fixedSize) {
			addComponentListener(createResizeListener());
		}
		if (detectColorClicks) {
			addMouseListener(createMouseListener());
			addMouseMotionListener(createMouseMotionListener());
		}
		configuration.addConfigurationChangeListener(createConfigurationListener(configuration));
		this.configuration = configuration;
		puzzleType = scramblePluginManager.NULL_PUZZLE_TYPE;
	}

	public void syncColorScheme(boolean defaults) {
		if(currentPlugin != null) {
			colorScheme = scramblePluginManager.getColorScheme(currentPlugin, defaults);
			redo();
		}
	}
	
	public void redo() {
		setScramble(scrambleString, puzzleType);
	}

	public void setScramble(ScrambleString scrambleString, PuzzleType puzzleType) {
		this.scrambleString = scrambleString;
		this.puzzleType = puzzleType;

		if(colorScheme == null) {
			currentPlugin = scrambleString.getScramblePlugin();
			colorScheme = scramblePluginManager.getColorScheme(currentPlugin, false);
		}

		faces = currentPlugin.getFaces(GAP, getUnitSize(false), this.puzzleType.getVariationName());
		buffer = scramblePluginManager.getScrambleImage(this.scrambleString, GAP, getUnitSize(false), colorScheme);
		repaint();	//this will cause the scramble to be drawn
		invalidate(); //this forces the component to fit itself to its layout properly
	}

	public boolean scrambleHasImage() {
		return buffer != null;
	}

	@Override
	public Dimension getPreferredSize() {
		if(buffer == null) {
			return PREFERRED_SIZE;
		}
		return new Dimension(buffer.getWidth(), buffer.getHeight());
	}

	@Override
	public Dimension getMinimumSize() {
		if(buffer == null) {
			return PREFERRED_SIZE;
		}
		Dimension d = currentPlugin.getImageSize(GAP, getUnitSize(true), puzzleType.getVariationName());
		if(d != null) {
			return d;
		}
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
			if(focusedFaceId == null) {
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				((Graphics2D)g).setComposite(ac);
				//first, draw the whole scramble opaque
				g.drawImage(buffer, 0, 0, null);
				
				//now prepare the surface for drawing the selected face in solid
				g.setClip(faces.get(focusedFaceId));
				ac = ac.derive(1.0f);
				((Graphics2D)g).setComposite(ac);
			}
			//if no face is selected, we draw the whole thing solid, otherwise, just the selected face
			g.drawImage(buffer, 0, 0, null);
		}

		g.dispose();
	}

	private void findFocusedFace(Point p) {
		focusedFaceId = null;
		if (p == null) {
			repaint();
			return;
		}
		focusedFaceId = faces.entrySet().stream()
				.filter(shape -> shape.getValue() != null && shape.getValue().contains(p))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);

		repaint();
	}

	public void commitColorSchemeToConfiguration() {
		for(Map.Entry<String, Color> face : colorScheme.entrySet()) {
			configuration.setColor(VariableKey.PUZZLE_COLOR(currentPlugin, face.getKey()), face.getValue());
		}
	}

	private int getUnitSize(boolean defaults) {
		if(fixedSize) {
			return currentPlugin.getDefaultUnitSize();
		}
		return getPuzzleType().getPuzzleUnitSize(currentPlugin, defaults);
	}


	private ConfigurationChangeListener createConfigurationListener(final Configuration configuration) {
		return profile -> {
			GAP = configuration.getInt(VariableKey.POPUP_GAP);
			if(GAP == null) {
				GAP = DEFAULT_GAP;
			}
		};
	}

	private ComponentAdapter createResizeListener() {
		return new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (!puzzleType.isNullType()) {
					puzzleType.setPuzzleUnitSize(currentPlugin.getNewUnitSize(getWidth(), getHeight(), GAP, puzzleType.getVariationName()));
					redo();
				}
			}
		};
	}

	private MouseMotionAdapter createMouseMotionListener() {
		return new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				findFocusedFace(e.getPoint());
			}
		};
	}

	private MouseAdapter createMouseListener() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(focusedFaceId != null) {
					Color c = JColorChooser.showDialog(ScrambleViewComponent.this,
							StringAccessor.getString("ScrambleViewComponent.choosecolor") + ": " + focusedFaceId,
							colorScheme.get(focusedFaceId));
					if(c != null) {
						colorScheme.put(focusedFaceId, c);
						redo();
					}
					findFocusedFace(getMousePosition());
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				findFocusedFace(null);
			}

		};
	}


	public PuzzleType getPuzzleType() {
		return puzzleType;
	}
}
