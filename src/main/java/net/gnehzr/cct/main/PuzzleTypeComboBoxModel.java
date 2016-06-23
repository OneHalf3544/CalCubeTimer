package net.gnehzr.cct.main;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;

/**
 * Created by OneHalf on 22.06.2016.
 */
public class PuzzleTypeComboBoxModel extends DefaultComboBoxModel<PuzzleType> {

	public PuzzleTypeComboBoxModel(ScramblePluginManager scramblePluginManager) {
        super(Iterables.toArray(scramblePluginManager.getPuzzleTypes(), PuzzleType.class));
    }
}
