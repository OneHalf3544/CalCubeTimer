package net.gnehzr.cct.main;

import com.google.inject.*;
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
public class Main implements Module {

    private static final Logger LOG = Logger.getLogger(Main.class);

    static ScrambleSecurityManager SCRAMBLE_SECURITY_MANAGER;

    @Override
    public void configure(Binder binder) {
    }

    public static void main(String[] args) {
        LOG.info("start CalCubeTimer");
        DefaultUncaughtExceptionHandler.initialize();

        Injector injector = Guice.createInjector(new Main());

        //The error messages are not internationalized because I want people to
        //be able to google the following messages
        if(args.length >= 2) {
            System.out.println("Too many arguments!");
            System.out.println("Usage: CALCubeTimer (profile directory)");
            return;
        }

        Configuration configuration = injector.getInstance(Configuration.class);
        ProfileDao profileDao = injector.getInstance(ProfileDao.class);
        CALCubeTimer calCubeTimer = injector.getInstance(CALCubeTimer.class);

        if(args.length == 1) {
            File startupProfileDir = new File(args[0]);
            if(!startupProfileDir.exists() || !startupProfileDir.isDirectory()) {
                LOG.info("Couldn't find directory " + startupProfileDir.getAbsolutePath());
            } else {

                Profile commandedProfile = profileDao.loadProfile(startupProfileDir);
                configuration.setCommandLineProfile(commandedProfile);
                profileDao.setSelectedProfile(commandedProfile);
            }
        }

        SCRAMBLE_SECURITY_MANAGER = new ScrambleSecurityManager(TimeoutJob.PLUGIN_LOADER, configuration);
        System.setSecurityManager(SCRAMBLE_SECURITY_MANAGER);

        SwingUtilities.invokeLater(() -> {
            String errors = configuration.getStartupErrors();
            if (!errors.isEmpty()) {
                Utils.showErrorDialog(null, errors, "Couldn't start CCT!");
                System.exit(1);
            }
            try {
                configuration.loadConfiguration(profileDao.getSelectedProfile().getConfigurationFile());
            } catch (IOException e) {
                LOG.info("unexpected exception", e);
            }

            JDialog.setDefaultLookAndFeelDecorated(true);
            JFrame.setDefaultLookAndFeelDecorated(true);
            calCubeTimer.setLookAndFeel();

            UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
            UIManager.put(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.TRUE);
            UIManager.put(LafWidget.ANIMATION_KIND, LafConstants.AnimationKind.NONE);
            UIManager.put(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);

            calCubeTimer.setTitle("CCT " + CALCubeTimer.CCT_VERSION);
            calCubeTimer.setIconImage(CALCubeTimer.cubeIcon.getImage());
            calCubeTimer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            calCubeTimer.setSelectedProfile(profileDao.getSelectedProfile()); //this will eventually cause sessionSelected() and configurationChanged() to be called

            calCubeTimer.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(calCubeTimer::prepareForProfileSwitch));
        });
    }

}
