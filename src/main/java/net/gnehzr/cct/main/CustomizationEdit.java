package net.gnehzr.cct.main;

import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.statistics.CCTUndoableEdit;

/**
* <p>
* <p>
* Created: 13.01.2015 1:32
* <p>
*
* @author OneHalf
*/
class CustomizationEdit implements CCTUndoableEdit {

    private final CalCubeTimerModel calCubeTimerModel;
    private PuzzleType oldCustom;
	private PuzzleType newCustom;
	private ScrambleChooserComboBox<PuzzleType> scrambleChooser;

	public CustomizationEdit(CalCubeTimerModel calCubeTimerModel, PuzzleType oldCustom, PuzzleType newCustom,
                             ScrambleChooserComboBox<PuzzleType> scrambleChooser) {
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
