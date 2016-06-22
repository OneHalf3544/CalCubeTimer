package net.gnehzr.cct.misc.customJTable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.statistics.SolveTime;

import javax.swing.*;

@Service
public class SolveTimeEditor extends SolutionCellEditor {

	@Autowired
	public SolveTimeEditor() {
		super(new JTextField(), "CALCubeTimer.typenewtime");
	}

	@Override
	protected SolveTime parseFromString(String s) {
		return new SolveTime(s);
	}

	@Override
	public Object getCellEditorValue() {
		return value;
	}
}
