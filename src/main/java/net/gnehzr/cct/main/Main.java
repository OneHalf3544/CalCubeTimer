package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.ScrambleSecurityManager;
import net.gnehzr.cct.scrambles.TimeoutJob;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.ProfileDao;
import org.apache.log4j.Logger;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.utils.LafConstants;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * <p>
 * <p>
 * Created: 13.11.2014 10:32
 * <p>
 *
 * @author OneHalf
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    static final ScrambleSecurityManager SCRAMBLE_SECURITY_MANAGER = new ScrambleSecurityManager(TimeoutJob.PLUGIN_LOADER);

    public static void main(String[] args) {
        LOG.info("start CalCubeTimer");
        DefaultUncaughtExceptionHandler.initialize();

        //The error messages are not internationalized because I want people to
        //be able to google the following messages
        if(args.length >= 2) {
            System.out.println("Too many arguments!");
            System.out.println("Usage: CALCubeTimer (profile directory)");
            return;
        }

        if(args.length == 1) {
            File startupProfileDir = new File(args[0]);
            if(!startupProfileDir.exists() || !startupProfileDir.isDirectory()) {
                LOG.info("Couldn't find directory " + startupProfileDir.getAbsolutePath());
            } else {
                Profile commandedProfile = ProfileDao.loadProfile(startupProfileDir);
                Configuration.setCommandLineProfile(commandedProfile);
                Configuration.setSelectedProfile(commandedProfile);
            }
        }

        System.setSecurityManager(SCRAMBLE_SECURITY_MANAGER);

        SwingUtilities.invokeLater(() -> {
            String errors = Configuration.getStartupErrors();
            if (!errors.isEmpty()) {
                Utils.showErrorDialog(null, errors, "Couldn't start CCT!");
                System.exit(1);
            }
            try {
                Configuration.loadConfiguration(Configuration.getSelectedProfile().getConfigurationFile());
            } catch (IOException e) {
                LOG.info("unexpected exception", e);
            }

            JDialog.setDefaultLookAndFeelDecorated(true);
            JFrame.setDefaultLookAndFeelDecorated(true);
            CALCubeTimer.setLookAndFeel();

            UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
            UIManager.put(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.TRUE);
            UIManager.put(LafWidget.ANIMATION_KIND, LafConstants.AnimationKind.NONE);
            UIManager.put(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);

            CALCubeTimer.cct = new CALCubeTimer();
            Configuration.addConfigurationChangeListener(CALCubeTimer.cct);
            CALCubeTimer.cct.setTitle("CCT " + CALCubeTimer.CCT_VERSION);
            CALCubeTimer.cct.setIconImage(CALCubeTimer.cubeIcon.getImage());
            CALCubeTimer.cct.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            CALCubeTimer.cct.setSelectedProfile(Configuration.getSelectedProfile()); //this will eventually cause sessionSelected() and configurationChanged() to be called
            CALCubeTimer.cct.setVisible(true);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    CALCubeTimer.cct.prepareForProfileSwitch();
                }
            });
        });
    }
}
