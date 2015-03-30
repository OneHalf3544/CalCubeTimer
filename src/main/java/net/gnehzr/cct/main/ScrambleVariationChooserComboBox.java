package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleVariation;
import net.gnehzr.cct.statistics.Profile;

/**
 * <p>
 * <p>
 * Created: 15.03.2015 14:14
 * <p>
 *
 * @author OneHalf
 */
public class ScrambleVariationChooserComboBox extends ScrambleChooserComboBox<ScrambleVariation> {

    public ScrambleVariationChooserComboBox(boolean icons, ScramblePluginManager scramblePluginManager, Configuration configuration) {
        super(icons, scramblePluginManager, configuration);
    }

    @Override
    protected ScrambleVariation[] getScramblesTypeArray(Profile profile) {
        return scramblePluginManager.getScrambleVariations();
    }
}
