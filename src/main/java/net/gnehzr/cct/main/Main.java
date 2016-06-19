package net.gnehzr.cct.main;

import com.google.common.base.Joiner;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.ConfigurationDao;
import net.gnehzr.cct.dao.HibernateDaoSupport;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.dynamicGUI.DynamicStringSettableManger;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;
import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import java.time.Duration;
import java.util.Collection;

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

    private final SessionFactory sessionFactory;

    public Main(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(CalCubeTimerModel.class).to(CalCubeTimerModelImpl.class).asEagerSingleton();
        binder.bind(CurrentProfileHolder.class).to(CalCubeTimerModelImpl.class).asEagerSingleton();
        binder.bind(CalCubeTimerGui.class).to(CALCubeTimerFrame.class).asEagerSingleton();

        binder.bind(Configuration.class).asEagerSingleton();
        binder.bind(ConfigurationDao.class).asEagerSingleton();
        binder.bind(StackmatHandler.class).asEagerSingleton();

        binder.bind(ActionMap.ToggleFullscreenTimingAction.class).asEagerSingleton();
        binder.bind(AddTimeAction.class).asEagerSingleton();
        binder.bind(ExportScramblesAction.class).asEagerSingleton();
        binder.bind(FullScreenDuringTimingChangeSettingAction.class).asEagerSingleton();
        binder.bind(ImportScramblesAction.class).asEagerSingleton();
        binder.bind(NewSessionAction.class).asEagerSingleton();
        binder.bind(ToggleScramblePopupAction.class).asEagerSingleton();

        binder.bind(TimerLabel.class).annotatedWith(Names.named("timeLabel")).to(TimerLabel.class).asEagerSingleton();
        binder.bind(TimerLabel.class).annotatedWith(Names.named("bigTimersDisplay")).to(TimerLabel.class).asEagerSingleton();

        binder.bind(Metronome.class).toProvider(() -> Metronome.createTickTockTimer(Duration.ofSeconds(1))).asEagerSingleton();
        binder.bind(TimingListener.class).to(TimingListenerImpl.class).asEagerSingleton();
        binder.bind(SolvingProcessListener.class).to(TimingListenerImpl.class).asEagerSingleton();
        binder.bind(SolvingProcess.class).to(SolvingProcessImpl.class).asEagerSingleton();

        binder.bind(SessionsList.class).asEagerSingleton();
        binder.bind(DynamicStringSettableManger.class).asEagerSingleton();
        binder.bind(ScramblePluginManager.class).asEagerSingleton();

        binder.bind(SessionFactory.class).toInstance(sessionFactory);

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

        SessionFactory sessionFactory = createSessionFactory();

        try {
            LOG.debug("create injector");
            injector = Guice.createInjector(new Main(sessionFactory));


        } catch (Exception e) {
            LOG.error("initialisation error", e);
            Utils.showErrorDialog(null, e, "initialisation error", "Couldn't start CCT!");
            Main.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                setLookAndFeel();

                injector.getInstance(CalCubeTimerModel.class).setSelectedProfile(
                        loadProfile(args, injector.getInstance(ProfileDao.class)));

                Collection<String> errors = injector.getInstance(Configuration.class).getStartupErrors();
                if (!errors.isEmpty()) {
                    Utils.showErrorDialog(null, Joiner.on('\n').join(errors), "Couldn't start CCT!");
                    Main.exit(1);
                }


                CalCubeTimerGui calCubeTimerFrame = injector.getInstance(CalCubeTimerGui.class);
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

    @Nullable
    private static SessionFactory createSessionFactory() {
        LOG.debug("create SessionFactory");
        SessionFactory sessionFactory = null;
        try {
            sessionFactory = HibernateDaoSupport.configureSessionFactory();

        } catch (Exception e) {
            LOG.fatal("cannot connect to database", e);
            Utils.showErrorDialog(null, e, "Cannot connect to database. Second instance running?", "Couldn't start CCT!");
            Main.exit(1);
        }
        return sessionFactory;
    }

    public static void exit(int code) {
        if (injector == null) {
            System.exit(code);
        }
        try {
            injector.getInstance(CalCubeTimerModel.class).saveProfileConfiguration();
        } catch (Exception e){
            LOG.error("save profile error", e);
        }
        injector.getInstance(SessionFactory.class).close();
        System.exit(code);
    }
}
