package net.gnehzr.cct.scrambles;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	@Nullable
	public ScrambleString getCurrentScramble() {
		return isLastScrambleInList() ? null : scrambles.get(scrambleNumber);
	}

	@Override
	public boolean isLastScrambleInList() {
		return scrambleNumber == scrambles.size();
	}

	@Override
	public ScrambleString getNext() {
		scrambleNumber++;
		return getCurrentScramble();
	}

	@Override
	public int getScrambleNumber() {
		return scrambleNumber + 1;
	}

	@Override
	public void setScrambleNumber(int scrambleNumber) {
		this.scrambleNumber = scrambleNumber - 1;
	}

	@Override
	public boolean isGenerating() {
		return false;
	}
}
