package net.gnehzr.cct.main;

import com.google.common.base.Throwables;
import org.springframework.stereotype.Service;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.dao.ProfileDao;
import net.gnehzr.cct.i18n.LocaleAndIcon;
import net.gnehzr.cct.i18n.LocaleRenderer;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.i18n.XMLGuiMessages;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.customJTable.SessionsListTable;
import net.gnehzr.cct.misc.dynamicGUI.DynamicBorderSetter;
import net.gnehzr.cct.misc.dynamicGUI.DynamicCheckBox;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.misc.dynamicGUI.DynamicStringSettableManger;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.statistics.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.PostConstruct;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

@Service
public class CALCubeTimerFrame extends JFrame {

	private static final Logger LOG = LogManager.getLogger(CALCubeTimerFrame.class);

    public static final String CCT_VERSION = CALCubeTimerFrame.class.getPackage().getImplementationVersion();
    public static final ImageIcon CUBE_ICON = new ImageIcon(CALCubeTimerFrame.class.getResource("cube.png"));

    public static final String SCRAMBLE_ATTRIBUTE_CHANGED = "Scramble Attribute Changed";

    private boolean fullscreen = false;

	private final StackmatInterpreter stackmatInterpreter;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private final net.gnehzr.cct.main.actions.ActionMap actionMap;
	private final ProfileDao profileDao;

    @Autowired
    private ScrambleListHolder scrambleListHolder;

    @Autowired
	private StackmatPluggedIndicatorLabel onLabel;

    @Autowired
	private CurrentSessionSolutionsTable currentSessionSolutionsTable = null;

	private JScrollPane timesScroller = null;

    @Autowired
	private SessionsListTable sessionsListTable;

    ScrambleHyperlinkArea scrambleHyperlinkArea = null;
	private PuzzleTypeComboBox puzzleTypeComboBox;
	private JPanel scrambleAttributesPanel = null;

    @Autowired
	private JTextField generatorTextField;

    private JSpinner scrambleNumberSpinner;
	private JSpinner scrambleLengthSpinner = null;
	JComboBox<Profile> profilesComboBox = null;
	private JComboBox<LocaleAndIcon> languages = null;

    @Autowired
	@Qualifier("timeLabel")
	private TimerLabel timeLabel;
	@Autowired
	@Qualifier("bigTimersDisplay")
	TimerLabel bigTimersDisplay;

	@Autowired
	NumberSpeaker numberSpeaker;
	//all of the above components belong in this HashMap, so we can find them
	//when they are referenced in the xml gui (type="blah...blah")
	//we also reset their attributes before parsing the xml gui
	ComponentsMap persistentComponents;

	@Autowired
	ScramblePopupPanel scramblePopup;

	CalCubeTimerModel model;
	@Autowired
	CurrentProfileHolder currentProfileHolder;

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

	@Autowired
	private SessionsList sessionsList;

	final ItemListener profileComboboxListener = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				Profile affectedProfile = (Profile) e.getItem();
                currentProfileHolder.setSelectedProfile(affectedProfile);

				//this needs to be here in the event that we loaded times from database
				actionMap.updateStatisticActionsStatuses();
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

			scrambleListHolder.asGenerating().generateScrambleForCurrentSession();

			boolean generatorEnabled = scramblePluginManager.isGeneratorEnabled(newPuzzleType);
			String generator = scramblePluginManager.getScrambleVariation(newPuzzleType).getGeneratorGroup();
			updateGeneratorField(generatorEnabled, generator);

			createScrambleAttributesPanel();
			updateScramble();
		}
	};


	private final ItemListener languagesComboboxListener = new ItemListener() {
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
		if (scrambleListHolder.isGenerating()) {
			PuzzleType oldPuzzleType = scrambleListHolder.getPuzzleType();
			scrambleListHolder.setScrambleList(convertCurrentSessionToImportedList(oldPuzzleType));
		}
		scrambleListHolder.asImported().setScrambleNumber((Integer) getScrambleNumberSpinner().getValue());
		updateScramble();
	};

	@Autowired
	private ScramblePopupPanel scramblePopupPanel;
	@Autowired
	private Metronome metromone;
    @Autowired
    private TimerLabelsHolder timerLabelsHolder;
	@Autowired
	private SolvingProcess solvingProcess;

	@NotNull
	private ImportedScrambleList convertCurrentSessionToImportedList(PuzzleType oldPuzzleType) {
		return new ImportedScrambleList(
				oldPuzzleType,
                sessionsList.getCurrentSession().getSolutionList().stream()
                        .map(Solution::getScrambleString)
                        .collect(toList())
        );
	}

	private ChangeListener scrambleLengthListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			PuzzleType puzzleType = scrambleListHolder.getPuzzleType();
			scramblePluginManager.getScrambleVariation(puzzleType).setLength((Integer) getScrambleLengthSpinner().getValue());
			updateScramble();
		}
	};

	@Autowired
	private FullscreenFrame fullscreenFrame;

	@Autowired
	public CALCubeTimerFrame(CalCubeTimerModel calCubeTimerModel, StackmatInterpreter stackmatInterpreter,
							 Configuration configuration, ProfileDao profileDao, ScramblePluginManager scramblePluginManager,
							 DynamicBorderSetter dynamicBorderSetter,
							 XMLGuiMessages xmlGuiMessages, net.gnehzr.cct.main.actions.ActionMap actionMap,
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

	public PuzzleTypeComboBox getPuzzleTypeComboBox() {
		return puzzleTypeComboBox;
	}

	@PostConstruct
    void initializeGUIComponents() {
        configuration.addConfigurationChangeListener(
                new CctModelConfigChangeListener(timerLabelsHolder, this,
                        currentProfileHolder, profileDao, configuration, scramblePluginManager,
                        actionMap, sessionsList, stackmatInterpreter));

		scrambleListHolder.addListener(this::updateScramble);

		setTitle("CCT " + CALCubeTimerFrame.CCT_VERSION);
		setIconImage(CALCubeTimerFrame.CUBE_ICON.getImage());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		puzzleTypeComboBox = new PuzzleTypeComboBox(configuration, model.getPuzzleTypeComboBoxModel());
		puzzleTypeComboBox.addItemListener(scrambleChooserListener);

		scrambleNumberSpinner = new JSpinner(new SpinnerNumberModel(1,	1, 1, 1));
		((JSpinner.DefaultEditor) scrambleNumberSpinner.getEditor()).getTextField().setColumns(3);
		scrambleNumberSpinner.addChangeListener(scrambleNumberSpinnerListener);

		scrambleLengthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		((JSpinner.DefaultEditor) scrambleLengthSpinner.getEditor()).getTextField().setColumns(3);
		scrambleLengthSpinner.addChangeListener(scrambleLengthListener);

		scrambleAttributesPanel = new JPanel();

		timesScroller = new JScrollPane(currentSessionSolutionsTable);
		JScrollPane sessionsListScrollPane = new JScrollPane(sessionsListTable);

		scrambleHyperlinkArea = new ScrambleHyperlinkArea(scramblePopup, configuration, scramblePluginManager);
		scrambleHyperlinkArea.setAlignmentX(.5f);

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
        persistentComponents.put("timerdisplay", timeLabel);
		persistentComponents.put("timeslist", timesScroller);
		persistentComponents.put("customguimenu", customGUIMenu);
		persistentComponents.put("languagecombobox", languages);
		persistentComponents.put("profilecombobox", profilesComboBox);
		persistentComponents.put("sessionslist", sessionsListScrollPane);
		persistentComponents.put("scrambleviewpanel", scramblePopupPanel);
		return persistentComponents;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

    public CALCubeTimerFrame getMainFrame() {
		return this;
	}

	private void refreshCustomGUIMenu() {
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
		scrambleNumberSpinner.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblenumber"));
		scrambleLengthSpinner.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblelength"));
		scrambleHyperlinkArea.updateStrings();

		timerLabelsHolder.stackmatChanged(); //force the stackmat label to refresh
		currentSessionSolutionsTable.refreshColumnNames();
		sessionsListTable.refreshColumnNames();

		createScrambleAttributesPanel();

		SwingUtilities.updateComponentTreeUI(this);
	}

	@Override
	public void setCursor(Cursor cursor) {
		if(cursor == null)
			super.setCursor(Cursor.getDefaultCursor());
		else
			super.setCursor(cursor);
	}

	private void parseXML_GUI(File xmlGUIfile) {
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
		getTimeLabel().configurationChanged();
		bigTimersDisplay.configurationChanged();

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

	public void updateScramble() {
		ScrambleString current = scrambleListHolder.getCurrentScramble();
		LOG.trace("update scramble view for {}, {}", current, scrambleListHolder.getPuzzleType());
		//set the length of the current scramble
		safeSetValue(scrambleLengthSpinner, current.getVariation().getLength(), scrambleLengthListener);
		//update new number of scrambles
		safeSetScrambleNumberMax(scrambleListHolder.scramblesCount());
		//update new scramble number
		safeSetValue(scrambleNumberSpinner, scrambleListHolder.getScrambleNumber(), scrambleNumberSpinnerListener);
		scrambleHyperlinkArea.setScramble(current, scrambleListHolder.getPuzzleType()); //this will update scramblePopup

		scrambleLengthSpinner.setEnabled(current.getVariation().getLength() != 0 && !current.isImported());
	}

	public void setFullscreen(boolean value) {
		if(value == isFullscreen()) {
            this.fullscreen = fullscreenFrame.isVisible();
            return;
		}
        LOG.trace("toggle fullscreen. was {}, new: {}", this.fullscreen, value);
        this.fullscreen = value;
        SwingUtilities.invokeLater(() -> {
			if (isFullscreen()) {
				fullscreenFrame.resizeFrame();
				bigTimersDisplay.requestFocusInWindow();
			}
			fullscreenFrame.setVisible(isFullscreen());
		});
	}

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
			getTimeLabel().getComponentResizeListener().componentResized(null);

			setFullscreen(isFullscreen());

			actionMap.updateStatisticActionsStatuses();
			actionMap.refreshActions();
		});
	}

	public TimerLabel getTimeLabel() {
		return timeLabel;
	}

	public void updateGeneratorField(boolean generatorEnabled, String generator) {
		//update new scramble generator
		generatorTextField.setText(generator);
		generatorTextField.setVisible(generatorEnabled);
	}

	public void createScrambleAttributesPanel() {
		LOG.debug("create scramble attributes panel");
		PuzzleType puzzleType = scrambleListHolder.getPuzzleType();
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

	public JSpinner getScrambleNumberSpinner() {
		return scrambleNumberSpinner;
	}

	public JSpinner getScrambleLengthSpinner() {
		return scrambleLengthSpinner;
	}

	public JComboBox<LocaleAndIcon> getLanguages() {
		return languages;
	}

	public void saveToConfiguration() {
		configuration.setDimension(VariableKey.MAIN_FRAME_DIMENSION, getSize());
		configuration.setPoint(VariableKey.MAIN_FRAME_LOCATION, getLocation());

		for (JSplitPane jsp : splitPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jsp.getName(), true, configuration.getXMLGUILayout()), jsp.getDividerLocation());
		}
		for (JTabbedPane jtp : tabbedPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jtp.getName(), true, configuration.getXMLGUILayout()), jtp.getSelectedIndex());
		}
		model.saveToConfiguration();
	}

    private class PuzzleAttributeCheckBox extends DynamicCheckBox {
		public PuzzleAttributeCheckBox(String availableAttributeName, PuzzleType puzzleType, boolean selected) {
			super(new DynamicString(
					availableAttributeName,
					puzzleType.getScramblePlugin().getMessageAccessor(),
					CALCubeTimerFrame.this.configuration));

			setSelected(selected);
			setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS));
			setActionCommand(SCRAMBLE_ATTRIBUTE_CHANGED);
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

