package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.statistics.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GeneratedScrambleList implements ScrambleList {

	private final ScramblePluginManager scramblePluginManager;
	private Session session;
	private int scrambleNumber = 0;

	private ScrambleString currentScrambleString;
	private ScrambleVariation currentScrambleVariation;

	public GeneratedScrambleList(Session session) {
		this.session = session;
		this.scramblePluginManager = session.getPuzzleType().scramblePluginManager;
	}

	@Override
	@NotNull
	public PuzzleType getPuzzleType() {
		return session.getPuzzleType();
	}

	public void setSession(@NotNull Session session) {
		this.session = Objects.requireNonNull(session);
	}
	
	public void setScrambleLength(int scrambleLength) {
		this.currentScrambleVariation.setLength(scrambleLength);
		session.getPuzzleType().getScrambleVariation().setLength(scrambleLength);
	}

	public void updateGeneratorGroup(String generatorGroup) {
		this.currentScrambleVariation = currentScrambleVariation.withGeneratorGroup(generatorGroup);
		scramblePluginManager.saveGeneratorToConfiguration(session.getPuzzleType());
	}

	public void clear() {
		setSession(session.cloneEmpty());
		scrambleNumber = 0;
	}
	
	@Override
	public int scramblesCount() {
		return session.getSolutionsCount();
	}

	@Override
	@Nullable
	public ScrambleString getCurrentScramble() {
		return currentScrambleString;
	}

	@Override
	public boolean isLastScrambleInList() {
		return scrambleNumber == session.getStatistics().getAttemptCount();
	}

	@Override
	public ScrambleString getNext() {
		scrambleNumber++;
		if (isLastScrambleInList()) {
			return currentScrambleString = getPuzzleType().generateScramble(currentScrambleVariation);
		}
		else {
			return session.getSolutions(scrambleNumber).getScrambleString();
		}
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
		return true;
	}
}
