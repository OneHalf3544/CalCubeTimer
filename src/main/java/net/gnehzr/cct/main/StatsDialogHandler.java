package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.CCTFileChooser;
import net.gnehzr.cct.misc.JTextAreaWithHistory;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.statistics.CurrentSessionSolutionsTableModel;
import net.gnehzr.cct.statistics.SessionPuzzleStatistics.AverageType;
import net.gnehzr.cct.statistics.RollingAverageOf;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class StatsDialogHandler extends JDialog implements ActionListener, ChangeListener {
    private JButton emailButton = null;
    private JButton submitButton = null;
    private JButton saveButton = null;
    private JButton doneButton = null;
    private JTextAreaWithHistory textArea = null;
    private SundayContestDialog sundaySubmitter = null;
    private JSpinner sizeSpinner = null;
    private final Configuration configuration;

    public StatsDialogHandler(JFrame owner, Configuration configuration) {
        super(owner, true);
        this.configuration = configuration;

        textArea = new JTextAreaWithHistory();
        JScrollPane textScroller = new JScrollPane(textArea);

        emailButton = new JButton(StringAccessor.getString("StatsDialogHandler.email"));
        emailButton.addActionListener(this);

        sundaySubmitter = new SundayContestDialog(this, configuration);
        submitButton = new JButton(StringAccessor.getString("StatsDialogHandler.sundaycontest"));
        submitButton.addActionListener(this);

        saveButton = new JButton(StringAccessor.getString("StatsDialogHandler.save"));
        saveButton.addActionListener(this);

        doneButton = new JButton(StringAccessor.getString("StatsDialogHandler.done"));
        doneButton.addActionListener(this);

        sizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)) {
            @Override
            public void setValue(Object value) { //this makes the spinner fire statechanges even if the value remains the same
                if (value.equals(getValue())) {
                    fireStateChanged();
                } else
                    super.setValue(value);
            }
        };
        sizeSpinner.addChangeListener(this);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(emailButton);
        bottomPanel.add(submitButton);
        bottomPanel.add(saveButton);
        bottomPanel.add(doneButton);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(new JLabel(StringAccessor.getString("StatsDialogHandler.fontsize")));
        bottomPanel.add(sizeSpinner);

        getContentPane().add(textScroller, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.PAGE_END);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            setSize(configuration.getDimension(VariableKey.STATS_DIALOG_DIMENSION));
            sizeSpinner.setValue(configuration.getInt(VariableKey.STATS_DIALOG_FONT_SIZE));
            setLocationRelativeTo(getParent());
        } else
            configuration.setDimension(VariableKey.STATS_DIALOG_DIMENSION, getSize());
        super.setVisible(b);
    }

    public void syncWithStats(CurrentSessionSolutionsTableModel statsModel, AverageType type, RollingAverageOf avgNum) {
        sundaySubmitter.syncWithStats(statsModel.getCurrentSession().getSessionPuzzleStatistics(), type, avgNum);
        setTitle(StringAccessor.getString("StatsDialogHandler.detailedstats") + " " + type.toString());
        switch (type) {
            case CURRENT_ROLLING_AVERAGE:
                String currentAverageStatictic = configuration.getString(VariableKey.CURRENT_AVERAGE_STATISTICS, false);
                textArea.setText(new DynamicString(currentAverageStatictic, statsModel, null, configuration).toString(avgNum, null));
                break;
            case BEST_ROLLING_AVERAGE:
                String bestRA = configuration.getString(VariableKey.BEST_RA_STATISTICS, false);
                textArea.setText(new DynamicString(bestRA, statsModel, null, configuration).toString(avgNum, null));
                break;
            case SESSION_AVERAGE:
                String sessionAverage = configuration.getString(VariableKey.SESSION_STATISTICS, false);
                textArea.setText(new DynamicString(sessionAverage, statsModel, null, configuration).toString());
                break;
        }
    }

    public void promptToSaveStats() {
        CCTFileChooser fc = new CCTFileChooser(configuration);
        int choice = fc.showDialog(this, StringAccessor.getString("StatsDialogHandler.savestats"));
        File outputFile = null;
        if (choice == CCTFileChooser.APPROVE_OPTION) {
            outputFile = fc.getSelectedFile();
            boolean append = false;
            if (outputFile.exists()) {
                Object[] options = {StringAccessor.getString("StatsDialogHandler.overwrite"),
                        StringAccessor.getString("StatsDialogHandler.append"),
                        StringAccessor.getString("StatsDialogHandler.cancel")};
                int choiceOverwrite = JOptionPane.showOptionDialog(fc,
                        StringAccessor.getString("StatsDialogHandler.fileexists") + " " + outputFile.getName(),
                        StringAccessor.getString("StatsDialogHandler.fileexists"),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[2]);
                if (choiceOverwrite == JOptionPane.NO_OPTION)
                    append = true;
                else if (choiceOverwrite != JOptionPane.YES_OPTION)
                    return;
            }
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile, append), "UTF-8"))) {
                if (append) {
                    out.println();
                    out.println();
                }
                out.print(textArea.getText().replaceAll("\n", System.getProperty("line.separator")));
                Utils.showConfirmDialog(this,
                        StringAccessor.getString("StatsDialogHandler.successmessage") +
                                outputFile.getAbsolutePath());
            } catch (Exception e) {
                Utils.showErrorDialog(this, e);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == saveButton) {
            promptToSaveStats();
        } else if (source == submitButton) {
            sundaySubmitter.setVisible(true);
        } else if (source == doneButton) {
            this.setVisible(false);
        } else if (source == emailButton) {
            new EmailDialog(this, textArea.getText(), configuration).setVisible(true);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int fontSize = (Integer) sizeSpinner.getValue();
        configuration.setLong(VariableKey.STATS_DIALOG_FONT_SIZE, fontSize);
        textArea.setFont(textArea.getFont().deriveFont((float) fontSize));
    }
}
