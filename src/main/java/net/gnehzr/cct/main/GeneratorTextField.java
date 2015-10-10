package net.gnehzr.cct.main;

import com.google.inject.Singleton;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.scrambles.ScrambleList;
import org.jvnet.lafwidget.LafWidget;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
* <p>
* <p>
* Created: 13.01.2015 1:31
* <p>
*
* @author OneHalf
*/
@Singleton
class GeneratorTextField extends JTextField {

    private CALCubeTimerFrame calCubeTimerFrame;

    @Inject
    private ScrambleList scramblesList;

    private String oldText;

    @Inject
    public GeneratorTextField(CALCubeTimerFrame calCubeTimerFrame) {
        this.calCubeTimerFrame = calCubeTimerFrame;
        addFocusListener(new MyFocusListener());
        addActionListener(new MyActionListener());
        setColumns(10);
        putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
        setToolTipText(StringAccessor.getString("CALCubeTimer.generatorgroup"));
    }

    @Override
    public void setText(String t) {
        setVisible(t != null);

        if(t != null && !t.equals(oldText)) {
            scramblesList.asGenerating().updateGeneratorGroup(t);
            calCubeTimerFrame.updateScramble();
        }
        oldText = t;

        super.setText(t);
    }

    private class MyFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            oldText = getText();
        }

        @Override
        public void focusLost(FocusEvent e) {
            setText(oldText);
        }
    }

    private class MyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setText(getText());
        }
    }
}
