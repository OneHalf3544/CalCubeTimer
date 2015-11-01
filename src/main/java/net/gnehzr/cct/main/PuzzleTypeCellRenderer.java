package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import javax.swing.*;
import java.awt.*;

public class PuzzleTypeCellRenderer extends SubstanceDefaultListCellRenderer {

	private static final Logger LOG = LogManager.getLogger(PuzzleTypeCellRenderer.class);

	private final boolean showIcons;
	private final ScramblePluginManager scramblePluginManager;

	public PuzzleTypeCellRenderer(boolean showIcons, ScramblePluginManager scramblePluginManager) {
		this.showIcons = showIcons;
		this.scramblePluginManager = scramblePluginManager;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value == null) {
			LOG.warn("render value == null");
			Component c = super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
			setIcon(null);
			return c;
		}

		PuzzleType puzzleType = (PuzzleType) value;

		Component c = super.getListCellRendererComponent(list, bolded(puzzleType.getVariationName()), index, isSelected, cellHasFocus);
		setIcon(showIcons ? scramblePluginManager.getScrambleVariation(puzzleType).getImage() : null);
		return c;
	}

	private String bolded(String bolded) {
		return String.format("<html><b>%s</b></html>", bolded);
	}
}
