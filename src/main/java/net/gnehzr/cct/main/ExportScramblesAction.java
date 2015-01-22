package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.statistics.ProfileDao;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:21
 * <p>
 *
 * @author OneHalf
 */
@Singleton
class ExportScramblesAction extends AbstractAction {

	public static final String ACTION_NAME = "exportscrambles";

	private final ProfileDao profileDao;
	private final ScrambleImporter scrambleImporter;
	private final ScrambleList scramblesList;
	private final CALCubeTimerFrame calCubeTimerFrame;

	@Inject
	public ExportScramblesAction(ScrambleImporter scrambleImporter, ProfileDao profileDao,
								 ScrambleList scramblesList, CALCubeTimerFrame calCubeTimerFrame){
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		this.scrambleImporter = scrambleImporter;
		this.profileDao = profileDao;
		this.scramblesList = scramblesList;
		this.calCubeTimerFrame = calCubeTimerFrame;

	}

	@Inject
	public void registerAction(ActionMap actionMap) {
		actionMap.registerAction(ACTION_NAME, this);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		scrambleImporter.exportScramblesAction(profileDao.getSelectedProfile(), scramblesList);
	}
}
