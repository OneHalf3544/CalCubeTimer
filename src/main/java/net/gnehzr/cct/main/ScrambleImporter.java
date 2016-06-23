package net.gnehzr.cct.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.*;

import java.util.List;

@Service
public class ScrambleImporter {

    private final CALCubeTimerFrame calCubeTimerFrame;
    private final Configuration configuration;
    private final ScramblePluginManager scramblePluginManager;
    private final ScrambleListHolder scrambleListHolder;

    @Autowired
    public ScrambleImporter(CALCubeTimerFrame CALCubeTimerFrame, ScrambleListHolder scrambleListHolder,
                            Configuration configuration,
                            ScramblePluginManager scramblePluginManager) {
        this.calCubeTimerFrame = CALCubeTimerFrame;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        this.scrambleListHolder = scrambleListHolder;
    }

    public void importScrambles(PuzzleType puzzleType, List<ScrambleString> scramblePlugins, CALCubeTimerFrame calCubeTimer) {
        scrambleListHolder.setScrambleList(new ImportedScrambleList(puzzleType, scramblePlugins, calCubeTimerFrame));
        calCubeTimer.getPuzzleTypeComboBox().setSelectedItem(scrambleListHolder.getPuzzleType());
        calCubeTimer.updateScramble();
    }

    public void exportScramblesAction(PuzzleType puzzleType) {
        new ScrambleExportDialog(
                calCubeTimerFrame.getMainFrame(),
                puzzleType,
                scramblePluginManager,
                configuration)
                .setVisible(true);
    }

}