package net.gnehzr.cct.main;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SessionsList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

/**
* <p>
* <p>
* Created: 17.01.2015 12:58
* <p>
*
* @author OneHalf
*/

class CctModelConfigChangeListener implements ConfigurationChangeListener {

    private static final Logger LOG = LogManager.getLogger(CctModelConfigChangeListener.class);

    private final TimingListener timingListener;
    private final CALCubeTimerFrame CALCubeTimerFrame;
    private final CurrentProfileHolder currentProfileHolder;
    private final ProfileDao profileDao;
    private final Configuration configuration;
    private final ScramblePluginManager scramblePluginManager;
    private final ActionMap actionMap;
    private final SessionsList sessionList;
    private final StackmatInterpreter stackmatInterpreter;

    public CctModelConfigChangeListener(TimingListener timingListener, CALCubeTimerFrame CALCubeTimerFrame,
                                        CurrentProfileHolder currentProfileHolder, ProfileDao profileDao,
                                        Configuration configuration, ScramblePluginManager scramblePluginManager,
                                        ActionMap actionMap, SessionsList sessionList,
                                        StackmatInterpreter stackmatInterpreter) {
        this.timingListener = timingListener;
        this.CALCubeTimerFrame = CALCubeTimerFrame;
        this.currentProfileHolder = currentProfileHolder;
        this.profileDao = profileDao;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        this.actionMap = actionMap;
        this.sessionList = sessionList;
        this.stackmatInterpreter = stackmatInterpreter;
    }

    @Override
    public void configurationChanged(Profile currentProfile) {
        LOG.info("process configuration changing (profile: {})", currentProfile.getName());
        actionMap.refreshActions();

        DefaultComboBoxModel<Profile> profileComboBoxModel = new DefaultComboBoxModel<>(
                Iterables.toArray(profileDao.getProfiles(), Profile.class));
        CALCubeTimerFrame.getMainFrame().profilesComboBox.setModel(profileComboBoxModel);

        CALCubeTimerFrame.getMainFrame().selectProfileWithoutListenersNotify(
                CALCubeTimerFrame.getMainFrame().profilesComboBox,
                currentProfileHolder.getSelectedProfile(),
                CALCubeTimerFrame.getMainFrame().profileComboboxListener);

        CALCubeTimerFrame.getLanguages().setSelectedItem(configuration.getDefaultLocale()); //this will force an update of the xml gui

        scramblePluginManager.reloadLengthsFromConfiguration(false);
        CALCubeTimerFrame.getMainFrame().getPuzzleTypeComboBox().setSelectedItem(sessionList.getCurrentSession().getPuzzleType());

        //we need to notify the stackmatinterpreter package because it has been rewritten to
        //avoid configuration entirely (which makes it easier to separate & use as a library)
        StackmatState.setInverted(
                configuration.getBoolean(VariableKey.INVERTED_MINUTES),
                configuration.getBoolean(VariableKey.INVERTED_SECONDS),
                configuration.getBoolean(VariableKey.INVERTED_HUNDREDTHS));

        stackmatInterpreter.initialize(
                configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE),
                configuration.getInt(VariableKey.MIXER_NUMBER),
                configuration.getBoolean(VariableKey.STACKMAT_ENABLED),
                configuration.getInt(VariableKey.SWITCH_THRESHOLD));

        configuration.setLong(VariableKey.MIXER_NUMBER, stackmatInterpreter.getSelectedMixerIndex());

        timingListener.stackmatChanged(); //force the stackmat label to refresh
    }
}
