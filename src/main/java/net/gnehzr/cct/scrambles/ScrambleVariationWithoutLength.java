package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;

/**
 * <p>
 * <p>
 * Created: 10.10.2015 21:39
 * <p>
 *
 * @author OneHalf
 */
class ScrambleVariationWithoutLength implements ScrambleSettings.WithoutLength {

	private ScrambleSettings scrambleSettings;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;

	public ScrambleVariationWithoutLength(ScrambleSettings scrambleSettings, Configuration configuration,
										  ScramblePluginManager scramblePluginManager) {
        this.scrambleSettings = scrambleSettings;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
	}

    @Override
	public ScrambleSettings withLength(int length) {
		ScrambleSettings scrambleSettings = new ScrambleSettings(
				configuration,
				this.scramblePluginManager,
				this.scrambleSettings.getGeneratorGroup(),
				0,
				this.scrambleSettings.getImage());
		scrambleSettings.setLength(length);
		return scrambleSettings;
	}

    @Override
	public String getGeneratorGroup() {
		return scrambleSettings.getGeneratorGroup();
	}

    @Override
	public ScrambleSettings.WithoutLength withGeneratorGroup(String generatorGroup) {
		return new ScrambleSettings(
				configuration,
				this.scramblePluginManager,
				generatorGroup,
				0,
				this.scrambleSettings.getImage())
				.withoutLength();
	}
}
