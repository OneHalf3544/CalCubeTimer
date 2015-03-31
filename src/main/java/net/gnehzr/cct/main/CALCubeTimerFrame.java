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
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.SessionsTable;
import net.gnehzr.cct.misc.customJTable.SolveTimeEditor;
import net.gnehzr.cct.misc.customJTable.SolveTimeRenderer;
import net.gnehzr.cct.misc.dynamicGUI.DynamicBorderSetter;
import net.gnehzr.cct.misc.dynamicGUI.DynamicCheckBox;
import net.gnehzr.cct.misc.dynamicGUI.DynamicDestroyable;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.scrambles.ScrambleCustomization;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import net.gnehzr.cct.umts.ircclient.IRCClientGUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.api.SubstanceConstants;
import org.jvnet.substance.watermark.SubstanceImageWatermark;
import org.jvnet.substance.watermark.SubstanceNullWatermark;
import org.jvnet.substance.watermark.SubstanceWatermark;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Singleton
public class CALCubeTimerFrame extends JFrame implements CalCubeTimerGui, TableModelListener {

	private static final Logger LOG = LogManager.getLogger(CALCubeTimerFrame.class);

	public static final String CCT_VERSION = CALCubeTimerFrame.class.getPackage().getImplementationVersion();
	public static final ImageIcon CUBE_ICON = new ImageIcon(CALCubeTimerFrame.class.getResource("cube.png"));

	CalCubeTimerModel model;
	private final StackmatInterpreter stackmatInterpreter;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private final ActionMap actionMap;
	private final ProfileDao profileDao;

	private JLabel onLabel = null;
	DraggableJTable timesTable = null;
	private JScrollPane timesScroller = null;
	private SessionsTable sessionsTable = null;
	ScrambleHyperlinkArea scrambleHyperlinkArea = null;
	private ScrambleCustomizationChooserComboBox scrambleCustomizationComboBox;
	private JPanel scrambleAttributesPanel = null;
	@Inject
	private JTextField generatorTextField;
	JSpinner scrambleNumber;
	private JSpinner scrambleLengthSpinner = null;
	JComboBox<Profile> profilesComboBox = null;
	JComboBox<LocaleAndIcon> languages = null;
	@Inject @Named("timeLabel")
	private TimerLabel timeLabel;
	@Inject @Named("bigTimersDisplay")
	TimerLabel bigTimersDisplay;
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
	private DynamicCheckBox[] attributes;

	//{{{ GUIParser
	//we save these guys to help us save the tabbedPane selection and
	//splitPane location later on
	List<JTabbedPane> tabbedPanes = new ArrayList<>();
	List<JSplitPane> splitPanes = new ArrayList<>();
	List<DynamicDestroyable> dynamicStringComponents = new ArrayList<>();


	final ItemListener profileComboboxListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			Profile affectedProfile = (Profile) e.getItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				model.prepareForProfileSwitch();
			} else if (e.getStateChange() == ItemEvent.SELECTED) {
				statsModel.removeTableModelListener(CALCubeTimerFrame.this); //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken

				profileDao.setSelectedProfile(affectedProfile);
				profileDao.loadDatabase(affectedProfile, scramblePluginManager);

				configuration.loadConfiguration(affectedProfile);
				configuration.apply(affectedProfile);

				model.sessionSelected(model.getNextSession(CALCubeTimerFrame.this)); //we want to load this profile's startup session
				statsModel.addTableModelListener(CALCubeTimerFrame.this); //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken
				repaintTimes(); //this needs to be here in the event that we loaded times from database
			}
		}
	};

	private final ItemListener scrambleChooserListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}

			Statistics s = statsModel.getCurrentSession().getStatistics();
			if (!model.getCustomizationEditsDisabled() && s != null) {
				//TODO - changing session? //TODO - deleted customization?
				s.editActions.add(new CustomizationEdit(model, model.getScramblesList().getCurrentScrambleCustomization(),
						(ScrambleCustomization) getScrambleCustomizationComboBox().getSelectedItem(), getScrambleCustomizationComboBox()));
			}

			model.getScramblesList().setCurrentScrambleCustomization((ScrambleCustomization) getScrambleCustomizationComboBox().getSelectedItem());
			//send current customization to irc, if connected
			ircClient.sendUserstate();

			//change current session's scramble customization
			statsModel.getCurrentSession().setCustomization(
                    model.getScramblesList().getCurrentScrambleCustomization(), profileDao.getSelectedProfile());

			boolean generatorEnabled = scramblePluginManager.isGeneratorEnabled(model.getScramblesList().getCurrentScrambleCustomization().getScrambleVariation());
			String generator = model.getScramblesList().getCurrentScrambleCustomization().getGenerator();
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

	private ChangeListener scrambleNumberListener = e -> {
		model.getScramblesList().setScrambleNumber((Integer) getScrambleNumber().getValue());
		updateScramble();
	};

	private ChangeListener scrambleLengthListener = e -> {
		model.getScramblesList().setScrambleLength((Integer) getScrambleLengthSpinner().getValue());
		updateScramble();
	};

	@Inject
	private SolveTimeEditor solveTimeEditor;

	@Inject
	private FullscreenFrame fullscreenFrame;

	@Inject
	StatisticsTableModel statsModel;

	@Inject
	private IRCClientGUI ircClient;

	@Inject
	private KeyboardHandler keyHandler;

	@Inject
	public CALCubeTimerFrame(CalCubeTimerModel calCubeTimerModel, StackmatInterpreter stackmatInterpreter,
							 Configuration configuration, ProfileDao profileDao, ScramblePluginManager scramblePluginManager,
							 DynamicBorderSetter dynamicBorderSetter,
							 XMLGuiMessages xmlGuiMessages, ActionMap actionMap, IRCClientGUI ircClient) {
		this.model = calCubeTimerModel;
		this.stackmatInterpreter = stackmatInterpreter;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.actionMap = actionMap;
		ircClient.setCalCubeTimer(this);
		this.profileDao = profileDao;
		this.dynamicBorderSetter = dynamicBorderSetter;
		this.xmlGuiMessages = xmlGuiMessages;
	}

	public void setSelectedProfile(Profile p) {
		profilesComboBox.setSelectedItem(p);
	}

	@Override
	public ScrambleChooserComboBox<ScrambleCustomization> getScrambleCustomizationComboBox() {
		return scrambleCustomizationComboBox;
	}

	@Inject
	private void initializeGUIComponents() {
		//NOTE: all internationalizable text must go in the loadStringsFromDefaultLocale() method
		DateTimeLabel currentTimeLabel = new DateTimeLabel(configuration);
		configuration.addConfigurationChangeListener(p -> currentTimeLabel.updateDisplay());

		scrambleCustomizationComboBox = new ScrambleCustomizationChooserComboBox(true, scramblePluginManager, configuration);
		scrambleCustomizationComboBox.addItemListener(scrambleChooserListener);

		scrambleNumber = new JSpinner(new SpinnerNumberModel(1,	1, 1, 1));
		((JSpinner.DefaultEditor) scrambleNumber.getEditor()).getTextField().setColumns(3);
		scrambleNumber.addChangeListener(scrambleNumberListener);

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

		timesTable = new DraggableJTable(configuration, false, true);
		timesTable.setName("timesTable");
		timesTable.setDefaultEditor(SolveTime.class, solveTimeEditor);
		timesTable.setDefaultRenderer(SolveTime.class, new SolveTimeRenderer(statsModel, configuration));
		timesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		timesTable.setModel(statsModel);
		//TODO - this wastes space, probably not easy to fix...
		timesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		timesScroller = new JScrollPane(timesTable);

		sessionsTable = new SessionsTable(statsModel, configuration, scramblePluginManager, profileDao);
		sessionsTable.setName("sessionsTable");
		//TODO - this wastes space, probably not easy to fix...
		sessionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane sessionsScroller = new JScrollPane(sessionsTable);
		sessionsTable.setSessionListener(model);

		scrambleHyperlinkArea = new ScrambleHyperlinkArea(scramblePopup, configuration, scramblePluginManager);
		scrambleHyperlinkArea.setAlignmentX(.5f);

		getTimeLabel().setKeyboardHandler(keyHandler);
		bigTimersDisplay.setKeyboardHandler(keyHandler);

		customGUIMenu = new JMenu();

		profilesComboBox = new JComboBox<>();
		profilesComboBox.addItemListener(profileComboboxListener);

		languages = new JComboBox<>();
		List<LocaleAndIcon> availableLocales = configuration.getAvailableLocales();
		languages.setModel(new DefaultComboBoxModel<>(availableLocales.toArray(new LocaleAndIcon[availableLocales.size()])));
		languages.addItemListener(languagesComboboxListener);
		languages.setRenderer(new LocaleRenderer(configuration));

		persistentComponents = new ComponentsMap();
		persistentComponents.put("scramblechooser", scrambleCustomizationComboBox);
		persistentComponents.put("scramblenumber", scrambleNumber);
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
		persistentComponents.put("sessionslist", sessionsScroller);
		persistentComponents.put("clock", currentTimeLabel);

		stackmatInterpreter.execute();
	}

	@Override
	public CALCubeTimerFrame getMainFrame() {
		return this;
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
                    Integer divide = configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true, configuration.getXMLGUILayout()), false);
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
			for(String key : messages.keySet())
				UIManager.put(key, messages.getString(key));
		} catch(MissingResourceException e) {
			throw Throwables.propagate(e);
		}

		StringAccessor.clearResources();
		xmlGuiMessages.reloadResources();
		statsModel.fireStringUpdates(); //this is necessary to update the undo-redo actions

		customGUIMenu.setText(StringAccessor.getString("CALCubeTimer.loadcustomgui"));
		timesTable.refreshStrings(StringAccessor.getString("CALCubeTimer.addtime"));
		scramblePopup.setTitle(StringAccessor.getString("CALCubeTimer.scrambleview"));
		scrambleNumber.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblenumber"));
		scrambleLengthSpinner.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblelength"));
		scrambleHyperlinkArea.updateStrings();

		model.getTimingListener().stackmatChanged(); //force the stackmat label to refresh
		timesTable.refreshColumnNames();
		sessionsTable.refreshColumnNames();

		// setLookAndFeel();
		createScrambleAttributesPanel();
		configurationDialog = null; //this will force the config dialog to reload when necessary

		SwingUtilities.updateComponentTreeUI(this);
		SwingUtilities.updateComponentTreeUI(scramblePopup);
	}

	@Override
	public void setCursor(Cursor cursor) {
		if(model.isLoading())
			super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		else if(cursor == null)
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
		getTimeLabel().configurationChanged(profileDao.getSelectedProfile());
		bigTimersDisplay.configurationChanged(profileDao.getSelectedProfile());

		xmlGuiMessages.reloadResources();

		DefaultHandler handler = new GuiParseSaxHandler(this, this, configuration, statsModel, dynamicBorderSetter, xmlGuiMessages, actionMap);
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

		timesTable.loadFromConfiguration();
		sessionsTable.loadFromConfiguration();

		for(JSplitPane pane : splitPanes) {
			Integer divide = configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true, configuration.getXMLGUILayout()), false);
			if(divide != null)
				pane.setDividerLocation(divide);
		}
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
		Statistics stats = statsModel.getCurrentSession().getStatistics();
		ircClient.sendUserstate();

		updateActionStatus(stats, "currentaverage0", AverageType.CURRENT);
		updateActionStatus(stats, "bestaverage0", AverageType.RA);
		updateActionStatus(stats, "currentaverage1", AverageType.CURRENT);
		updateActionStatus(stats, "bestaverage1", AverageType.RA);
		updateActionStatus(stats, "sessionaverage", AverageType.SESSION);
	}

	private void updateActionStatus(Statistics stats, String actionName, AverageType statType) {
		actionMap.getActionIfExist(actionName).ifPresent(action -> action.setEnabled(stats.isValid(statType, 0)));
	}

	//this happens in windows when alt+f4 is pressed
	@Override
	public void dispose() {
		super.dispose();
		Main.exit(0);
	}

	void updateWatermark() {
		SubstanceWatermark sw;
		if(configuration.getBoolean(VariableKey.WATERMARK_ENABLED, false)) {
			InputStream in;
			try {
				in = new FileInputStream(configuration.getString(VariableKey.WATERMARK_FILE, false));
			} catch (FileNotFoundException e) {
				in = CALCubeTimerFrame.class.getResourceAsStream(configuration.getString(VariableKey.WATERMARK_FILE, true));
			}
			SubstanceImageWatermark siw = new SubstanceImageWatermark(in);
			siw.setKind(SubstanceConstants.ImageWatermarkKind.APP_CENTER);
			siw.setOpacity(configuration.getFloat(VariableKey.OPACITY, false));
			sw = siw;
		} else
			sw = new SubstanceNullWatermark();
		SubstanceLookAndFeel.setSkin(SubstanceLookAndFeel.getCurrentSkin().withWatermark(sw));

		Window[] frames = Window.getWindows();
		for (Window frame : frames) {
			frame.repaint();
		}
	}

	private void safeSetValue(JSpinner test, Object val, ChangeListener listenerForDisable) {
		test.removeChangeListener(listenerForDisable);
		test.setValue(val);
		test.addChangeListener(listenerForDisable);
	}

	void selectProfileWithoutListenersNotify(JComboBox test, Object item, ItemListener itemListener) {
		test.removeItemListener(itemListener);
		test.setSelectedItem(item);
		test.addItemListener(itemListener);
	}

	private void safeSetScrambleNumberMax(int max) {
		scrambleNumber.removeChangeListener(scrambleNumberListener);
		((SpinnerNumberModel) scrambleNumber.getModel()).setMaximum(max);
		scrambleNumber.addChangeListener(scrambleNumberListener);
	}
	@Override
	public void updateScramble() {
		ScrambleString current = model.getScramblesList().getCurrent();
		if(current != null) {
			//set the length of the current scramble
			safeSetValue(scrambleLengthSpinner, current.getVariation().getLength(), scrambleLengthListener);
			//update new number of scrambles
			safeSetScrambleNumberMax(model.getScramblesList().size());
			//update new scramble number
			safeSetValue(scrambleNumber, model.getScramblesList().getScrambleNumber(), scrambleNumberListener);
			scrambleHyperlinkArea.setScramble(current, model.getScramblesList().getCurrentScrambleCustomization()); //this will update scramblePopup

			boolean canChangeStuff = model.getScramblesList().size() == model.getScramblesList().getScrambleNumber();
			scrambleCustomizationComboBox.setEnabled(canChangeStuff);
			scrambleLengthSpinner.setEnabled(current.getVariation().getLength() != 0 && canChangeStuff && !current.isImported());
		}
	}

	@Override
	public void setFullScreen(boolean value) {
		if(value == model.isFullscreen()) {
			model.setFullscreen(fullscreenFrame.isVisible());
			return;
		}
		model.setFullscreen(value);
		SwingUtilities.invokeLater(() -> {
			if (model.isFullscreen()) {
				fullscreenFrame.resizeFrame();
				bigTimersDisplay.requestFocusInWindow();
			}
			fullscreenFrame.setVisible(model.isFullscreen());
		});
	}

	@Override
	public void tableChanged(TableModelEvent event) {
		final Solution latestSolution = statsModel.getCurrentSession().getStatistics().get(-1);
		if(latestSolution != null) {
			ircClient.sendUserstate();
		}
		if(event != null && event.getType() == TableModelEvent.INSERT) {
			ScrambleString currentScramble = model.getScramblesList().getCurrent();
			boolean outOfScrambles = currentScramble.isImported(); //This is tricky, think before you change it
			outOfScrambles = !model.getScramblesList().getNext().isImported() && outOfScrambles;
			if(outOfScrambles) {
				Utils.showWarningDialog(this,
						StringAccessor.getString("CALCubeTimer.outofimported") +
								StringAccessor.getString("CALCubeTimer.generatedscrambles"));
			}
			updateScramble();
			//make the new time visible
			timesTable.invalidate(); //the table needs to be invalidated to force the new time to "show up"!!!
			Rectangle newTimeRect = timesTable.getCellRect(statsModel.getRowCount(), 0, true);
			timesTable.scrollRectToVisible(newTimeRect);

			model.speakTime(latestSolution.getTime(), this);
		}
		repaintTimes();
	}

	@Override
	public void loadXMLGUI() {
		SwingUtilities.invokeLater(() -> {
            refreshCustomGUIMenu();
            Component focusedComponent = CALCubeTimerFrame.this.getFocusOwner();
            parseXML_GUI(configuration.getXMLGUILayout());
            Dimension size = configuration.getDimension(VariableKey.MAIN_FRAME_DIMENSION, false);
            if(size == null) {
				CALCubeTimerFrame.this.pack();
			}
            else {
				CALCubeTimerFrame.this.setSize(size);
			}
            Point location = configuration.getPoint(VariableKey.MAIN_FRAME_LOCATION, false);
            if(location == null)
                CALCubeTimerFrame.this.setLocationRelativeTo(null);
            else {
                if(location.y < 0) //on windows, it is really bad if we let the window appear above the screen
                    location.y = 0;
                CALCubeTimerFrame.this.setLocation(location);
            }
            CALCubeTimerFrame.this.validate(); //this is needed to get the dividers to show up in the right place

            if(!configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) //This is to ensure that the keyboard is focused
                getTimeLabel().requestFocusInWindow();
            else if(focusedComponent != null)
                focusedComponent.requestFocusInWindow();
            else
                scrambleHyperlinkArea.requestFocusInWindow();
            getTimeLabel().componentResized(null);

			setFullScreen(model.isFullscreen());

            repaintTimes();
            actionMap.refreshActions();
        });
	}

	public void addTimeAction() {
		SwingUtilities.invokeLater(() -> {
            if(timesTable.isFocusOwner() || timesTable.requestFocusInWindow()) { //if the timestable is hidden behind a tab, we don't want to let the user add times
                timesTable.promptForNewRow();
                Rectangle newTimeRect = timesTable.getCellRect(statsModel.getRowCount(), 0, true);
                timesTable.scrollRectToVisible(newTimeRect);
            }
        });
	}

	public void resetAction() {
		int choice = Utils.showYesNoDialog(this, StringAccessor.getString("CALCubeTimer.confirmreset"));
		if(choice == JOptionPane.YES_OPTION) {
			getTimeLabel().reset();
			bigTimersDisplay.reset();
			model.getScramblesList().clear();
			updateScramble();
			statsModel.getCurrentSession().getStatistics().clear();
		}
	}

	public void showDocumentation() {
		try {
			URI uri = configuration.getDocumentationFile();
			Desktop.getDesktop().browse(uri);
		} catch(IOException error) {
			Utils.showErrorDialog(this, error);
		}
	}

	public void keyboardTimingAction() {
		boolean selected = (Boolean)actionMap.getAction(KeyboardTimingAction.KEYBOARD_TIMING_ACTION, this).getValue(Action.SELECTED_KEY);
		configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !selected);
		getTimeLabel().configurationChanged(profileDao.getSelectedProfile());
		bigTimersDisplay.configurationChanged(profileDao.getSelectedProfile());
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

	@NotNull
	Session createNewSession(Profile p, ScrambleCustomization customization) {
		PuzzleStatistics puzzleStatistics = p.getSessionsDatabase().getPuzzleStatistics(customization);
		Session session = new Session(LocalDateTime.now(), configuration, statsModel);
		puzzleStatistics.addSession(session);
		return session;
	}

	public void statusLightAction(){
		configuration.setBoolean(VariableKey.LESS_ANNOYING_DISPLAY, (Boolean) actionMap.getAction("togglestatuslight", this).getValue(Action.SELECTED_KEY));
		getTimeLabel().repaint();
	}

	public void hideScramblesAction(){
		configuration.setBoolean(VariableKey.HIDE_SCRAMBLES, (Boolean) actionMap.getAction("togglehidescrambles", this).getValue(Action.SELECTED_KEY));
		scrambleHyperlinkArea.refresh();
	}

	public void requestScrambleAction(){
		model.getScramblesList().getNext();
		updateScramble();
	}
	// End actions section }}}

	@Override
	public void addSplit(TimerState state) {
		long currentTime = System.currentTimeMillis();
		if((currentTime - model.getLastSplit()) / 1000. > configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)) {
			// todo hands variable is unused
			String hands = "";
			if(state instanceof StackmatState) {
				hands += ((StackmatState) state).leftHand() ? StringAccessor.getString("CALCubeTimer.lefthand")
														    : StringAccessor.getString("CALCubeTimer.righthand");
			}
			model.getSplits().add(state.toSolution(model.getScramblesList().getCurrent(), ImmutableList.of()).getTime());
			model.setLastSplit(currentTime);
		}
	}

	@Override
	public void updateInspection() {
		long inspection = model.getInpectionValue();
		String time;
		if(inspection <= -2) {
			model.setPenalty(SolveType.DNF);
			time = StringAccessor.getString("CALCubeTimer.disqualification");
		} else if(inspection <= 0) {
			model.setPenalty(SolveType.PLUS_TWO);
			time = StringAccessor.getString("CALCubeTimer.+2penalty");
		} else
			time = "" + inspection;
		getTimeLabel().setText(time);
		if(model.isFullscreen()) {
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
		ScrambleCustomization sc = model.getScramblesList().getCurrentScrambleCustomization();
		scrambleAttributesPanel.removeAll();
		if (sc == scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION) {
			return;
		}
		List<String> attrs = scramblePluginManager.getAvailablePuzzleAttributes(sc.getScramblePlugin().getClass());
		attributes = new DynamicCheckBox[attrs.size()];
		for (int ch = 0; ch < attrs.size(); ch++) { //create checkbox for each possible attribute
			boolean selected = false;
			for (String attr : sc.getScramblePlugin().getEnabledPuzzleAttributes(scramblePluginManager, configuration)) { //see if attribute is selected
				if (attrs.get(ch).equals(attr)) {
					selected = true;
					break;
				}
			}
			attributes[ch] = new DynamicCheckBox(new DynamicString(attrs.get(ch), statsModel, sc.getScramblePlugin().getMessageAccessor(), configuration), configuration);
			attributes[ch].setSelected(selected);
			attributes[ch].setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS, false));
			attributes[ch].setActionCommand(CalCubeTimerModel.SCRAMBLE_ATTRIBUTE_CHANGED);
			attributes[ch].addActionListener(e -> {
				ArrayList<String> attrs1 = new ArrayList<>();
				for (DynamicCheckBox attr : attributes) {
					if (attr.isSelected()) {
						attrs1.add(attr.getDynamicString().getRawText());
					}
				}
				scramblePluginManager.setEnabledPuzzleAttributes(attrs1);
				updateScramble();
			});
			scrambleAttributesPanel.add(attributes[ch]);
		}
		if (scrambleAttributesPanel.isDisplayable())
			scrambleAttributesPanel.getParent().validate();
	}

	@Override
	public JSpinner getScrambleNumber() {
		return scrambleNumber;
	}

	@Override
	public JSpinner getScrambleLengthSpinner() {
		return scrambleLengthSpinner;
	}

	public void showConfigurationDialog(CalCubeTimerModel calCubeTimerModel) {
        saveToConfiguration();
        if(configurationDialog == null) {
            configurationDialog = new ConfigurationDialog(
					this, true, configuration, profileDao, scramblePluginManager, statsModel,
                    calCubeTimerModel.getNumberSpeaker(), stackmatInterpreter, model.getTickTock(), timesTable);
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
		configuration.setString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, model.getScramblesList().getCurrentScrambleCustomization().toString());
		scramblePluginManager.saveLengthsToConfiguration();
		for (ScramblePlugin plugin : scramblePluginManager.getScramblePlugins()) {
			configuration.setStringArray(VariableKey.PUZZLE_ATTRIBUTES(plugin), plugin.getEnabledPuzzleAttributes(scramblePluginManager, configuration));
		}
		configuration.setPoint(VariableKey.SCRAMBLE_VIEW_LOCATION, scramblePopup.getLocation());
		configuration.setDimension(VariableKey.MAIN_FRAME_DIMENSION, getSize());
		configuration.setPoint(VariableKey.MAIN_FRAME_LOCATION, getLocation());
		ircClient.saveToConfiguration();

		for (JSplitPane jsp : splitPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jsp.getName(), true, configuration.getXMLGUILayout()), jsp.getDividerLocation());
		}
		for (JTabbedPane jtp : tabbedPanes) {
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jtp.getName(), true, configuration.getXMLGUILayout()), jtp.getSelectedIndex());
		}
		timesTable.saveToConfiguration();
		sessionsTable.saveToConfiguration();
	}

	@Override
	public JLabel getOnLabel() {
		return onLabel;
	}
}

