package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationDialog;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.Metronome;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:20
 * <p>
 *
 * @author OneHalf
 */
@Component
class ShowConfigurationDialogAction extends AbstractNamedAction {

	private CALCubeTimerFrame cct;

	private ConfigurationDialog configurationDialog;

	@Autowired
	private Configuration configuration;
	@Autowired
	private ProfileDao profileDao;
	@Autowired
	private ScramblePluginManager scramblePluginManager;
	@Autowired
	private NumberSpeaker numberSpeaker;
	@Autowired
	private StackmatInterpreter stackmatInterpreter;
	@Autowired
	private Metronome metromone;
	@Autowired
	private JTable currentSessionSolutionsTable;

	@Autowired
	public ShowConfigurationDialogAction(CALCubeTimerFrame cct){
		super("showconfiguration");
		this.cct = cct;
		putValue(MNEMONIC_KEY, KeyEvent.VK_C);
		putValue(ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
	}

	@Override
	public void actionPerformed(ActionEvent e){
		showConfigurationDialog();
	}

	private void showConfigurationDialog() {
		cct.saveToConfiguration();
		if (configurationDialog == null) {
			configurationDialog = new ConfigurationDialog(
					cct, true, configuration, profileDao, scramblePluginManager,
					numberSpeaker, stackmatInterpreter, metromone,
					currentSessionSolutionsTable);
		}
		SwingUtilities.invokeLater(() -> {
			configurationDialog.syncGUIwithConfig(false);
			configurationDialog.setVisible(true);
		});
	}

}
