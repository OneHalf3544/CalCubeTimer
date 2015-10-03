package net.gnehzr.cct.main;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.HibernateDaoSupport;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.utils.LafConstants;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.swing.*;

/**
 * <p>
 * <p>
 * Created: 13.11.2014 10:32
 * <p>
 *
 * @author OneHalf
 */
public class Main implements Module {

    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static Injector injector;

    @Override
    public void configure(Binder binder) {
        binder.bind(CalCubeTimerModel.class).to(CalCubeTimerModelImpl.class).asEagerSingleton();
        binder.bind(StackmatHandler.class).asEagerSingleton();

        binder.bind(CalCubeTimerGui.class).to(CALCubeTimerFrame.class).asEagerSingleton();

        binder.bind(NewSessionAction.class).asEagerSingleton();
        binder.bind(ToggleScramblePopupAction.class).asEagerSingleton();
        binder.bind(ExportScramblesAction.class).asEagerSingleton();
        binder.bind(ImportScramblesAction.class).asEagerSingleton();
        binder.bind(FullScreenDuringTimingChangeSettingAction.class).asEagerSingleton();

        binder.bind(TimingListener.class).to(TimingListenerImpl.class).asEagerSingleton();
        binder.bind(TimerLabel.class).annotatedWith(Names.named("timeLabel")).to(TimerLabel.class).asEagerSingleton();
        binder.bind(TimerLabel.class).annotatedWith(Names.named("bigTimersDisplay")).to(TimerLabel.class).asEagerSingleton();

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
        CalCubeTimerModel calCubeTimerModel;
        Configuration configuration;
        try {
            injector = Guice.createInjector(new Main());
            configuration = injector.getInstance(Configuration.class);
            profileDao = injector.getInstance(ProfileDao.class);
            calCubeTimerFrame = injector.getInstance(CALCubeTimerFrame.class);
            calCubeTimerModel = injector.getInstance(CalCubeTimerModel.class);

            if(args.length == 1) {
                String startupProfile = args[0];
                Profile commandedProfile = profileDao.loadProfile(startupProfile);
                if(commandedProfile == null) {
                    LOG.info("Couldn't find directory " + startupProfile);
                    profileDao.setSelectedProfile(profileDao.guestProfile);
                } else {
                    profileDao.setSelectedProfile(commandedProfile);
                }
            }

        } catch (Exception e) {
            LOG.error("initialisation error", e);
            Main.exit(1);
            throw new RuntimeException(e);
        }

        SwingUtilities.invokeLater(() -> {

            try {
                String errors = configuration.getStartupErrors();
                if (!errors.isEmpty()) {
                    Utils.showErrorDialog(null, errors, "Couldn't start CCT!");
                    Main.exit(1);
                }

                JDialog.setDefaultLookAndFeelDecorated(true);
                JFrame.setDefaultLookAndFeelDecorated(true);
                UIManager.setLookAndFeel(new org.jvnet.substance.skin.SubstanceModerateLookAndFeel());

                UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
                UIManager.put(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.TRUE);
                UIManager.put(LafWidget.ANIMATION_KIND, LafConstants.AnimationKind.NONE);
                UIManager.put(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);

                calCubeTimerFrame.setTitle("CCT " + CALCubeTimerFrame.CCT_VERSION);
                calCubeTimerFrame.setIconImage(CALCubeTimerFrame.CUBE_ICON.getImage());
                calCubeTimerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                calCubeTimerFrame.setSelectedProfile(profileDao.getSelectedProfile()); //this will eventually cause sessionSelected() and configurationChanged() to be called
                configuration.loadConfiguration(profileDao.getSelectedProfile());

                calCubeTimerFrame.setVisible(true);

                calCubeTimerFrame.loadXMLGUI();

                calCubeTimerModel.sessionSelected(calCubeTimerModel.getNextSession(calCubeTimerFrame));

                configuration.apply(profileDao.getSelectedProfile());
                calCubeTimerFrame.repaintTimes();
            }
            catch (Exception e) {
                LOG.error("unexpected exception", e);
                Main.exit(1);
            }
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
