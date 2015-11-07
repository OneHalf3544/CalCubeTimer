package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;

import java.util.List;

@Singleton
public class ScrambleImporter {

    private final CalCubeTimerGui calCubeTimerFrame;
    private final Configuration configuration;
    private final ScramblePluginManager scramblePluginManager;
    private final CalCubeTimerModel model;

    @Inject
    public ScrambleImporter(CalCubeTimerGui calCubeTimerGui, CalCubeTimerModel model,
                            Configuration configuration,
                            ScramblePluginManager scramblePluginManager) {
        this.calCubeTimerFrame = calCubeTimerGui;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        this.model = model;
    }

    public void importScrambles(PuzzleType puzzleType, List<ScrambleString> scramblePlugins, CALCubeTimerFrame calCubeTimer) {
        model.setScramblesList(new ImportedScrambleList(puzzleType, scramblePlugins, calCubeTimerFrame));
        calCubeTimer.getPuzzleTypeComboBox().setSelectedItem(model.getScramblesList().getPuzzleType());
        calCubeTimer.updateScramble();
    }

    public void exportScramblesAction(ScrambleList scramblesList) {
        new ScrambleExportDialog(
                calCubeTimerFrame.getMainFrame(),
                scramblesList.getPuzzleType(),
                scramblePluginManager,
                configuration)
                .setVisible(true);
    }

}