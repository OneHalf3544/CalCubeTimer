package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
class ShowConfigurationDialogAction extends AbstractAction {
	private CALCubeTimerFrame cct;
	public ShowConfigurationDialogAction(CALCubeTimerFrame cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.showConfigurationDialog(cct.model);
	}
}
