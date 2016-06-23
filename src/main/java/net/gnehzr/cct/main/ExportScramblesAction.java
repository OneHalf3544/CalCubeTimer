package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleListHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
class ExportScramblesAction extends AbstractAction {

	public static final String ACTION_NAME = "exportscrambles";

	private final ScrambleListHolder scrambleListHolder;
	private final ScrambleImporter scrambleImporter;

	@Autowired
	public ExportScramblesAction(ScrambleImporter scrambleImporter, ScrambleListHolder scrambleListHolder) {
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		this.scrambleImporter = scrambleImporter;
		this.scrambleListHolder = scrambleListHolder;
	}

	@Autowired
	public void registerAction(ActionMap actionMap) {
		actionMap.registerAction(ACTION_NAME, this);
	}

	@Override
	public void actionPerformed(ActionEvent e){
		scrambleImporter.exportScramblesAction(scrambleListHolder.getPuzzleType());
	}
}
