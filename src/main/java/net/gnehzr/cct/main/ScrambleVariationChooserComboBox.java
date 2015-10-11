package net.gnehzr.cct.main;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;

/**
 * <p>
 * <p>
 * Created: 15.03.2015 14:14
 * <p>
 *
 * @author OneHalf
 */
public class ScrambleVariationChooserComboBox extends ScrambleChooserComboBox<PuzzleType> {

    public ScrambleVariationChooserComboBox(boolean icons, ScramblePluginManager scramblePluginManager, Configuration configuration) {
        super(icons, scramblePluginManager, configuration);
    }

    @Override
    protected PuzzleType[] getScramblesTypeArray(Profile profile) {
        return Iterables.toArray(scramblePluginManager.getPuzzleTypes(), PuzzleType.class);
    }
}
