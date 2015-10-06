package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.dao.ProfileDao;

import java.util.List;

@Singleton
public class ScrambleImporter {

    private final CalCubeTimerGui calCubeTimerFrame;
    private final ProfileDao profileDao;
    private final Configuration configuration;
    private final ScramblePluginManager scramblePluginManager;
    private final CalCubeTimerModel model;

    @Inject
    public ScrambleImporter(CalCubeTimerGui calCubeTimerGui, CalCubeTimerModel model,
                            ProfileDao profileDao,
                            Configuration configuration,
                            ScramblePluginManager scramblePluginManager) {
        this.calCubeTimerFrame = calCubeTimerGui;
        this.profileDao = profileDao;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        this.model = model;
    }

    public void importScrambles(PuzzleType sc, List<ScrambleString> scramblePlugins, CALCubeTimerFrame calCubeTimer) {
        model.getScramblesList().setCurrentScrambleCustomization(sc);
        model.getScramblesList().importScrambles(scramblePlugins);
        calCubeTimer.getScrambleCustomizationComboBox().setSelectedItem(model.getScramblesList().getCurrentScrambleCustomization());
        calCubeTimer.updateScramble();
    }

    public void exportScramblesAction(Profile selectedProfile, ScrambleList scramblesList) {
        new ScrambleExportDialog(calCubeTimerFrame.getMainFrame(), scramblesList.getCurrentScrambleCustomization().getScrambleVariation(),
                scramblePluginManager, configuration, selectedProfile, profileDao);
    }

    public void importScrambles(ScrambleVariation sv, List<ScrambleString> scramblePlugins, Profile profile, ScrambleList scramblesList) {
        if(!((PuzzleType)calCubeTimerFrame.getScrambleCustomizationComboBox().getSelectedItem()).getScrambleVariation().equals(sv)) {
            scramblesList.setCurrentScrambleCustomization(scramblePluginManager.getCustomizationFromString(profile, "" + sv.toString()));
        }
        calCubeTimerFrame.getScrambleCustomizationComboBox().setSelectedItem(scramblesList.getCurrentScrambleCustomization());
        scramblesList.importScrambles(scramblePlugins);
        calCubeTimerFrame.updateScramble();
    }
}