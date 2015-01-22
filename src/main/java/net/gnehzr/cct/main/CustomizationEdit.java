package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.statistics.Statistics;

/**
* <p>
* <p>
* Created: 13.01.2015 1:32
* <p>
*
* @author OneHalf
*/
class CustomizationEdit implements Statistics.CCTUndoableEdit {

    private final CalCubeTimerModel calCubeTimerModel;
    private ScrambleCustomization oldCustom;
	private ScrambleCustomization newCustom;
	private ScrambleChooserComboBox<ScrambleCustomization> scrambleChooser;

	public CustomizationEdit(CalCubeTimerModel calCubeTimerModel, ScrambleCustomization oldCustom, ScrambleCustomization newCustom,
                             ScrambleChooserComboBox<ScrambleCustomization> scrambleChooser) {
        this.calCubeTimerModel = calCubeTimerModel;
        this.oldCustom = oldCustom;
        this.newCustom = newCustom;
		this.scrambleChooser = scrambleChooser;
	}

    @Override
    public void doEdit() {
        calCubeTimerModel.setCustomizationEditsDisabled(true);
        scrambleChooser.setSelectedItem(newCustom);
        calCubeTimerModel.setCustomizationEditsDisabled(false);
    }

    @Override
    public void undoEdit() {
        calCubeTimerModel.setCustomizationEditsDisabled(true);
        scrambleChooser.setSelectedItem(oldCustom);
        calCubeTimerModel.setCustomizationEditsDisabled(false);
    }
}
