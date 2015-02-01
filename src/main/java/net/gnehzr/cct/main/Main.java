package net.gnehzr.cct.main;

import com.google.inject.*;
import com.google.inject.name.Names;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.dao.HibernateDaoSupport;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.umts.ircclient.IRCClient;
import net.gnehzr.cct.umts.ircclient.IRCClientGUI;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.utils.LafConstants;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;
import java.awt.event.ItemEvent;
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
    private static Injector injector;

    @Override
    public void configure(Binder binder) {
        binder.bind(CalCubeTimerModel.class).to(CalCubeTimerModelImpl.class).asEagerSingleton();
        binder.bind(StackmatHandler.class).asEagerSingleton();
        binder.bind(IRCClient.class).to(IRCClientGUI.class);

        binder.bind(CalCubeTimerGui.class).to(CALCubeTimerFrame.class).asEagerSingleton();

        binder.bind(NewSessionAction.class).asEagerSingleton();
        binder.bind(ToggleScramblePopupAction.class).asEagerSingleton();
        binder.bind(ConnectToIRCServerAction.class).asEagerSingleton();
        binder.bind(ExportScramblesAction.class).asEagerSingleton();
        binder.bind(ImportScramblesAction.class).asEagerSingleton();

        binder.bind(TimingListener.class).to(TimingListenerImpl.class);
        binder.bind(TimerLabel.class).annotatedWith(Names.named("timeLabel")).to(TimerLabel.class);
        binder.bind(TimerLabel.class).annotatedWith(Names.named("bigTimersDisplay")).to(TimerLabel.class);

        binder.bind(SessionFactory.class).toProvider(HibernateDaoSupport::configureSessionFactory);
    }

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


        ProfileDao profileDao;
        CALCubeTimerFrame calCubeTimerFrame;
        Configuration configuration;
        try {
            injector = Guice.createInjector(new Main());
            configuration = injector.getInstance(Configuration.class);
            profileDao = injector.getInstance(ProfileDao.class);
            calCubeTimerFrame = injector.getInstance(CALCubeTimerFrame.class);

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
        } catch (Exception e) {
            LOG.error("initialisation error", e);
            Main.exit(1);
            throw new RuntimeException();
        }

        SwingUtilities.invokeLater(() -> {

            try {
                String errors = configuration.getStartupErrors();
                if (!errors.isEmpty()) {
                    Utils.showErrorDialog(null, errors, "Couldn't start CCT!");
                    Main.exit(1);
                }
                try {
                    configuration.loadConfiguration(profileDao.getSelectedProfile());
                } catch (IOException e) {
                    LOG.info("unexpected exception", e);
                }

                JDialog.setDefaultLookAndFeelDecorated(true);
                JFrame.setDefaultLookAndFeelDecorated(true);
                calCubeTimerFrame.setLookAndFeel();

                UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
                UIManager.put(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.TRUE);
                UIManager.put(LafWidget.ANIMATION_KIND, LafConstants.AnimationKind.NONE);
                UIManager.put(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);

                calCubeTimerFrame.setTitle("CCT " + CALCubeTimerFrame.CCT_VERSION);
                calCubeTimerFrame.setIconImage(CALCubeTimerFrame.CUBE_ICON.getImage());
                calCubeTimerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                calCubeTimerFrame.setSelectedProfile(profileDao.getSelectedProfile()); //this will eventually cause sessionSelected() and configurationChanged() to be called
            }
            catch (Exception e) {
                LOG.error("unexpected exception", e);
                Main.exit(1);
            }

            calCubeTimerFrame.setVisible(true);

            calCubeTimerFrame.languagesComboboxListener.itemStateChanged(
                    new ItemEvent(calCubeTimerFrame.languages, 0, configuration.getDefaultLocale(), ItemEvent.SELECTED));

            calCubeTimerFrame.profileComboboxListener.itemStateChanged(
                            new ItemEvent(calCubeTimerFrame.profilesComboBox, 0, profileDao.getSelectedProfile(), ItemEvent.SELECTED));
        });
    }

    public static void exit(int code) {
        if (injector == null) {
            System.exit(code);
        }
        injector.getInstance(CalCubeTimerModel.class).prepareForProfileSwitch();
        injector.getInstance(SessionFactory.class).close();
        System.exit(code);
    }
}
