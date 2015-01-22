package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:19
 * <p>
 *
 * @author OneHalf
 */
class DocumentationAction extends AbstractAction {
	private CALCubeTimerFrame cct;
	public DocumentationAction(CALCubeTimerFrame cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.showDocumentation();
	}
}
