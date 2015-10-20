package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class ScrambleChooserComboBox<T> extends JComboBox<T> implements TableCellRenderer, ConfigurationChangeListener {

	protected final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	public ScrambleChooserComboBox(boolean icons,
								   ScramblePluginManager scramblePluginManager,
								   Configuration configuration) {
		this.scramblePluginManager = scramblePluginManager;
		this.configuration = configuration;
		this.setRenderer(new PuzzleCustomizationCellRenderer(icons, scramblePluginManager));
		configuration.addConfigurationChangeListener(this);
		// todo configurationChanged(profileDao.getSelectedProfile());
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			super.setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}

		// Select the current value
		setSelectedItem(value);
		return this;
	}
	
	@Override
	public void configurationChanged(Profile profile) {
		this.setModel(new DefaultComboBoxModel<>(getScramblesTypeArray(profile)));
		this.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
	}

	protected abstract T[] getScramblesTypeArray(Profile profile);
}
