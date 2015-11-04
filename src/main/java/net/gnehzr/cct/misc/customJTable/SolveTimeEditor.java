package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.statistics.SolveTime;

import javax.swing.*;

@Singleton
public class SolveTimeEditor extends SolutionCellEditor {

	@Inject
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
