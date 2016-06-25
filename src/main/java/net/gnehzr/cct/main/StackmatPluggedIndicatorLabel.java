package net.gnehzr.cct.main;

import org.springframework.stereotype.Service;

import javax.swing.*;
import java.awt.*;

/**
 * Created by OneHalf on 24.06.2016.
 */
@Service
public class StackmatPluggedIndicatorLabel extends JLabel {
    @Override
    public void updateUI() {
        Font f = UIManager.getFont("Label.font");
        setFont(f.deriveFont(f.getSize2D() * 2));
        super.updateUI();
    }
}
