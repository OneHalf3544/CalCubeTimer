package net.gnehzr.cct.scrambles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>
 * <p>
 * Created: 10.10.2015 19:04
 * <p>
 *
 * @author OneHalf
 */
public interface ScrambleList {

	@NotNull
	PuzzleType getPuzzleType();

	int scramblesCount();

	@Nullable
	ScrambleString getCurrentScramble();

	boolean isLastScrambleInList();

	ScrambleString getNext();

	int getScrambleNumber();

	void setScrambleNumber(int scrambleNumber);

	boolean isGenerating();

	default GeneratedScrambleList asGenerating() {
		checkState(isGenerating());
		return ((GeneratedScrambleList) this);
	}

	default ImportedScrambleList asImported() {
		checkState(isGenerating());
		return ((ImportedScrambleList) this);
	}
}
