package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleListHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.scrambles.ScramblePluginManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
* <p>
* <p>
* Created: 17.01.2015 10:43
* <p>
*
* @author OneHalf
*/
@Service
class ImportScramblesAction extends AbstractAction {

    private final CALCubeTimerFrame calCubeTimerFrame;
    private final ScrambleImporter scrambleImporter;
    private final ScramblePluginManager scramblePluginManager;
    private final Configuration configuration;
    private ScrambleListHolder scrambleListHolder;

    @Autowired
    public ImportScramblesAction(CALCubeTimerFrame calCubeTimerFrame, ProfileDao profileDao,
                                 ScrambleImporter scrambleImporter,
                                 ScramblePluginManager scramblePluginManager,
                                 Configuration configuration,
                                 ScrambleListHolder scrambleListHolder) {
        this.calCubeTimerFrame = calCubeTimerFrame;
        this.scrambleImporter = scrambleImporter;
        this.scramblePluginManager = scramblePluginManager;
        this.configuration = configuration;
        this.scrambleListHolder = scrambleListHolder;
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
        putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ScrambleImportDialog(calCubeTimerFrame, scrambleImporter,
                scrambleListHolder.getScramblesList().getPuzzleType(),
                scramblePluginManager, configuration);
    }

    @Autowired
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction("importscrambles", this);
    }
}
