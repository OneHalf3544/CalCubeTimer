package net.gnehzr.cct.misc.dynamicGUI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

@Service
public class DynamicBorderSetter {

	private final Configuration configuration;
	private final XMLGuiMessages xmlGuiMessages;
	private final SessionsList sessionsList;

	@Autowired
	public DynamicBorderSetter(Configuration configuration, XMLGuiMessages xmlGuiMessages, SessionsList sessionsList) {
		this.configuration = configuration;
		this.xmlGuiMessages = xmlGuiMessages;
		this.sessionsList = sessionsList;
	}

	public Border getBorder(String dynamicString) {
		String[] titleAttrs = dynamicString.split(";");
		DynamicString titleString = new DynamicString(titleAttrs[0], xmlGuiMessages, configuration);

		DynamicString colorString = null;
		if (titleAttrs.length > 1) {
			colorString = new DynamicString(titleAttrs[1], xmlGuiMessages, configuration);
		}

		Border border = colorString == null
				? BorderFactory.createEtchedBorder()
				: BorderFactory.createLineBorder(Utils.stringToColor(colorString.toString(sessionsList), false));

		return new AABorder(BorderFactory.createTitledBorder(
				border, titleString.toString(sessionsList), TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, null, Color.BLACK));
	}
}
