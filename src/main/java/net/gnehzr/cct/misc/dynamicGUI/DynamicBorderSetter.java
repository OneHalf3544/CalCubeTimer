package net.gnehzr.cct.misc.dynamicGUI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

@Singleton
public class DynamicBorderSetter {

	private final CurrentSessionSolutionsTableModel statsModel;
	private final Configuration configuration;
	private final XMLGuiMessages xmlGuiMessages;

	@Inject
	public DynamicBorderSetter(CurrentSessionSolutionsTableModel statsModel, Configuration configuration, XMLGuiMessages xmlGuiMessages) {
		this.statsModel = statsModel;
		this.configuration = configuration;
		this.xmlGuiMessages = xmlGuiMessages;
	}

	public Border getBorder(String dynamicString) {
		String[] titleAttrs = dynamicString.split(";");
		DynamicString titleString = new DynamicString(titleAttrs[0], statsModel, xmlGuiMessages.XMLGUI_ACCESSOR, configuration);
		DynamicString colorString = null;
		if(titleAttrs.length > 1)
			colorString = new DynamicString(titleAttrs[1], null, xmlGuiMessages.XMLGUI_ACCESSOR, configuration);
		
		Border border;
		if(colorString == null)
			border = BorderFactory.createEtchedBorder();
		else
			border = BorderFactory.createLineBorder(Utils.stringToColor(colorString.toString(), false));
		return new AABorder(BorderFactory.createTitledBorder(border, titleString.toString(), TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, null, Color.BLACK));
	}
}
