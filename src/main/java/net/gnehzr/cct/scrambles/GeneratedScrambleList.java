package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.statistics.Session;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GeneratedScrambleList implements ScrambleList {

	private final ScramblePluginManager scramblePluginManager;
	private Session session;
	private int scrambleNumber = 0;

	private ScrambleString currentScrambleString;

	public GeneratedScrambleList(Session session) {
		this.session = session;
		this.scramblePluginManager = session.getPuzzleType().scramblePluginManager;
		this.currentScrambleString = session.getPuzzleType().isNullType()
				? scramblePluginManager.NULL_IMPORTED_SCRUMBLE
				: generateScramble();
	}

	@Override
	@NotNull
	public PuzzleType getPuzzleType() {
		return session.getPuzzleType();
	}

	public void setSession(@NotNull Session session) {
		this.session = Objects.requireNonNull(session);
		this.currentScrambleString = generateScramble();
	}
	
	public void setScrambleLength(int scrambleLength) {
		session.getPuzzleType().getScrambleVariation().setLength(scrambleLength);
	}

	public void updateGeneratorGroup(String generatorGroup) {
		scramblePluginManager.setScrambleSettings(getPuzzleType(), session.getPuzzleType().getScrambleVariation().withGeneratorGroup(generatorGroup));
		scramblePluginManager.saveGeneratorToConfiguration(session.getPuzzleType());
	}

	public void clear() {
		setSession(session.cloneEmpty());
		scrambleNumber = 0;
	}
	
	@Override
	public int scramblesCount() {
		return session.getAttemptsCount();
	}

	@Override
	@NotNull
	public ScrambleString getCurrentScramble() {
		return currentScrambleString;
	}

	@Override
	public boolean isLastScrambleInList() {
		return scrambleNumber == session.getAttemptsCount();
	}

	@Override
	public ScrambleString getNext() {
		scrambleNumber++;
		if (isLastScrambleInList()) {
			return currentScrambleString = generateScramble();
		}
		else {
			return session.getSolution(scrambleNumber).getScrambleString();
		}
	}

	private ScrambleString generateScramble() {
		return getPuzzleType().isNullType() ? currentScrambleString : getPuzzleType().generateScramble();
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
