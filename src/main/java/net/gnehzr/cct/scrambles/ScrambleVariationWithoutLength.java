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
class ScrambleVariationWithoutLength implements ScrambleVariation.WithoutLength {

	private ScrambleVariation scrambleVariation;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;

	public ScrambleVariationWithoutLength(ScrambleVariation scrambleVariation, Configuration configuration,
										  ScramblePluginManager scramblePluginManager) {
        this.scrambleVariation = scrambleVariation;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
	}

    @Override
	public ScrambleVariation withLength(int length) {
		ScrambleVariation scrambleVariation = new ScrambleVariation(
				this.scrambleVariation.getPlugin(),
				this.scrambleVariation.getName(),
				configuration,
				this.scramblePluginManager,
				this.scrambleVariation.getGeneratorGroup());
		scrambleVariation.setLength(length);
		return scrambleVariation;
	}

    @Override
	public String getName() {
		return scrambleVariation.getName();
	}

    @Override
	public String getGeneratorGroup() {
		return scrambleVariation.getGeneratorGroup();
	}

    @Override
	public ScrambleVariation.WithoutLength withGeneratorGroup(String generatorGroup) {
		return new ScrambleVariation(
				this.scrambleVariation.getPlugin(),
				this.scrambleVariation.getName(),
				configuration,
				this.scramblePluginManager,
				generatorGroup)
				.withoutLength();
	}
}
