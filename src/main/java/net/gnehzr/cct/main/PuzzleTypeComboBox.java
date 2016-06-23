package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import javax.swing.*;

import java.awt.*;

public final class PuzzleTypeComboBox extends JComboBox<PuzzleType> {

	public PuzzleTypeComboBox(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		this(configuration, new PuzzleTypeComboBoxModel(scramblePluginManager));
	}

	public PuzzleTypeComboBox(Configuration configuration, PuzzleTypeComboBoxModel puzzleTypeComboBoxModel) {
		this.setRenderer(new SubstanceDefaultListCellRenderer() {
			@Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return super.getListCellRendererComponent(
						list,
						String.format("<html><b>%s</b></html>", ((PuzzleType) value).getVariationName()),
						index,
						isSelected,
						cellHasFocus);
            }

		});
		configuration.addConfigurationChangeListener(p -> {
			initialize(configuration, puzzleTypeComboBoxModel);
		});
		initialize(configuration, puzzleTypeComboBoxModel);
	}

	private void initialize(Configuration configuration, PuzzleTypeComboBoxModel puzzleTypeComboBoxModel) {
		this.setModel(puzzleTypeComboBoxModel);
		this.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
	}

}
