package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.CurrentSessionSolutionsTable;
import org.springframework.beans.factory.annotation.Autowired;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import org.springframework.stereotype.Component;

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
@Component
public class AddTimeAction extends AbstractNamedAction {

	@Autowired
	private CurrentSessionSolutionsTableModel currentSessionSolutionsTableModel;

	@Autowired
	private CurrentSessionSolutionsTable currentSessionSolutionsTable;

	public AddTimeAction(){
		super("addtime");
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		this.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_MASK));
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
}
