package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

	@Autowired
	private CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;
	@Autowired
	private CurrentSessionSolutionsTable currentSessionSolutionsTable;

	@Autowired
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

	@Autowired
	public void registerAction(ActionMap actionMap) {
		actionMap.registerAction(ActionMap.ADD_TIME_ACTION, this);
	}

}
