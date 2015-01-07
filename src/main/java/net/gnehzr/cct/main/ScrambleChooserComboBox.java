package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.ProfileDao;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ScrambleChooserComboBox extends LoudComboBox implements TableCellRenderer, ConfigurationChangeListener {
	private boolean customizations;
	private final ScramblePlugin scramblePlugin;
	private final Configuration configuration;

	@Inject
	public ScrambleChooserComboBox(boolean icons, boolean customizations, ScramblePlugin scramblePlugin,
								   Configuration configuration, ProfileDao profileDao) {
		this.customizations = customizations;
		this.scramblePlugin = scramblePlugin;
		this.configuration = configuration;
		this.setRenderer(new PuzzleCustomizationCellRenderer(icons));
		configuration.addConfigurationChangeListener(this);
		configurationChanged(profileDao.getSelectedProfile());
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
		Object[] model;
		if(customizations)
			model = scramblePlugin.getScrambleCustomizations(profile, false).toArray();
		else
			model = scramblePlugin.getScrambleVariations();
		this.setModel(new DefaultComboBoxModel(model));
		this.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS, false));
	}
}
