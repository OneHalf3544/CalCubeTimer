package net.gnehzr.cct.main.context;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.dao.HibernateDaoSupport;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import net.gnehzr.cct.main.Main;
import net.gnehzr.cct.main.Metronome;
import net.gnehzr.cct.main.actions.ShowDetailedStatisticsAction;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;


import java.time.Duration;

import static net.gnehzr.cct.statistics.RollingAverageOf.OF_12;
import static net.gnehzr.cct.statistics.RollingAverageOf.OF_5;
import static net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType.BEST_ROLLING_AVERAGE;
import static net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType.CURRENT_ROLLING_AVERAGE;
import static net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType.SESSION_AVERAGE;

/**
 * Created by OneHalf on 21.06.2016.
 */
@org.springframework.context.annotation.Configuration
@ComponentScan(value = {
        "net.gnehzr.cct"
})
public class ContextConfiguration {

    private static final Logger LOG = LogManager.getLogger();

    @Bean
    public TimerLabel timeLabel(Configuration configuration) {
        return new TimerLabel(configuration);
    }

    @Bean
    public TimerLabel bigTimersDisplay(Configuration configuration) {
        return new TimerLabel(configuration);
    }

    @Bean
    public Metronome metronome() {
        return Metronome.createTickTockTimer(Duration.ofSeconds(1));
    }

    @Bean
    private static SessionFactory sessionFactory() {
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

    @Bean
    public ShowDetailedStatisticsAction ra5StatisticsAction(CALCubeTimerFrame calCubeTimerFrame,
                                                            Configuration configuration,
                                                            SessionsList sessionsList) {
        return new ShowDetailedStatisticsAction(
                "currentaverage0", CURRENT_ROLLING_AVERAGE, OF_5, calCubeTimerFrame, configuration, sessionsList);
    }

    @Bean
    public ShowDetailedStatisticsAction ba5StatisticsAction(CALCubeTimerFrame calCubeTimerFrame,
                                                            Configuration configuration,
                                                            SessionsList sessionsList) {
        return new ShowDetailedStatisticsAction(
                "bestaverage0", BEST_ROLLING_AVERAGE, OF_5, calCubeTimerFrame, configuration, sessionsList);
    }

    @Bean
    public ShowDetailedStatisticsAction ra12StatisticsAction(CALCubeTimerFrame calCubeTimerFrame,
                                                             Configuration configuration,
                                                             SessionsList sessionsList) {
        return new ShowDetailedStatisticsAction(
                "currentaverage1", CURRENT_ROLLING_AVERAGE, OF_12, calCubeTimerFrame, configuration, sessionsList);
    }

    @Bean
    public ShowDetailedStatisticsAction ba12StatisticsAction(CALCubeTimerFrame calCubeTimerFrame,
                                                             Configuration configuration,
                                                             SessionsList sessionsList) {
        return new ShowDetailedStatisticsAction(
                "bestaverage1", BEST_ROLLING_AVERAGE, OF_12, calCubeTimerFrame, configuration, sessionsList);
    }

    @Bean
    public ShowDetailedStatisticsAction sessionAverageStatisticsAction(CALCubeTimerFrame calCubeTimerFrame,
                                                                       Configuration configuration,
                                                                       SessionsList sessionsList) {
        return new ShowDetailedStatisticsAction(
                "sessionaverage", SESSION_AVERAGE, null, calCubeTimerFrame, configuration, sessionsList);
    }
}
