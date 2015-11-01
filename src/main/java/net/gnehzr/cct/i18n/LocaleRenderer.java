package net.gnehzr.cct.i18n;

import net.gnehzr.cct.configuration.Configuration;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

import javax.swing.*;
import java.awt.*;

public class LocaleRenderer extends SubstanceDefaultListCellRenderer {
	private final Configuration configuration;

	public LocaleRenderer(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Icon i = null;
		String val = null;
		Font f = null;
		if(value instanceof LocaleAndIcon) {
			LocaleAndIcon l = (LocaleAndIcon) value;
			i = l.getFlag();
			val = l.toString();
			f = configuration.getFontForLocale(l);
		}
		Component c = super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
		setIcon(i);
		if(f != null)
			setFont(f);
		return c;
	}
}
