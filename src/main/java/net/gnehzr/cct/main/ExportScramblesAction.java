package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.scrambles.ScrambleList;

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

	private final CalCubeTimerModel cubeTimerModel;
	private final ScrambleImporter scrambleImporter;
	private final ScrambleList scramblesList;

	@Inject
	public ExportScramblesAction(ScrambleImporter scrambleImporter, CalCubeTimerModel cubeTimerModel,
								 ScrambleList scramblesList) {
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		this.scrambleImporter = scrambleImporter;
		this.cubeTimerModel = cubeTimerModel;
		this.scramblesList = scramblesList;

	}

	@Inject
	public void registerAction(ActionMap actionMap) {
		actionMap.registerAction(ACTION_NAME, this);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		scrambleImporter.exportScramblesAction(cubeTimerModel.getSelectedProfile(), scramblesList);
	}
}
