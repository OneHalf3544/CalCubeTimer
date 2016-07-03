package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.ScrambleImportDialog;
import net.gnehzr.cct.main.ScrambleImporter;
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
class ImportScramblesAction extends AbstractNamedAction {

    private final ScrambleImporter scrambleImporter;
    private final ScramblePluginManager scramblePluginManager;
    private final Configuration configuration;
    private ScrambleListHolder scrambleListHolder;

    @Autowired
    public ImportScramblesAction(ProfileDao profileDao,
                                 ScrambleImporter scrambleImporter,
                                 ScramblePluginManager scramblePluginManager,
                                 Configuration configuration,
                                 ScrambleListHolder scrambleListHolder) {
        super("importscrambles");
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
        new ScrambleImportDialog(null, scrambleImporter,
                scrambleListHolder.getPuzzleType(),
                scramblePluginManager, configuration);
    }
}
