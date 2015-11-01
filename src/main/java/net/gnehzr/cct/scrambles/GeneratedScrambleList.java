package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GeneratedScrambleList implements ScrambleList {

	private static final Logger LOG = LogManager.getLogger(GeneratedScrambleList.class);

	private final ScramblePluginManager scramblePluginManager;
	private SessionsList sessionsList;
	private int scrambleNumber;

	private ScrambleString currentScrambleString;

	public GeneratedScrambleList(SessionsList sessionsList, Configuration configuration) {
		this.sessionsList = sessionsList;
		this.scramblePluginManager = getPuzzleType().scramblePluginManager;

		this.scrambleNumber = 0;
		this.currentScrambleString = null;
		configuration.addConfigurationChangeListener(currentProfile -> generateNext());
	}

	@Override
	@NotNull
	public PuzzleType getPuzzleType() {
		return sessionsList.getCurrentSession().getPuzzleType();
	}

	public ScrambleString generateScrambleForCurrentSession() {
		LOG.info("generate scramble: {}", sessionsList.getCurrentSession());
		return this.currentScrambleString = getPuzzleType().generateScramble();
	}

	public void setScrambleLength(int scrambleLength) {
		scramblePluginManager.getScrambleVariation(getPuzzleType()).setLength(scrambleLength);
	}

	public void updateGeneratorGroup(String generatorGroup) {
		scramblePluginManager.setScrambleSettings(getPuzzleType(), scramblePluginManager.getScrambleVariation(getPuzzleType()).withGeneratorGroup(generatorGroup));
		scramblePluginManager.saveGeneratorToConfiguration(getPuzzleType());
	}

	@Override
	public int scramblesCount() {
		return sessionsList.getCurrentSession().getAttemptsCount();
	}

	@Override
	@NotNull
	public ScrambleString getCurrentScramble() {
		return currentScrambleString;
	}

	@Override
	public boolean isLastScrambleInList() {
		return getScrambleNumber() > sessionsList.getCurrentSession().getAttemptsCount();
	}

	@Override
	public ScrambleString generateNext() {
		scrambleNumber++;
		if (isLastScrambleInList()) {
			return generateScrambleForCurrentSession();
		}
		else {
			return sessionsList.getCurrentSession().getSolution(scrambleNumber).getScrambleString();
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
