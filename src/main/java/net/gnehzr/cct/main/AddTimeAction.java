package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;

import javax.swing.*;
import java.awt.*;
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
public class AddTimeAction extends AbstractAction {

	@Inject
	private CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;
	@Inject
	private CurrentSessionSolutionsTable currentSessionSolutionsTable;

	@Inject
	public AddTimeAction(CALCubeTimerFrame cct){
		this.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
		this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
	}

	@Override
	public void actionPerformed(ActionEvent e){
		if (currentSessionSolutionsTable.isFocusOwner() || currentSessionSolutionsTable.requestFocusInWindow()) {
			//if the timestable is hidden behind a tab, we don't want to let the user add times
			currentSessionSolutionsTable.promptForNewRow();
			Rectangle newTimeRect = currentSessionSolutionsTable.getCellRect(currentSessionSolutionsTableModel.getRowCount(), 0, true);
			currentSessionSolutionsTable.scrollRectToVisible(newTimeRect);
		}
	}

	@Inject
	public void registerAction(ActionMap actionMap) {
		actionMap.registerAction(ActionMap.ADD_TIME_ACTION, this);
	}

}
