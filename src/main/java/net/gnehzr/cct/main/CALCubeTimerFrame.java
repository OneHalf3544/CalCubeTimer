package net.gnehzr.cct.main;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationDialog;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.LocaleRenderer;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.keyboardTiming.KeyboardHandler;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.customJTable.SessionsListTable;
import net.gnehzr.cct.misc.dynamicGUI.DynamicBorderSetter;
import net.gnehzr.cct.misc.dynamicGUI.DynamicCheckBox;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.misc.dynamicGUI.DynamicStringSettableManger;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.InspectionState;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.SessionSolutionsStatistics.AverageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Singleton
class CALCubeTimerFrame extends JFrame implements CalCubeTimerGui {

	private static final Logger LOG = LogManager.getLogger(CALCubeTimerFrame.class);

	private boolean fullscreen = false;

	CalCubeTimerModel model;
	private final StackmatInterpreter stackmatInterpreter;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private final ActionMap actionMap;
	private final ProfileDao profileDao;

	private JLabel onLabel = null;
	@Inject
	private CurrentSessionSolutionsTable currentSessionSolutionsTable = null;

	private JScrollPane timesScroller = null;
	private SessionsListTable sessionsListTable;
	ScrambleHyperlinkArea scrambleHyperlinkArea = null;
	private PuzzleTypeComboBox puzzleTypeComboBox;
	private JPanel scrambleAttributesPanel = null;
	@Inject
	private JTextField generatorTextField;
	JSpinner scrambleNumberSpinner;
	private JSpinner scrambleLengthSpinner = null;
	JComboBox<Profile> profilesComboBox = null;
	JComboBox<LocaleAndIcon> languages = null;
	@Inject @Named("timeLabel")
	private TimerLabel timeLabel;
	@Inject @Named("bigTimersDisplay")
	TimerLabel bigTimersDisplay;

	@Inject
	NumberSpeaker numberSpeaker;
	//all of the above components belong in this HashMap, so we can find them
	//when they are referenced in the xml gui (type="blah...blah")
	//we also reset their attributes before parsing the xml gui
	ComponentsMap persistentComponents;

	@Inject
	ScramblePopupFrame scramblePopup;

	ConfigurationDialog configurationDialog;
	private final DynamicBorderSetter dynamicBorderSetter;
	private final XMLGuiMessages xmlGuiMessages;
	private static final String GUI_LAYOUT_CHANGED = "GUI Layout Changed";
	private JMenu customGUIMenu;
	//This is a more appropriate way of doing gui's, to prevent weird resizing issues
	private static final Dimension min = new Dimension(235, 30);
	private List<DynamicCheckBox> attributeCheckBoxes;

	//{{{ GUIParser
	//we save these guys to help us save the tabbedPane selection and
	//splitPane location later on
	List<JTabbedPane> tabbedPanes = new ArrayList<>();
	List<JSplitPane> splitPanes = new ArrayList<>();
	DynamicStringSettableManger dynamicStringComponents;

	@Inject
	private SessionsList sessionsList;

	final ItemListener profileComboboxListener = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				Profile affectedProfile = (Profile) e.getItem();
                model.setSelectedProfile(affectedProfile);

				//this needs to be here in the event that we loaded times from database
				repaintTimes();
            }
		}

	};

	private final ItemListener scrambleChooserListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}

			PuzzleType newPuzzleType = (PuzzleType) getPuzzleTypeComboBox().getSelectedItem();

			//change current session's scramble puzzleType
			if (!sessionsList.getCurrentSession().getPuzzleType().equals(newPuzzleType)) {
				sessionsList.createSession(newPuzzleType);
			}

			model.getScramblesList().asGenerating().generateScrambleForCurrentSession();

			boolean generatorEnabled = scramblePluginManager.isGeneratorEnabled(newPuzzleType);
			String generator = scramblePluginManager.getScrambleVariation(newPuzzleType).getGeneratorGroup();
			updateGeneratorField(generatorEnabled, generator);

			createScrambleAttributesPanel();
			updateScramble();
		}
	};


	final ItemListener languagesComboboxListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			final LocaleAndIcon newLocale = ((LocaleAndIcon) e.getItem());
			if (e.getStateChange() == ItemEvent.SELECTED) {
				loadXMLGUI(); //this needs to be here so we reload the gui when configuration is changed
				if (!newLocale.equals(model.getLoadedLocale())) {
					if (model.getLoadedLocale() != null) {
						//we don't want to save the gui state if cct is starting up
						saveToConfiguration();
					}
					model.setLoadedLocale(newLocale);
					configuration.setDefaultLocale(newLocale);
					getLanguages().setFont(configuration.getFontForLocale(newLocale)); //for some reason, this cannot be put in the invokeLater() below
					SwingUtilities.invokeLater(CALCubeTimerFrame.this::loadStringsFromDefaultLocale);
				}
			}
		}
	};

	private ChangeListener scrambleNumberSpinnerListener = e -> {
		if (model.getScramblesList().isGenerating()) {
			ScrambleList oldScramblesList = model.getScramblesList();
			model.setScramblesList(convertCurrentSessionToImportedList(oldScramblesList));
		}
		model.getScramblesList().asImported().setScrambleNumber((Integer) getScrambleNumberSpinner().getValue());
		updateScramble();
	};

	@NotNull
	private ImportedScrambleList convertCurrentSessionToImportedList(ScrambleList oldScramblesList) {
		return new ImportedScrambleList(
                oldScramblesList.getPuzzleType(),
                sessionsList.getCurrentSession().getSolutionList().stream()
                        .map(Solution::getScrambleString)
                        .collect(toList()),
                        this);
	}

	private ChangeListener scrambleLengthListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			PuzzleType puzzleType = model.getScramblesList().getPuzzleType();
			scramblePluginManager.getScrambleVariation(puzzleType).setLength((Integer) getScrambleLengthSpinner().getValue());
			updateScramble();
		}
	};

	@Inject
	private FullscreenFrame fullscreenFrame;

	@Inject
	private KeyboardHandler keyHandler;

	@Inject
	public CALCubeTimerFrame(CalCubeTimerModel calCubeTimerModel, StackmatInterpreter stackmatInterpreter,
							 Configuration configuration, ProfileDao profileDao, ScramblePluginManager scramblePluginManager,
							 DynamicBorderSetter dynamicBorderSetter,
							 XMLGuiMessages xmlGuiMessages, ActionMap actionMap,
							 DynamicStringSettableManger dynamicStringComponents) {
		this.model = calCubeTimerModel;
		this.stackmatInterpreter = stackmatInterpreter;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.actionMap = actionMap;
		this.profileDao = profileDao;
		this.dynamicBorderSetter = dynamicBorderSetter;
		this.xmlGuiMessages = xmlGuiMessages;
		this.dynamicStringComponents = dynamicStringComponents;
	}

	@Override
	public PuzzleTypeComboBox getPuzzleTypeComboBox() {
		return puzzleTypeComboBox;
	}

	@Inject
	private void initializeGUIComponents(SessionsListTable sessionsListTable) {
		setTitle("CCT " + CALCubeTimerFrame.CCT_VERSION);
		setIconImage(CALCubeTimerFrame.CUBE_ICON.getImage());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.sessionsListTable = sessionsListTable;

		puzzleTypeComboBox = new PuzzleTypeComboBox(scramblePluginManager, configuration);
		puzzleTypeComboBox.addItemListener(scrambleChooserListener);

		scrambleNumberSpinner = new JSpinner(new SpinnerNumberModel(1,	1, 1, 1));
		((JSpinner.DefaultEditor) scrambleNumberSpinner.getEditor()).getTextField().setColumns(3);
		scrambleNumberSpinner.addChangeListener(scrambleNumberSpinnerListener);

		scrambleLengthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		((JSpinner.DefaultEditor) scrambleLengthSpinner.getEditor()).getTextField().setColumns(3);
		scrambleLengthSpinner.addChangeListener(scrambleLengthListener);

		scrambleAttributesPanel = new JPanel();

		onLabel = new JLabel() {
			@Override
			public void updateUI() {
				Font f = UIManager.getFont("Label.font");
				setFont(f.deriveFont(f.getSize2D() * 2));
				super.updateUI();
			}
		};

		timesScroller = new JScrollPane(currentSessionSolutionsTable);
		JScrollPane sessionsListScrollPane = new JScrollPane(sessionsListTable);

		scrambleHyperlinkArea = new ScrambleHyperlinkArea(scramblePopup, configuration, scramblePluginManager);
		scrambleHyperlinkArea.setAlignmentX(.5f);

		timeLabel.setKeyboardHandler(keyHandler);
		bigTimersDisplay.setKeyboardHandler(keyHandler);

		customGUIMenu = new JMenu();

		profilesComboBox = new JComboBox<>();
		profilesComboBox.addItemListener(profileComboboxListener);

		languages = new JComboBox<>();
		List<LocaleAndIcon> availableLocales = configuration.getAvailableLocales();
		languages.setModel(new DefaultComboBoxModel<>(availableLocales.toArray(new LocaleAndIcon[availableLocales.size()])));
		languages.addItemListener(languagesComboboxListener);
		languages.setRenderer(new LocaleRenderer(configuration));

		persistentComponents = componentsMap(sessionsListScrollPane);

		stackmatInterpreter.execute();
	}

	private ComponentsMap componentsMap(JScrollPane sessionsListScrollPane) {
		ComponentsMap persistentComponents = new ComponentsMap();
		persistentComponents.put("scramblechooser", puzzleTypeComboBox);
		persistentComponents.put("scramblenumber", scrambleNumberSpinner);
		persistentComponents.put("scramblelength", scrambleLengthSpinner);
		persistentComponents.put("scrambleattributes", scrambleAttributesPanel);
		persistentComponents.put("scramblegenerator", generatorTextField);
		persistentComponents.put("stackmatstatuslabel", onLabel);
		persistentComponents.put("scramblearea", scrambleHyperlinkArea);
		persistentComponents.put("timerdisplay", getTimeLabel());
		persistentComponents.put("timeslist", timesScroller);
		persistentComponents.put("customguimenu", customGUIMenu);
		persistentComponents.put("languagecombobox", languages);
		persistentComponents.put("profilecombobox", profilesComboBox);
		persistentComponents.put("sessionslist", sessionsListScrollPane);
		return persistentComponents;
	}

	@Override
	public boolean isFullscreen() {
		return fullscreen;
	}

	@Override
	public void setFullscreen(boolean fullscreen) {
		LOG.trace("toggle fullscreen. was {}, new: {}", this.fullscreen, fullscreen);
		this.fullscreen = fullscreen;
	}

	@Override
	public CALCubeTimerFrame getMainFrame() {
		return this;
	}

	@Override
	public void newSolutionAdded(TableModelEvent event) {
		final Solution latestSolution = sessionsList.getCurrentSession().getLastSolution();

		if (event.getType() == TableModelEvent.INSERT) {
			//make the new time visible
			currentSessionSolutionsTable.invalidate(); //the table needs to be invalidated to force the new time to "show up"!!!
			Rectangle newTimeRect = currentSessionSolutionsTable.getCellRect(sessionsList.getCurrentSession().getAttemptsCount(), 0, true);
			currentSessionSolutionsTable.scrollRectToVisible(newTimeRect);

			numberSpeaker.speakTime(latestSolution.getTime());
		}

		repaintTimes();
	}

	void refreshCustomGUIMenu() {
		customGUIMenu.removeAll();
		ButtonGroup group = new ButtonGroup();
		for(File file : configuration.getXMLLayoutsAvailable()) {
			JRadioButtonMenuItem temp = new JRadioButtonMenuItem(file.getName());
			temp.setSelected(file.equals(configuration.getXMLGUILayout()));
			temp.setActionCommand(GUI_LAYOUT_CHANGED);
			temp.addActionListener(e -> {
                saveToConfiguration();
                String layout = ((JRadioButtonMenuItem) e.getSource()).getText();
                configuration.setString(VariableKey.XML_LAYOUT, layout);
                parseXML_GUI(configuration.getXMLFile(layout));
                CALCubeTimerFrame.this.pack();
                CALCubeTimerFrame.this.setLocationRelativeTo(null);
                for (JSplitPane pane : splitPanes) { //the call to pack() is messing up the jsplitpanes
                    pane.setDividerLocation(pane.getResizeWeight());
                    Integer divide = configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true, configuration.getXMLGUILayout()));
                    if (divide != null)
                        pane.setDividerLocation(divide);
                }
            });
			group.add(temp);
			customGUIMenu.add(temp);
		}
	}

	@Override
	public void loadStringsFromDefaultLocale() {
		//this loads the strings for the swing components we use (JColorChooser and JFileChooser)
		UIManager.getDefaults().setDefaultLocale(Locale.getDefault());
		try {
			ResourceBundle messages = ResourceBundle.getBundle("languages/javax_swing");
			for(String key : messages.keySet()) {
				UIManager.put(key, messages.getString(key));
			}
		} catch(MissingResourceException e) {
			throw Throwables.propagate(e);
		}

		StringAccessor.clearResources();
		xmlGuiMessages.reloadResources();
		sessionsList.fireStringUpdates(); //this is necessary to update the undo-redo actions

		customGUIMenu.setText(StringAccessor.getString("CALCubeTimer.loadcustomgui"));
		currentSessionSolutionsTable.refreshStrings(StringAccessor.getString("CALCubeTimer.addtime"));
		scramblePopup.setTitle(StringAccessor.getString("CALCubeTimer.scrambleview"));
		scrambleNumberSpinner.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblenumber"));
		scrambleLengthSpinner.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblelength"));
		scrambleHyperlinkArea.updateStrings();

		model.getTimingListener().stackmatChanged(); //force the stackmat label to refresh
		currentSessionSolutionsTable.refreshColumnNames();
		sessionsListTable.refreshColumnNames();

		// setLookAndFeel();
		createScrambleAttributesPanel();
		configurationDialog = null; //this will force the config dialog to reload when necessary

		SwingUtilities.updateComponentTreeUI(this);
		SwingUtilities.updateComponentTreeUI(scramblePopup);
	}

	@Override
	public void setCursor(Cursor cursor) {
		if(cursor == null)
			super.setCursor(Cursor.getDefaultCursor());
		else
			super.setCursor(cursor);
	}

	void parseXML_GUI(File xmlGUIfile) {
		//this is needed to compute the size of the gui correctly
		//before reloading the gui, we must discard any old state these components may have had

		//we don't do anything with component names because
		//the only ones that matter are the 2 tables, and they're protected
		//by JScrollPanes from having their names changed.
		for(JComponentAndBorder cb : persistentComponents) {
			JComponent c = cb.c;
			c.setBorder(cb.b);
			c.setAlignmentX(Component.CENTER_ALIGNMENT);
			c.setAlignmentY(Component.CENTER_ALIGNMENT);
			c.setMinimumSize(null);
			c.setPreferredSize(null);
			c.setMaximumSize(null);
			c.setOpaque(c instanceof JMenu); //need this instanceof operator for the customguimenu
			c.setBackground(null);
			c.setForeground(null);
			c.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, false);
		}
		timesScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		timesScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrambleHyperlinkArea.resetPreferredSize();
		getTimeLabel().setAlignmentX(.5f);
		getTimeLabel().configurationChanged(model.getSelectedProfile());
		bigTimersDisplay.configurationChanged(model.getSelectedProfile());

		xmlGuiMessages.reloadResources();

		DefaultHandler handler = new GuiParseSaxHandler(this, this, configuration, dynamicBorderSetter, sessionsList, xmlGuiMessages, actionMap);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(xmlGUIfile, handler);
		} catch(SAXParseException spe) {
			LOG.error(spe.getSystemId() + ":" + spe.getLineNumber() + ": parse error: " + spe.getMessage());

			Exception x = spe;
			if (spe.getException() != null)
				x = spe.getException();
			LOG.info("unexpected exception", x);
		} catch(SAXException se) {
			Exception x = se;
			if(se.getException() != null)
				x = se.getException();
			LOG.info("unexpected exception", x);
		} catch(ParserConfigurationException | IOException pce) {
			LOG.info("unexpected exception", pce);
		}

		currentSessionSolutionsTable.loadFromConfiguration();
		sessionsListTable.loadFromConfiguration();

		for(JSplitPane pane : splitPanes) {
			Integer divide = configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true, configuration.getXMLGUILayout()));
			if(divide != null)
				pane.setDividerLocation(divide);
		}
		dynamicStringComponents.asStatisticsUpdateListener().statisticsUpdated();
	}

	@Override
	public Dimension getMinimumSize() {
		return min;
	}

	@Override
	public void setSize(Dimension d) {
		d.width = Math.max(d.width, min.width);
		d.height = Math.max(d.height, min.height);
		super.setSize(d);
	}

	/**
	 * Обновыление статистики
	 */
	@Override
	public void repaintTimes() {
		SessionSolutionsStatistics stats = sessionsList.getCurrentSession().getStatistics();

		updateActionStatus(stats, "currentaverage0", AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_5);
		updateActionStatus(stats, "currentaverage1", AverageType.CURRENT_ROLLING_AVERAGE, RollingAverageOf.OF_12);
		updateActionStatus(stats, "bestaverage0", AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_5);
		updateActionStatus(stats, "bestaverage1", AverageType.BEST_ROLLING_AVERAGE, RollingAverageOf.OF_12);
		updateActionStatus(stats, "sessionaverage", AverageType.SESSION_AVERAGE, null);
	}

	private void updateActionStatus(SessionSolutionsStatistics sessionStatistics, String actionName,
									AverageType statType, RollingAverageOf i) {
		actionMap.getActionIfExist(actionName).ifPresent(action -> action.setEnabled(sessionStatistics.isValid(statType, i)));
	}

	//this happens in windows when alt+f4 is pressed
	@Override
	public void dispose() {
		super.dispose();
		Main.exit(0);
	}

	private void safeSetValue(JSpinner test, Object val, ChangeListener listenerForDisable) {
		test.removeChangeListener(listenerForDisable);
		test.setValue(val);
		test.addChangeListener(listenerForDisable);
	}

	void selectProfileWithoutListenersNotify(JComboBox test, Profile item, ItemListener itemListener) {
		test.removeItemListener(itemListener);
		test.setSelectedItem(item);
		test.addItemListener(itemListener);
	}

	private void safeSetScrambleNumberMax(int max) {
		scrambleNumberSpinner.removeChangeListener(scrambleNumberSpinnerListener);
		((SpinnerNumberModel) scrambleNumberSpinner.getModel()).setMaximum(max);
		scrambleNumberSpinner.addChangeListener(scrambleNumberSpinnerListener);
	}

	@Override
	public void updateScramble() {
		ScrambleString current = model.getScramblesList().getCurrentScramble();
		LOG.trace("update scramble view for {}, {}", current, model.getScramblesList().getPuzzleType());
		//set the length of the current scramble
		safeSetValue(scrambleLengthSpinner, current.getVariation().getLength(), scrambleLengthListener);
		//update new number of scrambles
		safeSetScrambleNumberMax(model.getScramblesList().scramblesCount());
		//update new scramble number
		safeSetValue(scrambleNumberSpinner, model.getScramblesList().getScrambleNumber(), scrambleNumberSpinnerListener);
		scrambleHyperlinkArea.setScramble(current, model.getScramblesList().getPuzzleType()); //this will update scramblePopup

		scrambleLengthSpinner.setEnabled(current.getVariation().getLength() != 0 && !current.isImported());
	}

	@Override
	public void setFullScreen(boolean value) {
		if(value == isFullscreen()) {
			setFullscreen(fullscreenFrame.isVisible());
			return;
		}
		setFullscreen(value);
		SwingUtilities.invokeLater(() -> {
			if (isFullscreen()) {
				fullscreenFrame.resizeFrame();
				bigTimersDisplay.requestFocusInWindow();
			}
			fullscreenFrame.setVisible(isFullscreen());
		});
	}

	@Override
	public void loadXMLGUI() {
		SwingUtilities.invokeLater(() -> {
			refreshCustomGUIMenu();
			Component focusedComponent = CALCubeTimerFrame.this.getFocusOwner();
			parseXML_GUI(configuration.getXMLGUILayout());
			Dimension size = configuration.getDimension(VariableKey.MAIN_FRAME_DIMENSION);
			if (size == null) {
				CALCubeTimerFrame.this.pack();
			} else {
				CALCubeTimerFrame.this.setSize(size);
			}
			Point location = configuration.getPoint(VariableKey.MAIN_FRAME_LOCATION, false);
			if (location == null)
				CALCubeTimerFrame.this.setLocationRelativeTo(null);
			else {
				if (location.y < 0) //on windows, it is really bad if we let the window appear above the screen
					location.y = 0;
				CALCubeTimerFrame.this.setLocation(location);
			}
			CALCubeTimerFrame.this.validate(); //this is needed to get the dividers to show up in the right place

			if (!configuration.getBoolean(VariableKey.STACKMAT_ENABLED)) //This is to ensure that the keyboard is focused
				getTimeLabel().requestFocusInWindow();
			else if (focusedComponent != null)
				focusedComponent.requestFocusInWindow();
			else
				scrambleHyperlinkArea.requestFocusInWindow();
			getTimeLabel().componentResized(null);

			setFullScreen(isFullscreen());

			repaintTimes();
			actionMap.refreshActions();
		});
	}

	public void keyboardTimingAction() {
		boolean selected = (Boolean)actionMap.getAction(KeyboardTimingAction.KEYBOARD_TIMING_ACTION, this).getValue(Action.SELECTED_KEY);
		configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !selected);
		getTimeLabel().configurationChanged(model.getSelectedProfile());
		bigTimersDisplay.configurationChanged(model.getSelectedProfile());
		model.getStackmatInterpreter().enableStackmat(!selected);
		model.stopInspection();
		getTimeLabel().reset();
		bigTimersDisplay.reset();
		model.getTimingListener().stackmatChanged();
		if(selected) {
			getTimeLabel().requestFocusInWindow();
		}
		else {
			model.getTimingListener().timerAccidentlyReset(null); //when the keyboard timer is disabled, we reset the timer
		}
	}

	public void statusLightAction(){
		configuration.setBoolean(VariableKey.LESS_ANNOYING_DISPLAY, (Boolean) actionMap.getAction("togglestatuslight", this).getValue(Action.SELECTED_KEY));
		getTimeLabel().repaint();
	}

	// End actions section }}}

	@Override
	public void addSplit(TimerState state) {
		long currentTime = System.currentTimeMillis();
		if((currentTime - model.getLastSplit()) / 1000. > configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)) {
			model.getSplits().add(state.toSolution(model.getScramblesList().getCurrentScramble(), ImmutableList.of()).getTime());
			model.setLastSplit(currentTime);
		}
	}

	@Override
	public void updateInspection() {
		InspectionState inspection = model.getInspectionValue();
		String time;
		if(inspection.isDisqualification()) {
			model.setPenalty(SolveType.DNF);
			time = StringAccessor.getString("CALCubeTimer.disqualification");
		} else if(inspection.isPenalty()) {
			model.setPenalty(SolveType.PLUS_TWO);
			time = StringAccessor.getString("CALCubeTimer.+2penalty");
		} else {
			time = String.valueOf(inspection.getElapsedTime().getSeconds());
		}

		setTextToTimeLabels(time);
	}

	private void setTextToTimeLabels(String time) {
		getTimeLabel().setText(time);
		if (isFullscreen()) {
			bigTimersDisplay.setText(time);
		}
	}

	public TimerLabel getTimeLabel() {
		return timeLabel;
	}

	@Override
	public void updateGeneratorField(boolean generatorEnabled, String generator) {
		//update new scramble generator
		generatorTextField.setText(generator);
		generatorTextField.setVisible(generatorEnabled);
	}

	@Override
	public void createScrambleAttributesPanel() {
		LOG.debug("create scramble attributes panel");
		PuzzleType puzzleType = model.getScramblesList().getPuzzleType();
		scrambleAttributesPanel.removeAll();

		List<String> attrs = scramblePluginManager.getAvailablePuzzleAttributes(puzzleType.getScramblePlugin().getClass());
		attributeCheckBoxes = new ArrayList<>();
		for (String availableAttributeName : attrs) {
			//create checkbox for each possible attribute
			boolean selected = puzzleType.getScramblePlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration).stream()
					.anyMatch(availableAttributeName::equals);

			DynamicCheckBox dynamicCheckBox = new PuzzleAttributeCheckBox(availableAttributeName, puzzleType, selected);
			attributeCheckBoxes.add(dynamicCheckBox);
			scrambleAttributesPanel.add(dynamicCheckBox);
		}
		if (scrambleAttributesPanel.isDisplayable()) {
			scrambleAttributesPanel.getParent().validate();
		}
	}

	@Override
	public JSpinner getScrambleNumberSpinner() {
		return scrambleNumberSpinner;
	}

	@Override
	public JSpinner getScrambleLengthSpinner() {
		return scrambleLengthSpinner;
	}

	public void showConfigurationDialog(CalCubeTimerModel calCubeTimerModel) {
        saveToConfiguration();
        if(configurationDialog == null) {
            configurationDialog = new ConfigurationDialog(
					this, true, configuration, profileDao, scramblePluginManager,
					calCubeTimerModel.getNumberSpeaker(), calCubeTimerModel, stackmatInterpreter, model.getMetronome(),
					currentSessionSolutionsTable);
        }
        SwingUtilities.invokeLater(() -> {
            configurationDialog.syncGUIwithConfig(false);
            configurationDialog.setVisible(true);
        });
    }

	@Override
	public JComboBox<LocaleAndIcon> getLanguages() {
		return languages;
	}

	@Override
	public void saveToConfiguration() {
		configuration.setString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, model.getScramblesList().getPuzzleType().toString());
		scramblePluginManager.saveLengthsToConfiguration();
		for (ScramblePlugin plugin : scramblePluginManager.getScramblePlugins()) {
			configuration.setStringArray(VariableKey.PUZZLE_ATTRIBUTES(plugin), plugin.getEnabledPuzzleAttributes(scramblePluginManager, configuration));
		}
		configuration.setPoint(VariableKey.SCRAMBLE_VIEW_LOCATION, scramblePopup.getLocation());
		configuration.setDimension(VariableKey.MAIN_FRAME_DIMENSION, getSize());
		configuration.setPoint(VariableKey.MAIN_FRAME_LOCATION, getLocation());

		for (JSplitPane jsp : splitPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jsp.getName(), true, configuration.getXMLGUILayout()), jsp.getDividerLocation());
		}
		for (JTabbedPane jtp : tabbedPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jtp.getName(), true, configuration.getXMLGUILayout()), jtp.getSelectedIndex());
		}
		currentSessionSolutionsTable.saveToConfiguration();
		sessionsListTable.saveToConfiguration();
	}

	@Override
	public JLabel getOnLabel() {
		return onLabel;
	}

	private class PuzzleAttributeCheckBox extends DynamicCheckBox {
		public PuzzleAttributeCheckBox(String availableAttributeName, PuzzleType puzzleType, boolean selected) {
			super(new DynamicString(
					availableAttributeName,
					puzzleType.getScramblePlugin().getMessageAccessor(),
					CALCubeTimerFrame.this.configuration));

			setSelected(selected);
			setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS));
			setActionCommand(CalCubeTimerModel.SCRAMBLE_ATTRIBUTE_CHANGED);
			addActionListener(e -> {
				scramblePluginManager.setEnabledPuzzleAttributes(attributeCheckBoxes.stream()
						.filter(AbstractButton::isSelected)
						.map(DynamicCheckBox::getDynamicString)
						.map(DynamicString::getRawText)
						.collect(toList()));
				updateScramble();
			});
		}
	}

}

