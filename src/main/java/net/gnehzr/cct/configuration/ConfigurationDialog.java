package net.gnehzr.cct.configuration;

import com.google.common.collect.Iterables;
import net.gnehzr.cct.configuration.SolveTypeTagEditorTableModel.TypeAndName;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.main.Metronome;
import net.gnehzr.cct.misc.*;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.ProfileEditor;
import net.gnehzr.cct.scrambles.DefaultPuzzleViewComponent;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.SolveType;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import say.swing.JFontChooser;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigurationDialog extends JDialog {

	private static final float DISPLAY_FONT_SIZE = 20;
	private static final String[] FONT_SIZES = { "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36" };
	private final Configuration configuration;
	private final ProfileDao profileDao;
	private final ScramblePluginManager scramblePluginManager;
	private final NumberSpeaker numberSpeaker;
	private final CalCubeTimerModel cubeTimerModel;

	private final MouseListener mouseListener = new MouseAdapter() {

		@Override
		public void mouseClicked (MouseEvent e){
			Object source = e.getSource();

			if (source instanceof JColorComponent) {
				JColorComponent label = (JColorComponent) source;

				if (source == timerFontChooser || source == scrambleFontChooser) {
					String toDisplay = null;
					Font f;
					Color bg, fg;
					if (source == timerFontChooser) {
						f = configuration.getFont(VariableKey.TIMER_FONT, true).deriveFont(DISPLAY_FONT_SIZE);
						toDisplay = "0123456789:.,";
						bg = configuration.getColorNullIfInvalid(VariableKey.TIMER_BG, true);
						fg = configuration.getColor(VariableKey.TIMER_FG, true);
					} else { //scrambleFontChooser
						f = configuration.getFont(VariableKey.SCRAMBLE_FONT, true);
						bg = configuration.getColor(VariableKey.SCRAMBLE_UNSELECTED, true);
						fg = configuration.getColor(VariableKey.SCRAMBLE_SELECTED, true);
					}

					int maxFontSize = configuration.getInt(VariableKey.MAX_FONTSIZE);
					JFontChooser font = new JFontChooser(FONT_SIZES, f, source == scrambleFontChooser, maxFontSize, toDisplay, bg, fg, source == timerFontChooser);
					font.setSelectedFont(label.getFont());
					font.setFontForeground(label.getForeground());
					font.setFontBackground(label.getBackground());
					if (font.showDialog(ConfigurationDialog.this) == JFontChooser.OK_OPTION) {
						Font selected = font.getSelectedFont();
						selected = selected.deriveFont(Math.min(maxFontSize, selected.getSize2D()));
						label.setFont(selected);
						label.setOpaque(false);
						label.setBackground(font.getSelectedBG()); //this must occur before call to setForeground
						label.setForeground(font.getSelectedFG());
						pack();
					}
				} else {
					Color selected = JColorChooser.showDialog(ConfigurationDialog.this, StringAccessor.getString("ConfigurationDialog.choosecolor"), label.getBackground());
					if (selected != null)
						label.setBackground(selected);
				}
			}
		}
	};

	private interface SyncGUIListener extends ActionListener {
		@Override
		default void actionPerformed(ActionEvent e) {
			//this happens if the event was fired by a real button, which means we want to reset with the defaults
			syncGUIWithConfig(true);
		}

		void syncGUIWithConfig(boolean defaults);
	}
	private List<SyncGUIListener> resetListeners = new ArrayList<>();
	private ComboItem[] items;
	private StackmatInterpreter stackmat;
	private Metronome tickTock;
	JTable timesTable;

	public ConfigurationDialog(JFrame parent, boolean modal, Configuration configuration, ProfileDao profileDao,
							   ScramblePluginManager scramblePluginManager,
							   NumberSpeaker numberSpeaker, CalCubeTimerModel cubeTimerModel,
							   StackmatInterpreter stackmat, Metronome tickTock, JTable timesTable) {
		super(parent, modal);
		this.configuration = configuration;
		this.profileDao = profileDao;
		this.scramblePluginManager = scramblePluginManager;
		this.numberSpeaker = numberSpeaker;
		this.cubeTimerModel = cubeTimerModel;
		this.stackmat = stackmat;
		this.tickTock = tickTock;
		this.timesTable = timesTable;
		this.puzzleTypeListModel = new PuzzleTypeListModel();
		this.puzzlesCellEditor = new PuzzleSettingsTableEditor(configuration, scramblePluginManager);
		profilesModel = new ProfileListModel(this.profileDao);
		createGUI();
		setLocationRelativeTo(parent);
	}
	
	//this will return a jpanel with all the components laid out according to boxLayout
	//if boxLayout == null, then the jpanel uses a flowlayout
	private JPanel sideBySide(Integer boxLayout, Component... components) {
		JPanel panel = new JPanel();
		if(boxLayout != null)
			panel.setLayout(new BoxLayout(panel, boxLayout));
		for(Component c : components)
			panel.add(c);
		return panel;
	}
	private JButton getResetButton(boolean vertical) {
		String text = StringAccessor.getString("ConfigurationDialog.reset");
		if(vertical && !text.isEmpty()) {
			String t = "";
			for(int i = 0; i < text.length(); i++) {
				t += "<br>" + text.substring(i, i + 1); //this is written this way to deal with unicode characters that don't fit in java char values
			}
			text = "<html><center>" + t.substring(4) + "</center></html>";
		}
		JButton reset = new JButton(text);
		reset.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, true);
		return reset;
	}

	private JButton applyButton, saveButton = null;
	private JButton cancelButton = null;
	private JButton resetAllButton = null;
	private JComboBox<Profile> profiles = null;

	private void createGUI() {
		JPanel pane = new JPanel(new BorderLayout());
		setContentPane(pane);

		JTabbedPane tabbedPane = new JTabbedPane() { // this will automatically give tabs numeric mnemonics
			@Override
			public void addTab(String title, Component component) {
				int currTab = this.getTabCount();
				super.addTab((currTab + 1) + " " + title, component);
				if (currTab < 9)
					super.setMnemonicAt(currTab, Character.forDigit(currTab + 1, 10));
			}
		};
		pane.add(tabbedPane, BorderLayout.CENTER);

		JComponent tab = makeStandardOptionsPanel1();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.options"), tab);

		tab = makeStandardOptionsPanel2();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.moreoptions"), tab);

		tab = makeScrambleTypeOptionsPanel();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.scramcustomizations"), tab);

		tab = makeStackmatOptionsPanel();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.stackmatsettings"), tab);

		tab = makeStatisticsPanels();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.statistics"), tab);
		
		tab = makePuzzleColorsPanel();
		tabbedPane.addTab(StringAccessor.getString("ConfigurationDialog.colors"), tab);

		applyButton = new JButton(StringAccessor.getString("ConfigurationDialog.apply"));
		applyButton.setMnemonic(KeyEvent.VK_A);
		applyButton.addActionListener(this::actionPerformed);

		saveButton = new JButton(StringAccessor.getString("ConfigurationDialog.save"));
		saveButton.setMnemonic(KeyEvent.VK_S);
		saveButton.addActionListener(this::actionPerformed);

		cancelButton = new JButton(StringAccessor.getString("ConfigurationDialog.cancel"));
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(this::actionPerformed);

		resetAllButton = new JButton(StringAccessor.getString("ConfigurationDialog.resetall"));
		resetAllButton.setMnemonic(KeyEvent.VK_R);
		resetAllButton.addActionListener(this::actionPerformed);
		
		profiles = new JComboBox<>();
		profiles.addItemListener(this::itemStateChanged);

		pane.add(sideBySide(BoxLayout.LINE_AXIS, profiles, Box.createHorizontalGlue(), resetAllButton, Box.createRigidArea(new Dimension(30, 0)), applyButton,
				saveButton, cancelButton, Box.createHorizontalGlue()), BorderLayout.PAGE_END);

		setResizable(false);
		pack();
	}

	JCheckBox clockFormat;
	JCheckBox promptForNewTime;
	JCheckBox scramblePopup, sideBySideScramble;
	JCheckBox inspectionCountdown;
	JCheckBox speakInspection;
	JCheckBox speakTimes;
	JCheckBox splits;
	JCheckBox metronome;
	JSpinner minSplitTime;
	TickerSlider metronomeDelay = null;
	JColorComponent bestRA;
	JColorComponent currentAverage;
	JColorComponent bestTime;
	JColorComponent worstTime = null;
	private JPanel desktopPanel;
	private JButton refreshDesktops;
	JComboBox voices;
	SolveTypeTagEditorTableModel tagsModel;

	private JPanel makeStandardOptionsPanel1() {
		JPanel options = new JPanel();
		JPanel colorPanel = new JPanel(new GridLayout(0, 1, 0, 5));
		options.add(colorPanel);

		JPanel rightPanel = new JPanel(new GridLayout(0, 1));
		options.add(rightPanel);

		clockFormat = new JCheckBox(StringAccessor.getString("ConfigurationDialog.clockformat"));
		clockFormat.setMnemonic(KeyEvent.VK_U);
		rightPanel.add(clockFormat);

		promptForNewTime = new JCheckBox(StringAccessor.getString("ConfigurationDialog.promptnewtime"));
		promptForNewTime.setMnemonic(KeyEvent.VK_P);
		rightPanel.add(promptForNewTime);

		scramblePopup = new JCheckBox(StringAccessor.getString("ConfigurationDialog.scramblepopup"));
		sideBySideScramble = new JCheckBox(StringAccessor.getString("ConfigurationDialog.sidebysidescramble"));
		rightPanel.add(sideBySide(null, scramblePopup, sideBySideScramble));
		
		inspectionCountdown = new JCheckBox(StringAccessor.getString("ConfigurationDialog.inspection"));
		inspectionCountdown.addItemListener(this::itemStateChanged);
		speakInspection = new JCheckBox(StringAccessor.getString("ConfigurationDialog.readinspection"));
		JPanel sideBySide = new JPanel();
		sideBySide.add(inspectionCountdown);
		sideBySide.add(speakInspection);
		rightPanel.add(sideBySide);
		
		speakTimes = new JCheckBox(StringAccessor.getString("ConfigurationDialog.readtimes"));
		voices = new JComboBox<>(numberSpeaker.getSpeakers());
		sideBySide = new JPanel();
		sideBySide.add(speakTimes);
		sideBySide.add(new JLabel(StringAccessor.getString("ConfigurationDialog.voicechoice")));
		sideBySide.add(voices);
		rightPanel.add(sideBySide);

		bestRA = new JColorComponent(StringAccessor.getString("ConfigurationDialog.bestra"));
		bestRA.addMouseListener(mouseListener);
		colorPanel.add(bestRA);
		
		currentAverage = new JColorComponent(StringAccessor.getString("ConfigurationDialog.currentaverage"));
		currentAverage.addMouseListener(mouseListener);
		colorPanel.add(currentAverage);

		bestTime = new JColorComponent(StringAccessor.getString("ConfigurationDialog.besttime"));
		bestTime.addMouseListener(mouseListener);
		colorPanel.add(bestTime);

		worstTime = new JColorComponent(StringAccessor.getString("ConfigurationDialog.worsttime"));
		worstTime.addMouseListener(mouseListener);
		colorPanel.add(worstTime);

		desktopPanel = new JPanel(); //this gets populated in refreshDesktops()
		refreshDesktops = new JButton(StringAccessor.getString("ConfigurationDialog.refresh"));
		refreshDesktops.addActionListener(this::actionPerformed);

		DraggableJTable profilesTable = new DraggableJTable(configuration, true, false);
		profilesTable.refreshStrings(StringAccessor.getString("ConfigurationDialog.addprofile"));
		profilesTable.getTableHeader().setReorderingAllowed(false);
		profilesTable.setModel(profilesModel);
		profilesTable.setDefaultEditor(Profile.class, new ProfileEditor(StringAccessor.getString("ConfigurationDialog.newprofile"), profilesModel, profileDao));
		JScrollPane profileScroller = new JScrollPane(profilesTable);
		profileScroller.setPreferredSize(new Dimension(150, 0));
		
		DraggableJTable tagsTable = new DraggableJTable(configuration, true, false);
		tagsTable.getTableHeader().setReorderingAllowed(false);
		tagsModel = new SolveTypeTagEditorTableModel(tagsTable, configuration);
		tagsTable.refreshStrings(StringAccessor.getString("ConfigurationDialog.addtag"));
		tagsTable.setDefaultEditor(TypeAndName.class, tagsModel.editor);
		tagsTable.setModel(tagsModel);
		JScrollPane tagScroller = new JScrollPane(tagsTable);
		tagScroller.setPreferredSize(new Dimension(100, 100));
		
		SyncGUIListener al = defaults -> {
            // makeStandardOptionsPanel1
            clockFormat.setSelected(configuration.getBoolean(VariableKey.CLOCK_FORMAT, defaults));
            promptForNewTime.setSelected(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME, defaults));
            scramblePopup.setSelected(configuration.getBoolean(VariableKey.SCRAMBLE_POPUP, defaults));
            sideBySideScramble.setSelected(configuration.getBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, defaults));
            inspectionCountdown.setSelected(configuration.getBoolean(VariableKey.COMPETITION_INSPECTION, defaults));
            speakInspection.setSelected(configuration.getBoolean(VariableKey.SPEAK_INSPECTION, defaults));
            speakInspection.setEnabled(inspectionCountdown.isSelected());
            bestRA.setBackground(configuration.getColor(VariableKey.BEST_RA, defaults));
            bestTime.setBackground(configuration.getColor(VariableKey.BEST_TIME, defaults));
            worstTime.setBackground(configuration.getColor(VariableKey.WORST_TIME, defaults));
            currentAverage.setBackground(configuration.getColor(VariableKey.CURRENT_AVERAGE, defaults));
            speakTimes.setSelected(configuration.getBoolean(VariableKey.SPEAK_TIMES, defaults));
            voices.setSelectedItem(numberSpeaker.getCurrentSpeaker());
            tagsModel.setTags(
                    SolveType.getSolveTypes(configuration.getStringArray(VariableKey.SOLVE_TAGS, defaults)),
                    /*todo*/ Collections.<SolveType>emptyList());

            refreshDesktops();
        };
		JButton reset = getResetButton(false);
		reset.addActionListener(al);
		resetListeners.add(al);
		
		return sideBySide(BoxLayout.PAGE_AXIS,
				Box.createVerticalGlue(),
				options,
				sideBySide(BoxLayout.LINE_AXIS, desktopPanel, profileScroller, tagScroller, Box.createHorizontalGlue(), reset),
				Box.createVerticalGlue());
	}

	JTextArea splitsKeySelector;
	JTextArea stackmatKeySelector1;
	JTextArea stackmatKeySelector2;
	JCheckBox stackmatEmulation;
	int splitkey;
	int sekey1;
	int sekey2;
	JCheckBox flashyWindow;
	JCheckBox isBackground;
	JTextField backgroundFile;
	JButton browse;
	JSlider opacity;
	JColorComponent scrambleFontChooser;
	JColorComponent timerFontChooser;

	private JPanel makeStandardOptionsPanel2() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		SpinnerNumberModel model = new SpinnerNumberModel(0.0, 0.0, null, .01);
		minSplitTime = new JSpinner(model);
		JSpinner.NumberEditor doubleModel = new JSpinner.NumberEditor(minSplitTime, "0.00");
		minSplitTime.setEditor(doubleModel);
		((JSpinner.DefaultEditor) minSplitTime.getEditor()).getTextField().setColumns(4);

		splits = new JCheckBox(StringAccessor.getString("ConfigurationDialog.splits"));
		splits.addActionListener(this::actionPerformed);

		splitsKeySelector = new JTextArea();
		splitsKeySelector.setColumns(10);
		splitsKeySelector.setEditable(false);
		splitsKeySelector.setToolTipText(StringAccessor.getString("ConfigurationDialog.clickhere"));
		splitsKeySelector.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(!TimerLabel.ignoreKey(e, false, false, 0, 0)) {
                    if(e.getSource() == splitsKeySelector){
                        splitkey = e.getKeyCode();
                        splitsKeySelector.setText(KeyEvent.getKeyText(splitkey));
                    } else if(e.getSource() == stackmatKeySelector1){
                        sekey1 = e.getKeyCode();
                        stackmatKeySelector1.setText(KeyEvent.getKeyText(sekey1));
                    } else if(e.getSource() == stackmatKeySelector2){
                        sekey2 = e.getKeyCode();
                        stackmatKeySelector2.setText(KeyEvent.getKeyText(sekey2));
                    }
                }
			}
		});

		panel.add(sideBySide(null, splits,
				new JLabel(StringAccessor.getString("ConfigurationDialog.minsplittime")),
				minSplitTime,
				new JLabel(StringAccessor.getString("ConfigurationDialog.splitkey")),
				splitsKeySelector));

		metronome = new JCheckBox(StringAccessor.getString("ConfigurationDialog.metronome"));
		metronome.addActionListener(this::actionPerformed);
		metronomeDelay = new TickerSlider(tickTock);
		panel.add(sideBySide(null, metronome, new JLabel(StringAccessor.getString("ConfigurationDialog.delay")), metronomeDelay));
		
		stackmatEmulation = new JCheckBox(StringAccessor.getString("ConfigurationDialog.emulatestackmat"));
		stackmatEmulation.addActionListener(this::actionPerformed);

		stackmatKeySelector1 = new JTextArea();
		stackmatKeySelector1.setColumns(10);
		stackmatKeySelector1.setEditable(false);
		stackmatKeySelector1.setToolTipText(StringAccessor.getString("ConfigurationDialog.clickhere"));
		stackmatKeySelector1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(!TimerLabel.ignoreKey(e, false, false, 0, 0)) {
                    if(e.getSource() == splitsKeySelector){
                        splitkey = e.getKeyCode();
                        splitsKeySelector.setText(KeyEvent.getKeyText(splitkey));
                    } else if(e.getSource() == stackmatKeySelector1){
                        sekey1 = e.getKeyCode();
                        stackmatKeySelector1.setText(KeyEvent.getKeyText(sekey1));
                    } else if(e.getSource() == stackmatKeySelector2){
                        sekey2 = e.getKeyCode();
                        stackmatKeySelector2.setText(KeyEvent.getKeyText(sekey2));
                    }
                }
			}
		});

		stackmatKeySelector2 = new JTextArea();
		stackmatKeySelector2.setColumns(10);
		stackmatKeySelector2.setEditable(false);
		stackmatKeySelector2.setToolTipText(StringAccessor.getString("ConfigurationDialog.clickhere"));
		stackmatKeySelector2.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(!TimerLabel.ignoreKey(e, false, false, 0, 0)) {
                    if(e.getSource() == splitsKeySelector){
                        splitkey = e.getKeyCode();
                        splitsKeySelector.setText(KeyEvent.getKeyText(splitkey));
                    } else if(e.getSource() == stackmatKeySelector1){
                        sekey1 = e.getKeyCode();
                        stackmatKeySelector1.setText(KeyEvent.getKeyText(sekey1));
                    } else if(e.getSource() == stackmatKeySelector2){
                        sekey2 = e.getKeyCode();
                        stackmatKeySelector2.setText(KeyEvent.getKeyText(sekey2));
                    }
                }
			}
		});

		panel.add(sideBySide(null, stackmatEmulation,
				new JLabel(StringAccessor.getString("ConfigurationDialog.stackmatkeys")),
				stackmatKeySelector1, stackmatKeySelector2));

		flashyWindow = new JCheckBox(StringAccessor.getString("ConfigurationDialog.flashchatwindow"));
		flashyWindow.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(flashyWindow);

		isBackground = new JCheckBox(StringAccessor.getString("ConfigurationDialog.watermark"));
		isBackground.addActionListener(this::actionPerformed);
		backgroundFile = new JTextField(30);
		backgroundFile.setToolTipText(StringAccessor.getString("ConfigurationDialog.clearfordefault"));
		browse = new JButton(StringAccessor.getString("ConfigurationDialog.browse"));
		browse.addActionListener(this::actionPerformed);
		panel.add(sideBySide(null, isBackground, new JLabel(StringAccessor.getString("ConfigurationDialog.file")), backgroundFile, browse));

		opacity = new JSlider(SwingConstants.HORIZONTAL, 0, 10, 0);
		panel.add(sideBySide(null, new JLabel(StringAccessor.getString("ConfigurationDialog.opacity")), opacity));

		scrambleFontChooser = new JColorComponent(StringAccessor.getString("ConfigurationDialog.scramblefont"));
		scrambleFontChooser.addMouseListener(mouseListener);

		timerFontChooser = new JColorComponent(StringAccessor.getString("ConfigurationDialog.timerfont"));
		timerFontChooser.addMouseListener(mouseListener);
		
		SyncGUIListener al = defaults -> {
            // makeStandardOptionsPanel2
            minSplitTime.setValue(configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, defaults));
            splits.setSelected(configuration.getBoolean(VariableKey.TIMING_SPLITS, defaults));
            splitkey = configuration.getInt(VariableKey.SPLIT_KEY, defaults);
            splitsKeySelector.setText(KeyEvent.getKeyText(splitkey));
            splitsKeySelector.setEnabled(splits.isSelected());
            stackmatEmulation.setSelected(configuration.getBoolean(VariableKey.STACKMAT_EMULATION, defaults));
            sekey1 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1, defaults);
            sekey2 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2, defaults);
            stackmatKeySelector1.setText(KeyEvent.getKeyText(sekey1));
            stackmatKeySelector2.setText(KeyEvent.getKeyText(sekey2));
            stackmatKeySelector1.setEnabled(stackmatEmulation.isSelected());
            stackmatKeySelector2.setEnabled(stackmatEmulation.isSelected());
            backgroundFile.setEnabled(isBackground.isSelected());
            browse.setEnabled(isBackground.isSelected());
            opacity.setEnabled(isBackground.isSelected());
            scrambleFontChooser.setFont(configuration.getFont(VariableKey.SCRAMBLE_FONT, defaults));
            timerFontChooser.setFont(configuration.getFont(VariableKey.TIMER_FONT, defaults).deriveFont(DISPLAY_FONT_SIZE));
            scrambleFontChooser.setBackground(configuration.getColor(VariableKey.SCRAMBLE_UNSELECTED, defaults));
            scrambleFontChooser.setForeground(configuration.getColor(VariableKey.SCRAMBLE_SELECTED, defaults));
            timerFontChooser.setBackground(configuration.getColorNullIfInvalid(VariableKey.TIMER_BG, defaults));
            timerFontChooser.setForeground(configuration.getColor(VariableKey.TIMER_FG, defaults));
            minSplitTime.setEnabled(splits.isSelected());
            metronome.setSelected(configuration.getBoolean(VariableKey.METRONOME_ENABLED, defaults));
            metronomeDelay.setEnabled(metronome.isSelected());
            metronomeDelay.setDelayBounds(configuration.getInt(VariableKey.METRONOME_DELAY_MIN, defaults), configuration.getInt(VariableKey.METRONOME_DELAY_MAX,
                    defaults), configuration.getInt(VariableKey.METRONOME_DELAY, defaults));
        };
		resetListeners.add(al);

		JButton reset = getResetButton(false);
		reset.addActionListener(al);
		panel.add(sideBySide(BoxLayout.LINE_AXIS, 
				Box.createHorizontalGlue(),	scrambleFontChooser, timerFontChooser, Box.createHorizontalGlue(), reset));
		
		return panel;
	}

	PuzzleSettingsTableEditor puzzlesCellEditor;
	PuzzleTypeListModel puzzleTypeListModel;

	ProfileListModel profilesModel;

	private JPanel makeScrambleTypeOptionsPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));

		DraggableJTable scramTable = new DraggableJTable(configuration, true, false);
		scramTable.refreshStrings(StringAccessor.getString("ConfigurationDialog.addpuzzle"));
		scramTable.getTableHeader().setReorderingAllowed(false);
		scramTable.setShowGrid(false);
		scramTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		scramTable.setDefaultRenderer(PuzzleType.class, puzzlesCellEditor);
		scramTable.setDefaultEditor(PuzzleType.class, puzzlesCellEditor);
		scramTable.setDefaultEditor(String.class, puzzlesCellEditor);
		scramTable.setModel(puzzleTypeListModel);

		JScrollPane jsp = new JScrollPane(scramTable);
		jsp.setPreferredSize(new Dimension(300, 0));
		panel.add(jsp, BorderLayout.CENTER);
		
		SyncGUIListener sl = defaults -> {
            // profile settings
            scramblePluginManager.reloadLengthsFromConfiguration(defaults);
            puzzleTypeListModel.setContents(scramblePluginManager.getPuzzleTypes(cubeTimerModel.getSelectedProfile()));
            profilesModel.setContents(profileDao.getProfiles());
        };
		resetListeners.add(sl);
		JButton reset = getResetButton(true);
		reset.addActionListener(sl);
		panel.add(reset, BorderLayout.LINE_END);
		
		return panel;
	}

	JSpinner stackmatValue = null;
	JCheckBox invertedHundredths = null;
	JCheckBox invertedSeconds = null;
	JCheckBox invertedMinutes = null;
	private JComboBox<ComboItem> lines = null;
	private JPanel mixerPanel = null;
	private JButton stackmatRefresh = null;
	JSpinner stackmatSamplingRate = null;
	private JPanel makeStackmatOptionsPanel() {
		JPanel options = new JPanel(new GridLayout(0, 1));

		SpinnerNumberModel integerModel = new SpinnerNumberModel(1, 1, 256, 1);
		stackmatValue = new JSpinner(integerModel);
		((JSpinner.DefaultEditor) stackmatValue.getEditor()).getTextField().setColumns(5);
		
		options.add(sideBySide(null, new JLabel(StringAccessor.getString("ConfigurationDialog.stackmatvalue")), stackmatValue));
		options.add(new JLabel(StringAccessor.getString("ConfigurationDialog.stackmatvaluedescription")));
		options.add(new JLabel(StringAccessor.getString("ConfigurationDialog.stackmatminsechund")));
		
		invertedMinutes = new JCheckBox(StringAccessor.getString("ConfigurationDialog.15minutes"));
		invertedMinutes.setMnemonic(KeyEvent.VK_I);
		invertedSeconds = new JCheckBox(StringAccessor.getString("ConfigurationDialog.165seconds"));
		invertedSeconds.setMnemonic(KeyEvent.VK_I);
		invertedHundredths = new JCheckBox(StringAccessor.getString("ConfigurationDialog.165hundredths"));
		invertedHundredths.setMnemonic(KeyEvent.VK_I);
		
		options.add(sideBySide(null, invertedMinutes, invertedSeconds, invertedHundredths));

		mixerPanel = new JPanel();

		if(stackmat != null) {
			items = getMixers();
			int selected = stackmat.getSelectedMixerIndex();
			lines = new JComboBox<>(items);
			lines.setMaximumRowCount(15);
			lines.setRenderer(new ComboRenderer<>());
			lines.addActionListener(new ComboListener(lines));
			lines.setSelectedIndex(selected);
			mixerPanel.add(lines);
		}

		stackmatRefresh = new JButton(StringAccessor.getString("ConfigurationDialog.refreshmixers"));
		stackmatRefresh.addActionListener(this::actionPerformed);
		mixerPanel.add(stackmatRefresh);

		options.add(mixerPanel);

		integerModel = new SpinnerNumberModel(1, 1, null, 1);
		stackmatSamplingRate = new JSpinner(integerModel);
		((JSpinner.DefaultEditor) stackmatSamplingRate.getEditor()).getTextField().setColumns(6);
		JButton reset = getResetButton(false);
		options.add(sideBySide(BoxLayout.LINE_AXIS,
				sideBySide(null, new JLabel(StringAccessor.getString("ConfigurationDialog.samplingrate")), stackmatSamplingRate), reset));

		SyncGUIListener sl = defaults -> {
            // makeStackmatOptionsPanel
            stackmatValue.setValue(configuration.getInt(VariableKey.SWITCH_THRESHOLD, defaults));
            invertedMinutes.setSelected(configuration.getBoolean(VariableKey.INVERTED_MINUTES, defaults));
            invertedSeconds.setSelected(configuration.getBoolean(VariableKey.INVERTED_SECONDS, defaults));
            invertedHundredths.setSelected(configuration.getBoolean(VariableKey.INVERTED_HUNDREDTHS, defaults));
            stackmatSamplingRate.setValue(configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE, defaults));
        };
		resetListeners.add(sl);
		reset.addActionListener(sl);
		
		return options;
	}
	
	public ComboItem[] getMixers() {
		String[] mixerNames = stackmat.getMixerChoices(StringAccessor.getString("StackmatInterpreter.mixer"),
				StringAccessor.getString("StackmatInterpreter.description"),
				StringAccessor.getString("StackmatInterpreter.nomixer"));
		ComboItem[] mixers = new ComboItem[mixerNames.length];
		for(int i=0; i<mixers.length; i++) {
			mixers[i] = new ComboItem(mixerNames[i], stackmat.isMixerEnabled(i));
		}
		int current = stackmat.getSelectedMixerIndex();
		mixers[current].setInUse(true);
		return mixers;
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if(source == inspectionCountdown) {
			speakInspection.setEnabled(inspectionCountdown.isSelected());
		} else if(e.getStateChange() == ItemEvent.SELECTED && source == profiles && !profiles.getSelectedItem().equals(cubeTimerModel.getSelectedProfile())) {
			int choice = Utils.showYesNoCancelDialog(this, StringAccessor.getString("ConfigurationDialog.saveprofile"));
			switch (choice) {
				case JOptionPane.YES_OPTION:
					applyAndSave();
					break;
				case JOptionPane.NO_OPTION:
					break;
				default:
					profiles.setSelectedItem(cubeTimerModel.getSelectedProfile());
					return;
			}

			// TODO load profile correctly
			Profile profile = (Profile) profiles.getSelectedItem();
			cubeTimerModel.setSelectedProfile(profile);

			configuration.loadConfiguration(profile);
			configuration.apply(profile);
			syncGUIwithConfig(false);
		}
	}

	private JEditorPane getStatsLegend() {
		JEditorPane pane = new JEditorPane("text/html", "");
		pane.setText("<html><font face='" + pane.getFont().getFontName() + "'><a href=''>" + StringAccessor.getString("ConfigurationDialog.seedynamicstrings") + "</a></font>");
		pane.setEditable(false);
		pane.setFocusable(false);
		pane.setOpaque(false);
		pane.setBorder(null);
		pane.addHyperlinkListener(this::hyperlinkUpdate);
		return pane;
	}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			showDynamicStrings();
	}

	private void showDynamicStrings() {
		try {
			URI uri = configuration.getDynamicStringsFile();
			Desktop.getDesktop().browse(uri);
		} catch(Exception error) {
			Utils.showErrorDialog(this, error);
		}
	}
	
	private JPanel makeStatisticsPanels() {
		JTabbedPane t = new JTabbedPane();
		JComponent tab = makeSessionSetupPanel();
		t.addTab(StringAccessor.getString("ConfigurationDialog.sessionstats"), tab);
		
		tab = makeBestRASetupPanel();
		t.addTab(StringAccessor.getString("ConfigurationDialog.bestrastats"), tab);
		
		tab = makeCurrentAverageSetupPanel();
		t.addTab(StringAccessor.getString("ConfigurationDialog.currrastats"), tab);
		
		JPanel c = new JPanel(new BorderLayout());
		c.add(t, BorderLayout.CENTER);
		return c;
	}
	
	
	JTextAreaWithHistory sessionStats = null;
	private JPanel makeSessionSetupPanel() {
		JPanel options = new JPanel(new BorderLayout(10, 0));
		sessionStats = new JTextAreaWithHistory();
		JScrollPane scroller = new JScrollPane(sessionStats);
		options.add(scroller, BorderLayout.CENTER);

		SyncGUIListener sl = defaults -> {
            // makeSessionSetupPanel
            sessionStats.setText(configuration.getString(VariableKey.SESSION_STATISTICS, defaults));
        };
		resetListeners.add(sl);
		JButton reset = getResetButton(false);
		reset.addActionListener(sl);

		options.add(sideBySide(BoxLayout.LINE_AXIS, getStatsLegend(), Box.createHorizontalGlue(), reset), BorderLayout.PAGE_END);
		return options;
	}
	JTextAreaWithHistory currentAverageStats = null;
	private JPanel makeCurrentAverageSetupPanel() {
		JPanel options = new JPanel(new BorderLayout(10, 0));
		currentAverageStats = new JTextAreaWithHistory();
		JScrollPane scroller = new JScrollPane(currentAverageStats);
		options.add(scroller, BorderLayout.CENTER);

		SyncGUIListener sl = defaults -> {
            // makeCurrentAverageSetupPanel
            currentAverageStats.setText(configuration.getString(VariableKey.CURRENT_AVERAGE_STATISTICS, defaults));
        };
		resetListeners.add(sl);
		JButton reset = getResetButton(false);
		reset.addActionListener(sl);

		options.add(sideBySide(BoxLayout.LINE_AXIS, getStatsLegend(), Box.createHorizontalGlue(), reset), BorderLayout.PAGE_END);
		return options;
	}
	JTextAreaWithHistory bestRAStats = null;
	private JPanel makeBestRASetupPanel() {
		JPanel options = new JPanel(new BorderLayout(10, 0));
		bestRAStats = new JTextAreaWithHistory();
		JScrollPane scroller = new JScrollPane(bestRAStats);
		options.add(scroller, BorderLayout.CENTER);

		SyncGUIListener sl = defaults -> {
            // makeBestRASetupPanel
            bestRAStats.setText(configuration.getString(VariableKey.BEST_RA_STATISTICS, defaults));
        };
		resetListeners.add(sl);
		JButton reset = getResetButton(false);
		reset.addActionListener(sl);

		options.add(sideBySide(BoxLayout.LINE_AXIS, getStatsLegend(), Box.createHorizontalGlue(), reset), BorderLayout.PAGE_END);
		return options;
	}

	private List<DefaultPuzzleViewComponent> solvedPuzzles;

	private JScrollPane makePuzzleColorsPanel() {
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.LINE_AXIS));
		options.add(Box.createHorizontalGlue());
		JScrollPane scroller = new JScrollPane(options, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.getHorizontalScrollBar().setUnitIncrement(10);
		List<PuzzleType> scramblePlugins = scramblePluginManager.getPuzzleTypes();
		solvedPuzzles = new ArrayList<>();

		for (PuzzleType puzzleType : scramblePlugins) {
			if (!puzzleType.getScramblePlugin().supportsScrambleImage()) {
				continue;
			}
			final DefaultPuzzleViewComponent scrambleViewComponent = new DefaultPuzzleViewComponent(puzzleType, configuration, scramblePluginManager);
			solvedPuzzles.add(scrambleViewComponent);

			scrambleViewComponent.setDefaultPuzzleView(puzzleType);
			scrambleViewComponent.setAlignmentY(Component.CENTER_ALIGNMENT);
			scrambleViewComponent.setAlignmentX(Component.RIGHT_ALIGNMENT);

			SyncGUIListener syncGUIListener = scrambleViewComponent::syncColorScheme;
			resetListeners.add(syncGUIListener);
			JButton resetColors = getResetButton(false);
			resetColors.addActionListener(syncGUIListener);
			resetColors.setAlignmentX(Component.RIGHT_ALIGNMENT);

			options.add(sideBySide(BoxLayout.PAGE_AXIS, scrambleViewComponent, resetColors));
		}
		options.add(Box.createHorizontalGlue());
		scroller.setPreferredSize(new Dimension(0, options.getPreferredSize().height));
		return scroller;
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == applyButton) {
			applyAndSave();
		} else if(source == saveButton) {
			applyAndSave();
			setVisible(false);
		} else if(source == cancelButton) {
			setVisible(false);
		} else if(source == resetAllButton) {
			int choice = Utils.showYesNoDialog(this, StringAccessor.getString("ConfigurationDialog.confirmreset"));
			if(choice == JOptionPane.YES_OPTION)
				syncGUIwithConfig(true);
		} else if(source == splits) {
			minSplitTime.setEnabled(splits.isSelected());
			splitsKeySelector.setEnabled(splits.isSelected());
		} else if(source == stackmatEmulation){
			stackmatKeySelector1.setEnabled(stackmatEmulation.isSelected());
			stackmatKeySelector2.setEnabled(stackmatEmulation.isSelected());
		} else if(source == browse) {
			CCTFileChooser fc = new CCTFileChooser(configuration);
			fc.setFileFilter(new ImageFilter());
			fc.setAccessory(new ImagePreview(fc));
			if(fc.showOpenDialog(this) == CCTFileChooser.APPROVE_OPTION) {
				backgroundFile.setText(fc.getSelectedFile().getAbsolutePath());
			}
		} else if(source == isBackground) {
			backgroundFile.setEnabled(isBackground.isSelected());
			browse.setEnabled(isBackground.isSelected());
			opacity.setEnabled(isBackground.isSelected());
		} else if(source == stackmatRefresh) {
			items = getMixers();
			int selected = stackmat.getSelectedMixerIndex();
			mixerPanel.remove(lines);
			lines = new JComboBox<>(items);
			lines.setMaximumRowCount(15);
			lines.setRenderer(new ComboRenderer<>());
			lines.addActionListener(new ComboListener(lines));
			lines.setSelectedIndex(selected);
			mixerPanel.add(lines, 0);
			pack();
		} else if(source == metronome) {
			metronomeDelay.setEnabled(metronome.isSelected());
		} else if(source instanceof JRadioButton) {
			JRadioButton jrb = (JRadioButton) source;
			configuration.setLong(VariableKey.FULLSCREEN_DESKTOP, Integer.parseInt(jrb.getActionCommand()));
		} else if(source == refreshDesktops) {
			refreshDesktops();
		}
	}

	void refreshDesktops() {
		Component focused = getFocusOwner();
		desktopPanel.removeAll();
		ButtonGroup g = new ButtonGroup();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for(int ch = 0; ch < gs.length; ch++) {
			GraphicsDevice gd = gs[ch];
			DisplayMode screenSize = gd.getDisplayMode();
			JRadioButton temp = new JRadioButton(
					(ch+1) + ":" + screenSize.getWidth() + "x" + screenSize.getHeight()
							+ " (" + StringAccessor.getString("ConfigurationDialog.desktopresolution") + ")");
			if(ch == configuration.getInt(VariableKey.FULLSCREEN_DESKTOP))
				temp.setSelected(true);
			g.add(temp);
			temp.setActionCommand("" + ch);
			temp.addActionListener(this::actionPerformed);
			desktopPanel.add(temp);
		}
		desktopPanel.add(refreshDesktops);
		if(focused != null)
			focused.requestFocusInWindow();
	}
	
	public void syncGUIwithConfig(boolean defaults) {
		setTitle(StringAccessor.getString("ConfigurationDialog.cctoptions") + " " + cubeTimerModel.getSelectedProfile().getName());
		profiles.setModel(new DefaultComboBoxModel<>(Iterables.toArray(profileDao.getProfiles(), Profile.class)));
		profiles.setSelectedItem(cubeTimerModel.getSelectedProfile());
		for(SyncGUIListener sl : resetListeners) {
			sl.syncGUIWithConfig(defaults);
		}
	}

	@Override
	public void setVisible(boolean newVisibleState) {
		if(!newVisibleState) {
			cancel();
		}
		super.setVisible(newVisibleState);
	}
	
	// this probably won't get used as much as apply, but it's here if you need it
	private void cancel() {
		scramblePluginManager.reloadLengthsFromConfiguration(false);
		profilesModel.discardChanges();
	}

	private void applyAndSave() {
		configuration.setColor(VariableKey.CURRENT_AVERAGE, currentAverage.getBackground());
		configuration.setColor(VariableKey.BEST_RA, bestRA.getBackground());
		configuration.setColor(VariableKey.BEST_TIME, bestTime.getBackground());
		configuration.setColor(VariableKey.WORST_TIME, worstTime.getBackground());
		configuration.setBoolean(VariableKey.CLOCK_FORMAT, clockFormat.isSelected());
		configuration.setBoolean(VariableKey.PROMPT_FOR_NEW_TIME, promptForNewTime.isSelected());
		configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, scramblePopup.isSelected());
		configuration.setBoolean(VariableKey.SIDE_BY_SIDE_SCRAMBLE, sideBySideScramble.isSelected());
		configuration.setBoolean(VariableKey.COMPETITION_INSPECTION, inspectionCountdown.isSelected());
		configuration.setBoolean(VariableKey.SPEAK_INSPECTION, speakInspection.isSelected());
		configuration.setBoolean(VariableKey.METRONOME_ENABLED, metronome.isSelected());
		configuration.setLong(VariableKey.METRONOME_DELAY, metronomeDelay.getMilliSecondsDelay());
		configuration.setBoolean(VariableKey.SPEAK_TIMES, speakTimes.isSelected());
		Object voice = voices.getSelectedItem();
		if(voice != null) {
			configuration.setString(VariableKey.VOICE, voice.toString());
		}

		configuration.setLong(VariableKey.SWITCH_THRESHOLD, (Integer) stackmatValue.getValue());
		configuration.setBoolean(VariableKey.INVERTED_MINUTES, invertedMinutes.isSelected());
		configuration.setBoolean(VariableKey.INVERTED_SECONDS, invertedSeconds.isSelected());
		configuration.setBoolean(VariableKey.INVERTED_HUNDREDTHS, invertedHundredths.isSelected());
		configuration.setLong(VariableKey.MIXER_NUMBER, lines.getSelectedIndex());
		configuration.setLong(VariableKey.STACKMAT_SAMPLING_RATE, (Integer) stackmatSamplingRate.getValue());

		configuration.setString(VariableKey.SESSION_STATISTICS, sessionStats.getText());
		configuration.setString(VariableKey.CURRENT_AVERAGE_STATISTICS, currentAverageStats.getText());
		configuration.setString(VariableKey.BEST_RA_STATISTICS, bestRAStats.getText());

		solvedPuzzles.forEach(DefaultPuzzleViewComponent::commitColorSchemeToConfiguration);

		configuration.setBoolean(VariableKey.TIMING_SPLITS, splits.isSelected());
		configuration.setDouble(VariableKey.MIN_SPLIT_DIFFERENCE, (Double) minSplitTime.getValue());
		configuration.setLong(VariableKey.SPLIT_KEY, splitkey);
		configuration.setBoolean(VariableKey.STACKMAT_EMULATION, stackmatEmulation.isSelected());
		configuration.setLong(VariableKey.STACKMAT_EMULATION_KEY1, sekey1);
		configuration.setLong(VariableKey.STACKMAT_EMULATION_KEY2, sekey2);

		configuration.setFont(VariableKey.SCRAMBLE_FONT, scrambleFontChooser.getFont());
		configuration.setColor(VariableKey.SCRAMBLE_SELECTED, scrambleFontChooser.getForeground());
		configuration.setColor(VariableKey.SCRAMBLE_UNSELECTED, scrambleFontChooser.getBackground());
		
		configuration.setColor(VariableKey.TIMER_BG, timerFontChooser.getBackground());
		configuration.setColor(VariableKey.TIMER_FG, timerFontChooser.getForeground());
		configuration.setFont(VariableKey.TIMER_FONT, timerFontChooser.getFont());

		scramblePluginManager.saveLengthsToConfiguration();
		for(PuzzleType sc : puzzleTypeListModel.getContents())
			sc.scramblePluginManager.saveGeneratorToConfiguration(sc);

		profilesModel.commitChanges();

		tagsModel.apply();
		
		configuration.apply(cubeTimerModel.getSelectedProfile());

		for (ComboItem item : items) {
			item.setInUse(false);
		}
		items[configuration.getInt(VariableKey.MIXER_NUMBER)].setInUse(true);

		configuration.saveConfiguration(cubeTimerModel.getSelectedProfile());
	}

}
