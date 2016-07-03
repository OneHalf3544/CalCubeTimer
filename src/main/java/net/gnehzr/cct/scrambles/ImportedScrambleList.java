package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ImportedScrambleList implements ScrambleList {

	private final List<ScrambleString> scrambles;
	private final PuzzleType puzzleType;

	private int scrambleNumber = 0;

	public ImportedScrambleList(PuzzleType puzzleType, List<ScrambleString> scrambles) {
		this.puzzleType = puzzleType;
		this.scrambles = ImmutableList.copyOf(scrambles);
	}

	@Override
	@NotNull
	public PuzzleType getPuzzleType() {
		return puzzleType;
	}

	@Override
	public int scramblesCount() {
		return scrambles.size();
	}
	
	@Override
	@NotNull
	public ScrambleString getCurrentScramble() {
		return scrambles.get(scrambleNumber);
	}

	public boolean isLastScrambleInList() {
		return getScrambleNumber() >= scrambles.size();
	}

	@Override
	public ScrambleString generateNext() {
		if (!isLastScrambleInList()) {
			scrambleNumber++;
		} else {
			Utils.showWarningDialog(null,
					StringAccessor.getString("CALCubeTimer.outofimported") +
							StringAccessor.getString("CALCubeTimer.generatedscrambles"));
		}
		return getCurrentScramble();
	}

	@Override
	public int getScrambleNumber() {
		return scrambleNumber + 1;
	}

	public void setScrambleNumber(int scrambleNumber) {
		this.scrambleNumber = scrambleNumber - 1;
	}

	@Override
	public boolean isGenerating() {
		return false;
	}
}
