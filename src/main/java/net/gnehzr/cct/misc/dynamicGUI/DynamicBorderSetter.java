package net.gnehzr.cct.misc.dynamicGUI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

@Singleton
public class DynamicBorderSetter {

	private final Configuration configuration;
	private final XMLGuiMessages xmlGuiMessages;
	private final SessionsList sessionsList;

	@Inject
	public DynamicBorderSetter(Configuration configuration, XMLGuiMessages xmlGuiMessages, SessionsList sessionsList) {
		this.configuration = configuration;
		this.xmlGuiMessages = xmlGuiMessages;
		this.sessionsList = sessionsList;
	}

	public Border getBorder(String dynamicString) {
		String[] titleAttrs = dynamicString.split(";");
		DynamicString titleString = new DynamicString(titleAttrs[0], xmlGuiMessages.XMLGUI_ACCESSOR, configuration);

		DynamicString colorString = null;
		if (titleAttrs.length > 1) {
			colorString = new DynamicString(titleAttrs[1], xmlGuiMessages.XMLGUI_ACCESSOR, configuration);
		}

		Border border = colorString == null
				? BorderFactory.createEtchedBorder()
				: BorderFactory.createLineBorder(Utils.stringToColor(colorString.toString(sessionsList), false));

		return new AABorder(BorderFactory.createTitledBorder(
				border, titleString.toString(sessionsList), TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, null, Color.BLACK));
	}
}
