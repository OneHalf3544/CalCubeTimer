package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;

import static com.google.common.collect.Iterables.toArray;

/**
 * <p>
 * <p>
 * Created: 15.03.2015 14:16
 * <p>
 *
 * @author OneHalf
 */
public class ScrambleCustomizationChooserComboBox extends ScrambleChooserComboBox<ScrambleCustomization> {

    public ScrambleCustomizationChooserComboBox(boolean icons, ScramblePluginManager scramblePluginManager, Configuration configuration) {
        super(icons, scramblePluginManager, configuration);
    }

    @Override
    protected ScrambleCustomization[] getScramblesTypeArray(Profile profile) {
        return toArray(scramblePluginManager.getScrambleCustomizations(profile, false), ScrambleCustomization.class);
    }
}
