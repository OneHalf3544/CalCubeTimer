package net.gnehzr.cct.misc.customJTable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.statistics.SessionsList;

import javax.swing.*;

@Service
public class ScrambleStringEditor extends SolutionCellEditor {

	private final SessionsList sessionsList;

	@Autowired
	public ScrambleStringEditor(SessionsList sessionsList) {
		super(new JTextField(), "CALCubeTimer.typenewscramble");
		this.sessionsList = sessionsList;
	}

	@Override
	protected ScrambleString parseFromString(String s) throws Exception {
		return sessionsList.getCurrentSession().getPuzzleType().importScramble(s);
	}

}
