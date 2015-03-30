package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScrambleList;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.dao.ProfileDao;

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
@Singleton
class ImportScramblesAction extends AbstractAction {

    private final CALCubeTimerFrame calCubeTimerFrame;
    private final ProfileDao profileDao;
    private final ScrambleImporter scrambleImporter;
    private final ScramblePluginManager scramblePluginManager;
    private final Configuration configuration;
    private final ScrambleList scramblesList;

    @Inject
    public ImportScramblesAction(CALCubeTimerFrame calCubeTimerFrame, ProfileDao profileDao,
                                 ScrambleImporter scrambleImporter, ScramblePluginManager scramblePluginManager,
                                 Configuration configuration, ScrambleList scramblesList) {
        this.calCubeTimerFrame = calCubeTimerFrame;
        this.profileDao = profileDao;
        this.scrambleImporter = scrambleImporter;
        this.scramblePluginManager = scramblePluginManager;
        this.configuration = configuration;
        this.scramblesList = scramblesList;
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
        putValue(Action.ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ScrambleImportDialog(calCubeTimerFrame, scrambleImporter,
                scramblesList.getCurrentScrambleCustomization(),
                scramblePluginManager, configuration);
    }

    @Inject
    public void registerAction(ActionMap actionMap) {
        actionMap.registerAction("importscrambles", this);
    }
}
