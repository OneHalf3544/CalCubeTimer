package net.gnehzr.cct.scrambles;

import org.jetbrains.annotations.NotNull;

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

	@NotNull
	ScrambleString getCurrentScramble();

	ScrambleString generateNext();

	boolean isGenerating();

	default boolean isImported() {
		return !isGenerating();
	}

	default GeneratedScrambleList asGenerating() {
		checkState(isGenerating());
		return ((GeneratedScrambleList) this);
	}

	default ImportedScrambleList asImported() {
		checkState(isImported());
		return ((ImportedScrambleList) this);
	}

	int getScrambleNumber();
}
