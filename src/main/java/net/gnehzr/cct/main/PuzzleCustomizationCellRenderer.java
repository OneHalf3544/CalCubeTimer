package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import javax.swing.*;
import java.awt.*;

public class PuzzleCustomizationCellRenderer extends SubstanceDefaultListCellRenderer {

	private final boolean icons;
	private final ScramblePluginManager scramblePluginManager;

	public PuzzleCustomizationCellRenderer(boolean i, ScramblePluginManager scramblePluginManager) {
		this.icons = i;
		this.scramblePluginManager = scramblePluginManager;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		String val;
		Icon i = null;
		if(value != null) {
			PuzzleType puzzleType = (PuzzleType) value;
			ScrambleSettings scrambleSettings = scramblePluginManager.getScrambleVariation(puzzleType);
			if (icons) {
				i = scrambleSettings.getImage();
			}
			String bolded = puzzleType.getVariationName();
			if(bolded.isEmpty()) {
				bolded = puzzleType.getScramblePlugin().getPuzzleName();
			}
			val = "<html><b>" + bolded + "</b>";  
			if (puzzleType.getCustomization() != null) {
				val += ":" + puzzleType.getCustomization();
			}
			val += "</html>"; 
		} else
			val = ""; 
		Component c = super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
		setIcon(i);
		return c;
	}
}
