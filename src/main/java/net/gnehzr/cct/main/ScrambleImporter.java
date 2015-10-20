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

    public void importScrambles(PuzzleType puzzleType, List<ScrambleString> scramblePlugins, CALCubeTimerFrame calCubeTimer) {
        model.setScramblesList(new ImportedScrambleList(puzzleType, scramblePlugins, calCubeTimerFrame));
        calCubeTimer.getScrambleCustomizationComboBox().setSelectedItem(model.getScramblesList().getPuzzleType());
        calCubeTimer.updateScramble();
    }

    public void exportScramblesAction(Profile selectedProfile, ScrambleList scramblesList) {
        new ScrambleExportDialog(calCubeTimerFrame.getMainFrame(), scramblesList.getPuzzleType(),
                scramblePluginManager, configuration, selectedProfile);
    }

}