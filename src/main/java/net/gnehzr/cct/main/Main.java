package net.gnehzr.cct.main;

import com.google.common.base.Joiner;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.main.context.ContextConfiguration;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.pushingpixels.lafwidget.LafWidget;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.util.Collection;

/**
 * <p>
 * <p>
 * Created: 13.11.2014 10:32
 * <p>
 *
 * @author OneHalf
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        LOG.info("start CalCubeTimer");

        DefaultUncaughtExceptionHandler.initialize();

        JDialog.setDefaultLookAndFeelDecorated(false);
        JFrame.setDefaultLookAndFeelDecorated(false);

        //The error messages are not internationalized because I want people to
        //be able to google the following messages
        if(args.length >= 2) {
            System.out.println("Too many arguments!");
            System.out.println("Usage: CALCubeTimer (profile directory)");
            return;
        }

        try {
            LOG.debug("create injector");
            context = new AnnotationConfigApplicationContext(ContextConfiguration.class);


        } catch (Exception e) {
            LOG.error("initialisation error", e);
            Utils.showErrorDialog(null, e, "initialisation error", "Couldn't start CCT!");
            Main.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                setLookAndFeel();

                context.getBean(CurrentProfileHolder.class).setSelectedProfile(
                        loadProfile(args, context.getBean(ProfileDao.class)));

                Collection<String> errors = context.getBean(Configuration.class).getStartupErrors();
                if (!errors.isEmpty()) {
                    Utils.showErrorDialog(null, Joiner.on('\n').join(errors), "Couldn't start CCT!");
                    Main.exit(1);
                }


                CalCubeTimerGui calCubeTimerFrame = context.getBean(CalCubeTimerGui.class);
                calCubeTimerFrame.loadXMLGUI();
                calCubeTimerFrame.setVisible(true);

            } catch (Exception e) {
                LOG.error("unexpected exception", e);
                Utils.showErrorDialog(null, e, "Couldn't start CCT!");
                Main.exit(1);
            }
        });
    }

    private static void setLookAndFeel() throws UnsupportedLookAndFeelException {

        JDialog.setDefaultLookAndFeelDecorated(true);
        JFrame.setDefaultLookAndFeelDecorated(true);
        UIManager.setLookAndFeel(new org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel());

        UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
        UIManager.put(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.TRUE);
    }

    private static Profile loadProfile(String[] args, ProfileDao profileDao) {
        if (args.length != 1) {
            return profileDao.loadLastProfile();
        }

        String startupProfile = args[0];
        Profile profileFromCommandLine = profileDao.loadProfile(startupProfile);
        if (profileFromCommandLine == null) {
            LOG.info("Couldn't find profile {}", startupProfile);
            return profileDao.getOrCreateGuestProfile();
        } else {
            return profileFromCommandLine;
        }
    }

    public static void exit(int code) {
        if (context == null) {
            System.exit(code);
        }
        try {
            context.getBean(CalCubeTimerModel.class).currentProfileHolder.saveProfileConfiguration(context.getBean(CalCubeTimerModel.class));
        } catch (Exception e){
            LOG.error("save profile error", e);
        }
        context.getBean(SessionFactory.class).close();
        System.exit(code);
    }
}
