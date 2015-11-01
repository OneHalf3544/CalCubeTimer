package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;

import javax.swing.*;

import static com.google.common.collect.Iterables.toArray;

public final class PuzzleTypeComboBox extends JComboBox<PuzzleType> {

	protected final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	public PuzzleTypeComboBox(boolean icons,
							  ScramblePluginManager scramblePluginManager,
							  Configuration configuration) {
		this.scramblePluginManager = scramblePluginManager;
		this.configuration = configuration;
		this.setRenderer(new PuzzleTypeCellRenderer(icons, scramblePluginManager));
		configuration.addConfigurationChangeListener(p -> {
			this.setModel(new DefaultComboBoxModel<>(toArray(this.scramblePluginManager.getPuzzleTypes(), PuzzleType.class)));
			this.setMaximumRowCount(this.configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
		});
	}
}
