package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:21
 * <p>
 *
 * @author OneHalf
 */
class ResetAction extends AbstractAction {
	private CALCubeTimer cct;
	public ResetAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.resetAction();
	}
}
