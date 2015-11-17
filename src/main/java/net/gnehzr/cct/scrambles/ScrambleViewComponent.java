package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class ScrambleViewComponent extends PuzzleViewComponent {

	private ScrambleString scrambleString;

	public ScrambleViewComponent(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		super(configuration, scramblePluginManager);
		addComponentListener(createResizeListener());
		scrambleString = null;
	}

	public boolean scrambleHasImage() {
		return buffer != null;
	}

	public void setScramble(ScrambleString scrambleString) {
		this.scrambleString = scrambleString;

		colorScheme.computeIfAbsent(
                getPuzzleType(),
                puzzleType -> scramblePluginManager.getColorScheme(getPuzzleType().getScramblePlugin(), false, configuration));

		faceShapes = getPuzzleType().getScramblePlugin().getFaces(
				GAP,
				getUnitSize(false, getPuzzleType()),
				getPuzzleType().getVariationName());

		buffer = scramblePluginManager.getScrambleImage(scrambleString, GAP, getUnitSize(false, getPuzzleType()), colorScheme.get(getPuzzleType()));
		repaint();	//this will cause the scramble to be drawn
		invalidate(); //this forces the component to fit itself to its layout properly
	}

	private ComponentAdapter createResizeListener() {
		return new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				PuzzleType puzzleType = scrambleString.getPuzzleType();
				puzzleType.setPuzzleUnitSize(
						puzzleType.getScramblePlugin().getNewUnitSize(getWidth(), getHeight(), GAP, puzzleType.getVariationName()));
				setScramble(scrambleString);
			}
		};
	}

	@Override
	protected PuzzleType getPuzzleType() {
		return scrambleString.getPuzzleType();
	}

	@Override
	public void syncColorScheme(boolean defaults) {
		if (scrambleString == null) {
			return;
		}
		setScramble(scrambleString);
		super.syncColorScheme(defaults);
	}

	@Override
	protected int getUnitSize(boolean defaults, PuzzleType puzzleType) {
        return puzzleType.getPuzzleUnitSize(puzzleType.getScramblePlugin(), defaults);
    }

	@Override
	protected void paintFace(Graphics g) {
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
		((Graphics2D)g).setComposite(ac);
		//first, draw the whole scramble opaque
		g.drawImage(buffer, 0, 0, null);

		//now prepare the surface for drawing the selected face in solid
		g.setClip(null);
		ac = ac.derive(1.0f);
		((Graphics2D)g).setComposite(ac);
		//if no face is selected, we draw the whole thing solid, otherwise, just the selected face
		g.drawImage(buffer, 0, 0, null);
    }
}
