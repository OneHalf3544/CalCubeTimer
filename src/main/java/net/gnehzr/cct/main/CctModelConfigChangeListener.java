package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.dao.ProfileDao;

import javax.swing.*;
import java.util.List;

/**
* <p>
* <p>
* Created: 17.01.2015 12:58
* <p>
*
* @author OneHalf
*/
@Singleton
class CctModelConfigChangeListener implements ConfigurationChangeListener {

    @Inject
    private TimingListener timingListener;

    private final CalCubeTimerGui calCubeTimerGui;
    private CalCubeTimerModel calCubeTimerModel;
    private final ProfileDao profileDao;
    private final Configuration configuration;
    private final ScramblePluginManager scramblePluginManager;
    private final ActionMap actionMap;

    @Inject
    public CctModelConfigChangeListener(CalCubeTimerGui calCubeTimerGui,
                                        CalCubeTimerModel calCubeTimerModel, ProfileDao profileDao,
                                        Configuration configuration, ScramblePluginManager scramblePluginManager, ActionMap actionMap) {
        this.calCubeTimerGui = calCubeTimerGui;
        this.calCubeTimerModel = calCubeTimerModel;
        this.profileDao = profileDao;
        this.configuration = configuration;
        this.scramblePluginManager = scramblePluginManager;
        this.actionMap = actionMap;
    }

    @Override
    public void configurationChanged(Profile currentProfile) {
        //Configuration for the cctbot to work.
        actionMap.refreshActions();

        List<Profile> profiles = profileDao.getProfiles();
        calCubeTimerGui.getMainFrame().profilesComboBox.setModel(new DefaultComboBoxModel<>(profiles.toArray(new Profile[profiles.size()])));
        calCubeTimerGui.getMainFrame().selectProfileWithoutListenersNotify(calCubeTimerGui.getMainFrame().profilesComboBox, calCubeTimerModel.getSelectedProfile(), calCubeTimerGui.getMainFrame().profileComboboxListener);
        calCubeTimerGui.getLanguages().setSelectedItem(configuration.getDefaultLocale()); //this will force an update of the xml gui
        calCubeTimerGui.getMainFrame().updateWatermark();

        scramblePluginManager.reloadLengthsFromConfiguration(false);
        ScrambleCustomization newCustom = scramblePluginManager.getCurrentScrambleCustomization(currentProfile);
        calCubeTimerGui.getMainFrame().getScrambleCustomizationComboBox().setSelectedItem(newCustom);

        //we need to notify the stackmatinterpreter package because it has been rewritten to
        //avoid configuration entirely (which makes it easier to separate & use as a library)
        StackmatState.setInverted(
                configuration.getBoolean(VariableKey.INVERTED_MINUTES),
                configuration.getBoolean(VariableKey.INVERTED_SECONDS),
                configuration.getBoolean(VariableKey.INVERTED_HUNDREDTHS));

        calCubeTimerModel.getStackmatInterpreter().initialize(
                configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE),
                configuration.getInt(VariableKey.MIXER_NUMBER),
                configuration.getBoolean(VariableKey.STACKMAT_ENABLED),
                configuration.getInt(VariableKey.SWITCH_THRESHOLD));

        configuration.setLong(VariableKey.MIXER_NUMBER, calCubeTimerModel.getStackmatInterpreter().getSelectedMixerIndex());

        timingListener.stackmatChanged(); //force the stackmat label to refresh
    }
}
