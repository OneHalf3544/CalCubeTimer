package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.ProfileDao;

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

    public void importScrambles(ScrambleCustomization sc, List<Scramble> scrambles, CALCubeTimerFrame calCubeTimer) {
        model.getScramblesList().setScrambleCustomization(sc);
        model.getScramblesList().importScrambles(scrambles);
        calCubeTimer.getScrambleCustomizationComboBox().setSelectedItem(model.getScramblesList().getScrambleCustomization());
        calCubeTimer.updateScramble();
    }

    public void exportScramblesAction(Profile selectedProfile, ScrambleList scramblesList) {
        new ScrambleExportDialog(calCubeTimerFrame.getMainFrame(), scramblesList.getScrambleCustomization().getScrambleVariation(),
                scramblePluginManager, configuration, selectedProfile, profileDao);
    }

    public void importScrambles(ScrambleVariation sv, List<Scramble> scrambles, Profile profile, ScrambleList scramblesList) {
        if(!((ScrambleCustomization)calCubeTimerFrame.getScrambleCustomizationComboBox().getSelectedItem()).getScrambleVariation().equals(sv)) {
            scramblesList.setScrambleCustomization(scramblePluginManager.getCustomizationFromString(profile, "" + sv.toString()));
        }
        calCubeTimerFrame.getScrambleCustomizationComboBox().setSelectedItem(scramblesList.getScrambleCustomization());
        scramblesList.importScrambles(scrambles);
        calCubeTimerFrame.updateScramble();
    }
}