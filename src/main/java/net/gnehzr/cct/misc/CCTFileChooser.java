package net.gnehzr.cct.misc;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import javax.swing.*;
import java.awt.*;

public class CCTFileChooser extends JFileChooser {
	private final Configuration configuration;

	public CCTFileChooser(Configuration configuration) {
		super(configuration.getString(VariableKey.LAST_VIEWED_FOLDER, false));
		this.configuration = configuration;
	}
	@Override
	public int showDialog(Component parent, String approveButtonText)
			throws HeadlessException {
		int t = super.showDialog(parent, approveButtonText);
		if(t == JFileChooser.APPROVE_OPTION)
			configuration.setString(VariableKey.LAST_VIEWED_FOLDER, this.getSelectedFile().getParent());
		return t;
	}
}
