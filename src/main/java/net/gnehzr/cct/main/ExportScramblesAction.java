package net.gnehzr.cct.main;

import net.gnehzr.cct.statistics.ProfileDao;

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
class ExportScramblesAction extends AbstractAction {
	private CALCubeTimer cct;
	private final ProfileDao profileDao;

	public ExportScramblesAction(CALCubeTimer cct, ProfileDao profileDao){
		this.cct = cct;
		this.profileDao = profileDao;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.exportScramblesAction(profileDao.getSelectedProfile());
	}
}
