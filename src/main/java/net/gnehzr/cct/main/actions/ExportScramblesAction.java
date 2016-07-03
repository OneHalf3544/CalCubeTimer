package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.ScrambleImporter;
import net.gnehzr.cct.scrambles.ScrambleListHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import javax.swing.ActionMap;
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
class ExportScramblesAction extends AbstractNamedAction {

	public static final String ACTION_NAME = "exportscrambles";

	private final ScrambleListHolder scrambleListHolder;
	private final ScrambleImporter scrambleImporter;

	@Autowired
	public ExportScramblesAction(ScrambleImporter scrambleImporter, ScrambleListHolder scrambleListHolder) {
		super(ACTION_NAME);
		putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
		this.scrambleImporter = scrambleImporter;
		this.scrambleListHolder = scrambleListHolder;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		scrambleImporter.exportScramblesAction(scrambleListHolder.getPuzzleType());
	}
}
