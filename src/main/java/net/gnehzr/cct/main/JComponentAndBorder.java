package net.gnehzr.cct.main;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.Objects;

/**
* <p>
* <p>
* Created: 13.01.2015 1:31
* <p>
*
* @author OneHalf
*/
class JComponentAndBorder {
    JComponent c;
    Border b;
    public JComponentAndBorder(JComponent c) {
        this.c = Objects.requireNonNull(c);
        this.b = c.getBorder();
    }
}
