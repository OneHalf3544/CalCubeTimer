package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import javax.swing.*;

import java.awt.*;

import static com.google.common.collect.Iterables.toArray;

public final class PuzzleTypeComboBox extends JComboBox<PuzzleType> {

	public PuzzleTypeComboBox(ScramblePluginManager scramblePluginManager, Configuration configuration) {
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
			initialize(scramblePluginManager, configuration);
		});
		initialize(scramblePluginManager, configuration);
	}

	private void initialize(ScramblePluginManager scramblePluginManager, Configuration configuration) {
		this.setModel(new DefaultComboBoxModel<>(toArray(scramblePluginManager.getPuzzleTypes(), PuzzleType.class)));
		this.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
	}
}
