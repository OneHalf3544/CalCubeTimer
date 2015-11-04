package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;

@Singleton
public class ScrambleStringEditor extends SolutionCellEditor {

	private final SessionsList sessionsList;

	@Inject
	public ScrambleStringEditor(SessionsList sessionsList) {
		super(new JTextField(), "CALCubeTimer.typenewscramble");
		this.sessionsList = sessionsList;
	}

	@Override
	protected ScrambleString parseFromString(String s) throws Exception {
		return sessionsList.getCurrentSession().getPuzzleType().importScramble(s);
	}

}
