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

	private ScrambleString currentScrambleString;

	public GeneratedScrambleList(SessionsList sessionsList, Configuration configuration) {
		this.sessionsList = sessionsList;
		this.scramblePluginManager = getPuzzleType().scramblePluginManager;

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

	@Deprecated
	public void setScrambleLength(int scrambleLength) {
		scramblePluginManager.getScrambleVariation(getPuzzleType()).setLength(scrambleLength);
	}

	@Deprecated
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
	public ScrambleString generateNext() {
		return generateScrambleForCurrentSession();
	}

	@Override
	public boolean isGenerating() {
		return true;
	}

	@Override
	public int getScrambleNumber() {
		return sessionsList.getCurrentSession().getAttemptsCount();
	}
}
