package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.Utils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:19
 * <p>
 *
 * @author OneHalf
 */
class DocumentationAction extends AbstractNamedAction {

	private CALCubeTimerFrame cct;
	private Configuration configuration;

	public DocumentationAction(CALCubeTimerFrame cct, Configuration configuration){
		super("showdocumentation");
		this.cct = cct;
		this.configuration = configuration;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		try {
			Desktop.getDesktop().browse(configuration.getDocumentationFile());
		} catch(IOException error) {
			Utils.showErrorDialog(cct, error);
		}
	}
}
