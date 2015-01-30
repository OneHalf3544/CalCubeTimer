package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.dao.ProfileDao;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ScrambleChooserComboBox<T> extends JComboBox<T> implements TableCellRenderer, ConfigurationChangeListener {
	private boolean customizations;
	private final ScramblePluginManager scramblePluginManager;
	private final Configuration configuration;

	public ScrambleChooserComboBox(boolean icons, boolean customizations, ScramblePluginManager scramblePluginManager,
								   Configuration configuration, ProfileDao profileDao) {
		this.customizations = customizations;
		this.scramblePluginManager = scramblePluginManager;
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
			model = scramblePluginManager.getScrambleCustomizations(profile, false).toArray();
		else
			model = scramblePluginManager.getScrambleVariations();
		this.setModel(new DefaultComboBoxModel<>((T[])model));
		this.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS, false));
	}
}
