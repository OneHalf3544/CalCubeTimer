package net.gnehzr.cct.main;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.ConfigurationDialog;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.help.AboutScrollFrame;
import net.gnehzr.cct.i18n.*;
import net.gnehzr.cct.keyboardTiming.KeyboardHandler;
import net.gnehzr.cct.keyboardTiming.TimerLabel;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.*;
import net.gnehzr.cct.misc.dynamicGUI.DynamicBorderSetter;
import net.gnehzr.cct.misc.dynamicGUI.DynamicCheckBox;
import net.gnehzr.cct.misc.dynamicGUI.DynamicDestroyable;
import net.gnehzr.cct.misc.dynamicGUI.DynamicString;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.scrambles.ScrambleList.ScrambleString;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import net.gnehzr.cct.statistics.Statistics.CCTUndoableEdit;
import net.gnehzr.cct.umts.ircclient.IRCClientGUI;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.api.SubstanceConstants;
import org.jvnet.substance.watermark.SubstanceImageWatermark;
import org.jvnet.substance.watermark.SubstanceNullWatermark;
import org.jvnet.substance.watermark.SubstanceWatermark;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

public class CALCubeTimer extends JFrame implements ActionListener, TableModelListener, ChangeListener, ConfigurationChangeListener, ItemListener, SessionListener {

	private static final Logger LOG = Logger.getLogger(CALCubeTimer.class);

	public static final String CCT_VERSION = CALCubeTimer.class.getPackage().getImplementationVersion();
	public static final ImageIcon cubeIcon = new ImageIcon(CALCubeTimer.class.getResource("cube.png"));

	public final StatisticsTableModel statsModel; //used in ProfileDatabase
	private final Configuration configuration;

	JLabel onLabel = null;
	DraggableJTable timesTable = null;
	private JScrollPane timesScroller = null;
	private SessionsTable sessionsTable = null;
	private JScrollPane sessionsScroller = null;
	ScrambleArea scrambleArea = null;
	ScrambleChooserComboBox scrambleChooser;
	private JPanel scrambleAttributes = null;
	private JTextField generator;
	JSpinner scrambleNumber;
	private JSpinner scrambleLength = null;
	private DateTimeLabel currentTimeLabel = null;
	private JComboBox<Profile> profilesComboBox = null;
	private LoudComboBox<LocaleAndIcon> languages = null;
	TimerLabel timeLabel = null;
	//all of the above components belong in this HashMap, so we can find them
	//when they are referenced in the xml gui (type="blah...blah")
	//we also reset their attributes before parsing the xml gui
	ComponentsMap persistentComponents;

	TimerLabel bigTimersDisplay = null;
	JLayeredPane fullscreenPanel = null;
	ScrambleFrame scramblePopup = null;
	ScrambleList scramblesList;
	StackmatInterpreter stackmatTimer = null;
	IRCClientGUI client;
	ConfigurationDialog configurationDialog;
	private TimingListener timingListener;
	boolean timing = false;
	private final ProfileDao profileDao;
	private final ScramblePlugin scramblePlugin;
	private final DynamicBorderSetter dynamicBorderSetter;
	private final NumberSpeaker numberSpeaker;
	private final XMLGuiMessages xmlGuiMessages;

	@Inject
	private SolveTimeEditor solveTimeEditor;

	@Inject
	public CALCubeTimer(Configuration configuration, ProfileDao profileDao, ScramblePlugin scramblePlugin,
						StatisticsTableModel statsModel1, DynamicBorderSetter dynamicBorderSetter,
						NumberSpeaker numberSpeaker, XMLGuiMessages xmlGuiMessages) {
		statsModel = statsModel1;
		this.configuration = configuration;
		this.profileDao = profileDao;
		profileDao.setCalCubeTimer(this);
		this.dynamicBorderSetter = dynamicBorderSetter;
		this.numberSpeaker = numberSpeaker;
		this.xmlGuiMessages = xmlGuiMessages;
		this.setUndecorated(true);
		this.actionMap = createActions();
		this.scramblePlugin = scramblePlugin;
		scramblesList = new ScrambleList(this.scramblePlugin);
		configuration.addConfigurationChangeListener(this);
	}

	public void setSelectedProfile(Profile p) {
		profilesComboBox.setSelectedItem(p);
	}

	ActionMap actionMap;

	private ActionMap createActions(){
		ActionMap actionMap = new ActionMap(this);

		statsModel.setUndoRedoListener(new UndoRedoListener() {
			private int undoable, redoable;
			@Override
			public void undoRedoChange(int undoable, int redoable) {
				this.undoable = undoable;
				this.redoable = redoable;
				refresh();
			}
			@Override
			public void refresh() {
				actionMap.get("undo").setEnabled(undoable != 0);
				actionMap.get("redo").setEnabled(redoable != 0);
				actionMap.get("undo").putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.undo") + undoable);
				actionMap.get("redo").putValue(Action.NAME, StringAccessor.getString("CALCubeTimer.redo") + redoable);
			}
		});
		return actionMap;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		String actionCommand = e.getActionCommand();
		switch (actionCommand) {
			case SCRAMBLE_ATTRIBUTE_CHANGED:
				ArrayList<String> attrs = new ArrayList<>();
				for (DynamicCheckBox attr : attributes)
					if (attr.isSelected())
						attrs.add(attr.getDynamicString().getRawText());
				String[] attributes = attrs.toArray(new String[attrs.size()]);
				scramblesList.getScrambleCustomization().getScramblePlugin().setEnabledPuzzleAttributes(attributes);
				updateScramble();
				break;

				case GUI_LAYOUT_CHANGED:
				saveToConfiguration();
				String layout = ((JRadioButtonMenuItem) source).getText();
				configuration.setString(VariableKey.XML_LAYOUT, layout);
				parseXML_GUI(configuration.getXMLFile(layout));
				this.pack();
				this.setLocationRelativeTo(null);
				for (JSplitPane pane : splitPanes) { //the call to pack() is messing up the jsplitpanes
					pane.setDividerLocation(pane.getResizeWeight());
					Integer divide = configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true, configuration.getXMLGUILayout()), false);
					if (divide != null)
						pane.setDividerLocation(divide);
				}
				break;
		}
	}

	private static class JComponentAndBorder {
		JComponent c;
		Border b;
		public JComponentAndBorder(JComponent c) {
			this.c = c;
			this.b = c.getBorder();
		}
	}
	static class ComponentsMap implements Iterable<JComponentAndBorder> {
		public ComponentsMap() {}
		private HashMap<String, JComponentAndBorder> componentMap = new HashMap<>();

		public JComponent getComponent(String name) {
			if(!componentMap.containsKey(name.toLowerCase()))
				return null;
			return componentMap.get(name.toLowerCase()).c;
		}
		public void put(String name, JComponent c) {
			componentMap.put(name.toLowerCase(), new JComponentAndBorder(c));
		}
		@Override
		public Iterator<JComponentAndBorder> iterator() {
			return new ArrayList<>(componentMap.values()).iterator();
		}
	}

	private Timer tickTock;
	private static final String GUI_LAYOUT_CHANGED = "GUI Layout Changed";
	private JMenu customGUIMenu;

	@Inject
	private void initializeGUIComponents() {
		//NOTE: all internationalizable text must go in the loadStringsFromDefaultLocale() method
		tickTock = createTickTockTimer();

		currentTimeLabel = new DateTimeLabel(configuration);

		scrambleChooser = new ScrambleChooserComboBox(true, true, scramblePlugin, configuration, profileDao);
		scrambleChooser.addItemListener(this);

		scrambleNumber = new JSpinner(new SpinnerNumberModel(1,	1, 1, 1));
		((JSpinner.DefaultEditor) scrambleNumber.getEditor()).getTextField().setColumns(3);
		scrambleNumber.addChangeListener(this);

		scrambleLength = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		((JSpinner.DefaultEditor) scrambleLength.getEditor()).getTextField().setColumns(3);
		scrambleLength.addChangeListener(this);

		scrambleAttributes = new JPanel();
		generator = new GeneratorTextField();

		scramblePopup = new ScrambleFrame(this, actionMap.get("togglescramblepopup"), false, configuration);
		scramblePopup.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		scramblePopup.setIconImage(cubeIcon.getImage());
		scramblePopup.setFocusableWindowState(false);

		onLabel = new JLabel() {
			@Override
			public void updateUI() {
				Font f = UIManager.getFont("Label.font");
				setFont(f.deriveFont(f.getSize2D() * 2));
				super.updateUI();
			}
		};

		timesTable = new DraggableJTable(configuration, false, true);
//		timesTable.setFocusable(false); //Man, this is almost perfect for us
		timesTable.setName("timesTable");
		timesTable.setDefaultEditor(SolveTime.class, solveTimeEditor);
		timesTable.setDefaultRenderer(SolveTime.class, new SolveTimeRenderer(statsModel, configuration));
		timesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		timesTable.setModel(statsModel);
		//TODO - this wastes space, probably not easy to fix...
		timesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		timesScroller = new JScrollPane(timesTable);

		sessionsTable = new SessionsTable(statsModel, configuration, scramblePlugin, profileDao);
		sessionsTable.setName("sessionsTable");
		//TODO - this wastes space, probably not easy to fix...
		sessionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		sessionsScroller = new JScrollPane(sessionsTable);
		sessionsTable.setSessionListener(this);

		scrambleArea = new ScrambleArea(scramblePopup, configuration, scramblePlugin);
		scrambleArea.setAlignmentX(.5f);

		stackmatTimer = new StackmatInterpreter(
				configuration,
				configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE, false),
				configuration.getInt(VariableKey.MIXER_NUMBER, false),
				configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false),
				configuration.getInt(VariableKey.SWITCH_THRESHOLD, false));
		timingListener = new TimingListenerImpl(this, configuration);
		new StackmatHandler(timingListener, stackmatTimer, configuration);

		timeLabel = new TimerLabel(scrambleArea, configuration);
		bigTimersDisplay = new TimerLabel(scrambleArea, configuration);

		KeyboardHandler keyHandler = new KeyboardHandler(timingListener, configuration);
		timeLabel.setKeyboardHandler(keyHandler);
		bigTimersDisplay.setKeyboardHandler(keyHandler);

		fullscreenPanel = new JLayeredPane();
		final JButton fullScreenButton = new JButton(actionMap.get("togglefullscreen"));

		fullscreenPanel.add(bigTimersDisplay, new Integer(0));
		fullscreenPanel.add(fullScreenButton, new Integer(1));

		fullscreenPanel.addComponentListener(new ComponentAdapter() {
			private static final int LENGTH = 30;
			@Override
			public void componentResized(ComponentEvent e) {
				bigTimersDisplay.setBounds(0, 0, e.getComponent().getWidth(), e.getComponent().getHeight());
				fullScreenButton.setBounds(e.getComponent().getWidth() - LENGTH, 0, LENGTH, LENGTH);
			}
		});

		customGUIMenu = new JMenu();

		profilesComboBox = new LoudComboBox<>();
		profilesComboBox.addItemListener(this);

		languages = new LoudComboBox<>();
		List<LocaleAndIcon> availableLocales = configuration.getAvailableLocales();
		languages.setModel(new DefaultComboBoxModel<>(availableLocales.toArray(new LocaleAndIcon[availableLocales.size()])));
		languages.addItemListener(this);
		languages.setRenderer(new LocaleRenderer(configuration));

		persistentComponents = new ComponentsMap();
		persistentComponents.put("scramblechooser", scrambleChooser);
		persistentComponents.put("scramblenumber", scrambleNumber);
		persistentComponents.put("scramblelength", scrambleLength);
		persistentComponents.put("scrambleattributes", scrambleAttributes);
		persistentComponents.put("scramblegenerator", generator);
		persistentComponents.put("stackmatstatuslabel", onLabel);
		persistentComponents.put("scramblearea", scrambleArea);
		persistentComponents.put("timerdisplay", timeLabel);
		persistentComponents.put("timeslist", timesScroller);
		persistentComponents.put("customguimenu", customGUIMenu);
		persistentComponents.put("languagecombobox", languages);
		persistentComponents.put("profilecombobox", profilesComboBox);
		persistentComponents.put("sessionslist", sessionsScroller);
		persistentComponents.put("clock", currentTimeLabel);

		stackmatTimer.execute();
	}

    private Timer createTickTockTimer() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(configuration.getString(VariableKey.METRONOME_CLICK_FILE, false));
            Objects.requireNonNull(inputStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
            DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
            Timer timer = new Timer(1000, new ActionListener() {
                int i = 0;

                @Override
				public void actionPerformed(ActionEvent arg0) {
                    LOG.info(i++);
                    clip.stop();
                    clip.setFramePosition(0);
                    clip.start();
                }
            });
            timer.setInitialDelay(0);
            return timer;

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
            throw new IllegalStateException(e1);
        }
    }

    private class GeneratorTextField extends JTextField implements FocusListener, ActionListener {
		public GeneratorTextField() {
			addFocusListener(this);
			addActionListener(this);
			setColumns(10);
			putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
			setToolTipText(StringAccessor.getString("CALCubeTimer.generatorgroup"));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setText(getText());
		}
		private String oldText;
		@Override
		public void setText(String t) {
			setVisible(t != null);

			if(t != null && !t.equals(oldText)) {
				scramblesList.updateGeneratorGroup(t);
				updateScramble();
			}
			oldText = t;
			
			super.setText(t);
		}
		@Override
		public void focusGained(FocusEvent e) {
			oldText = getText();
		}
		@Override
		public void focusLost(FocusEvent e) {
			setText(oldText);
		}
	}

	void refreshCustomGUIMenu() {
		customGUIMenu.removeAll();
		ButtonGroup group = new ButtonGroup();
		for(File file : configuration.getXMLLayoutsAvailable()) {
			JRadioButtonMenuItem temp = new JRadioButtonMenuItem(file.getName());
			temp.setSelected(file.equals(configuration.getXMLGUILayout()));
			temp.setActionCommand(GUI_LAYOUT_CHANGED);
			temp.addActionListener(this);
			group.add(temp);
			customGUIMenu.add(temp);
		}
	}

	//if we deleted the current session, should we create a new one, or load the "nearest" session?
	private Session getNextSession() {
		Session nextSesh = statsModel.getCurrentSession();
		Profile p = profileDao.getSelectedProfile();
		String customization = scramblesList.getScrambleCustomization().toString();
		ProfileDatabase puzzleDatabase = p.getPuzzleDatabase();
		PuzzleStatistics ps = puzzleDatabase.getPuzzleStatistics(customization);
		if(!ps.containsSession(nextSesh)) {
			//failed to find a session to continue, so load newest session
			int sessionCount = puzzleDatabase.getRowCount();
			nextSesh = puzzleDatabase.getSessions().stream()
					.max(Comparator.comparing(session -> session.getStatistics().getStartDate()))
					.orElseGet(() -> createNewSession(p, customization));
		}
		return nextSesh;
	}

	@Override
	public void sessionSelected(Session s) {
		statsModel.setSession(s);
		scramblesList.clear();
		Statistics stats = s.getStatistics();
		for(int ch = 0; ch < stats.getAttemptCount(); ch++)
			scramblesList.addScramble(stats.get(ch).getScramble());
		scramblesList.setScrambleNumber(scramblesList.size() + 1);

		customizationEditsDisabled = true;
		scrambleChooser.setSelectedItem(s.getCustomization()); //this will update the scramble
		customizationEditsDisabled = false;
		
		sendUserstate();
	}

	@Override
	public void sessionsDeleted() {
		Session s = getNextSession();
		statsModel.setSession(s);
		scrambleChooser.setSelectedItem(s.getCustomization());
	}

	private static boolean customizationEditsDisabled = false;

	private class CustomizationEdit implements CCTUndoableEdit {
		private ScrambleCustomization oldCustom, newCustom;
		public CustomizationEdit(ScrambleCustomization oldCustom, ScrambleCustomization newCustom) {
			this.oldCustom = oldCustom;
			this.newCustom = newCustom;
		}
		@Override
		public void doEdit() {
			customizationEditsDisabled = true;
			scrambleChooser.setSelectedItem(newCustom);
			customizationEditsDisabled = false;
		}
		@Override
		public void undoEdit() {
			customizationEditsDisabled = true;
			scrambleChooser.setSelectedItem(oldCustom);
			customizationEditsDisabled = false;
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		if(source == scrambleChooser && e.getStateChange() == ItemEvent.SELECTED) {
			Statistics s = statsModel.getCurrentStatistics();
			if(!customizationEditsDisabled && s != null) {
				//TODO - changing session? //TODO - deleted customization?
				s.editActions.add(new CustomizationEdit(scramblesList.getScrambleCustomization(),
						(ScrambleCustomization) scrambleChooser.getSelectedItem()));
			}
			
			scramblesList.setScrambleCustomization((ScrambleCustomization) scrambleChooser.getSelectedItem());
			//send current customization to irc, if connected
			sendUserstate();
			
			//change current session's scramble customization
			if(statsModel.getCurrentSession() != null) {
				statsModel.getCurrentSession().setCustomization(
						scramblesList.getScrambleCustomization().toString(), profileDao.getSelectedProfile());
			}
			
			//update new scramble generator
			generator.setText(scramblesList.getScrambleCustomization().getGenerator());
			generator.setVisible(scramblesList.getScrambleCustomization().getScramblePlugin().isGeneratorEnabled());
			
			createScrambleAttributes();
			updateScramble();
		} else if(source == profilesComboBox) {
			Profile affected = (Profile)e.getItem();
			if(e.getStateChange() == ItemEvent.DESELECTED) {
				prepareForProfileSwitch();
			} else if(e.getStateChange() == ItemEvent.SELECTED) {
				statsModel.removeTableModelListener(this); //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken

				profileDao.setSelectedProfile(affected);
				if (!profileDao.loadDatabase(affected, scramblePlugin)) {
					//the user will be notified of this in the profiles combobox
				}
				try {
					configuration.loadConfiguration(affected.getConfigurationFile());
					configuration.apply(affected);
				} catch (IOException err) {
					LOG.info("unexpected exception", err);
				}
				sessionSelected(getNextSession()); //we want to load this profile's startup session
				statsModel.addTableModelListener(this); //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken
				repaintTimes(); //this needs to be here in the event that we loaded times from database
			}
		} else if(source == languages) {
			final LocaleAndIcon newLocale = ((LocaleAndIcon) e.getItem());
			if(e.getStateChange() == ItemEvent.SELECTED) {
				loadXMLGUI(); //this needs to be here so we reload the gui when configuration is changed
				if(!newLocale.equals(loadedLocale)) {
					if(loadedLocale != null) //we don't want to save the gui state if cct is starting up
						saveToConfiguration();
					loadedLocale = newLocale;
					configuration.setDefaultLocale(newLocale);
					languages.setFont(configuration.getFontForLocale(newLocale)); //for some reason, this cannot be put in the invokeLater() below
					SwingUtilities.invokeLater(this::loadStringsFromDefaultLocale);
				}
			}
		}
	}

	private static boolean loading = false;

	public void setWaiting(boolean loading) {
		CALCubeTimer.loading = loading;
		setCursor(null);
	}

	private LocaleAndIcon loadedLocale;
	void loadStringsFromDefaultLocale() {
		Utils.doInWaitingState(this, () -> {
			//this loads the strings for the swing components we use (JColorChooser and JFileChooser)
			UIManager.getDefaults().setDefaultLocale(Locale.getDefault());
			try {
				ResourceBundle messages = ResourceBundle.getBundle("languages/javax_swing");
				for(String key : messages.keySet())
					UIManager.put(key, messages.getString(key));
			} catch(MissingResourceException e) {
				LOG.info("unexpected exception", e);
			}

			StringAccessor.clearResources();
			xmlGuiMessages.reloadResources();
			statsModel.fireStringUpdates(); //this is necessary to update the undo-redo actions

			customGUIMenu.setText(StringAccessor.getString("CALCubeTimer.loadcustomgui"));
			timesTable.refreshStrings(StringAccessor.getString("CALCubeTimer.addtime"));
			scramblePopup.setTitle(StringAccessor.getString("CALCubeTimer.scrambleview"));
			scrambleNumber.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblenumber"));
			scrambleLength.setToolTipText(StringAccessor.getString("CALCubeTimer.scramblelength"));
			scrambleArea.updateStrings();

			timingListener.stackmatChanged(); //force the stackmat label to refresh
			timesTable.refreshColumnNames();
			sessionsTable.refreshColumnNames();

			setLookAndFeel();
			createScrambleAttributes();
			configurationDialog = null; //this will force the config dialog to reload when necessary

			SwingUtilities.updateComponentTreeUI(this);
			SwingUtilities.updateComponentTreeUI(scramblePopup);

		});

	}

	@Override
	public void setCursor(Cursor cursor) {
		if(loading)
			super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		else if(cursor == null)
			super.setCursor(Cursor.getDefaultCursor());
		else
			super.setCursor(cursor);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if(source == scrambleNumber) {
			scramblesList.setScrambleNumber((Integer) scrambleNumber.getValue());
			updateScramble();
		} else if(source == scrambleLength) {
			scramblesList.setScrambleLength((Integer) scrambleLength.getValue());
			updateScramble();
		}
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
		scrambleArea.resetPreferredSize();
		timeLabel.setAlignmentX(.5f);
		timeLabel.configurationChanged(profileDao.getSelectedProfile());
		bigTimersDisplay.configurationChanged(profileDao.getSelectedProfile());

		xmlGuiMessages.reloadResources();

		DefaultHandler handler = new GUIParser(this, this, configuration, statsModel, dynamicBorderSetter, xmlGuiMessages);
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

	//This is a more appropriate way of doing gui's, to prevent weird resizing issues
	private static final Dimension min = new Dimension(235, 30);
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

	private DynamicCheckBox[] attributes;
	private static final String SCRAMBLE_ATTRIBUTE_CHANGED = "Scramble Attribute Changed";
	public void createScrambleAttributes() {
		ScrambleCustomization sc = scramblesList.getScrambleCustomization();
		scrambleAttributes.removeAll();
		if(sc == null) {
			return;
		}
		String[] attrs = sc.getScramblePlugin().getAvailablePuzzleAttributes();
		attributes = new DynamicCheckBox[attrs.length];
		ScramblePluginMessages.loadResources(sc.getScramblePlugin().getPluginClassName());
		for(int ch = 0; ch < attrs.length; ch++) { //create checkbox for each possible attribute
			boolean selected = false;
			for(String attr : sc.getScramblePlugin().getEnabledPuzzleAttributes()) { //see if attribute is selected
				if(attrs[ch].equals(attr)) {
					selected = true;
					break;
				}
			}
			attributes[ch] = new DynamicCheckBox(new DynamicString(attrs[ch], statsModel, ScramblePluginMessages.SCRAMBLE_ACCESSOR, configuration), configuration);
			attributes[ch].setSelected(selected);
			attributes[ch].setFocusable(configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS, false));
			attributes[ch].setActionCommand(SCRAMBLE_ATTRIBUTE_CHANGED);
			attributes[ch].addActionListener(this);
			scrambleAttributes.add(attributes[ch]);
		}
		if(scrambleAttributes.isDisplayable())
			scrambleAttributes.getParent().validate();
	}
	//{{{ GUIParser
	//we save these guys to help us save the tabbedPane selection and
	//splitPane location later on
	List<JTabbedPane> tabbedPanes = new ArrayList<>();
	List<JSplitPane> splitPanes = new ArrayList<>();
	List<DynamicDestroyable> dynamicStringComponents = new ArrayList<>();

	private void repaintTimes() {
		Statistics stats = statsModel.getCurrentStatistics();
		sendUserstate();
		AbstractAction a;
		if((a = actionMap.getRawAction("currentaverage0")) != null) a.setEnabled(stats.isValid(AverageType.CURRENT, 0));
		if((a = actionMap.getRawAction("bestaverage0")) != null) a.setEnabled(stats.isValid(AverageType.RA, 0));
		if((a = actionMap.getRawAction("currentaverage1")) != null) a.setEnabled(stats.isValid(AverageType.CURRENT, 1));
		if((a = actionMap.getRawAction("bestaverage1")) != null) a.setEnabled(stats.isValid(AverageType.RA, 1));
		if((a = actionMap.getRawAction("sessionaverage")) != null) a.setEnabled(stats.isValid(AverageType.SESSION, 0));
	}

	//this happens in windows when alt+f4 is pressed
	@Override
	public void dispose() {
		super.dispose();
		System.exit(0);
	}

	void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(new org.jvnet.substance.skin.SubstanceModerateLookAndFeel());
		} catch (Exception e1) {
			LOG.info("unexpected exception", e1);
		}
		updateWatermark();
	}
	private void updateWatermark() {
		SubstanceWatermark sw;
		if(configuration.getBoolean(VariableKey.WATERMARK_ENABLED, false)) {
			InputStream in;
			try {
				in = new FileInputStream(configuration.getString(VariableKey.WATERMARK_FILE, false));
			} catch (FileNotFoundException e) {
				in = CALCubeTimer.class.getResourceAsStream(configuration.getString(VariableKey.WATERMARK_FILE, true));
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

	private void safeSetValue(JSpinner test, Object val) {
		test.removeChangeListener(this);
		test.setValue(val);
		test.addChangeListener(this);
	}
	private void safeSelectItem(JComboBox test, Object item) {
		test.removeItemListener(this);
		test.setSelectedItem(item);
		test.addItemListener(this);
	}
	private void safeSetScrambleNumberMax(int max) {
		scrambleNumber.removeChangeListener(this);
		((SpinnerNumberModel) scrambleNumber.getModel()).setMaximum(max);
		scrambleNumber.addChangeListener(this);
	}
	void updateScramble() {
		ScrambleString current = scramblesList.getCurrent();
		if(current != null) {
			//set the length of the current scramble
			safeSetValue(scrambleLength, current.getLength());
			//update new number of scrambles
			safeSetScrambleNumberMax(scramblesList.size());
			//update new scramble number
			safeSetValue(scrambleNumber, scramblesList.getScrambleNumber());
			scrambleArea.setScramble(current.getScramble(), scramblesList.getScrambleCustomization()); //this will update scramblePopup

			boolean canChangeStuff = scramblesList.size() == scramblesList.getScrambleNumber();
			scrambleChooser.setEnabled(canChangeStuff);
			scrambleLength.setEnabled(current.getLength() != 0 && canChangeStuff && !current.isImported());
		}
	}

	void prepareForProfileSwitch() {
		Profile p = profileDao.getSelectedProfile();
		try {
			profileDao.saveDatabase(p);
		} catch (TransformerConfigurationException | IOException | SAXException e1) {
			LOG.info("unexpected exception", e1);
		}
		saveToConfiguration();
		try {
			configuration.saveConfigurationToFile(p.getConfigurationFile());
		} catch (Exception e) {
			LOG.info("unexpected exception", e);
		}
	}

	private void saveToConfiguration() {
		configuration.setString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, scramblesList.getScrambleCustomization().toString());
		scramblePlugin.saveLengthsToConfiguration();
		for(ScramblePlugin plugin : scramblePlugin.getScramblePlugins())
			configuration.setStringArray(VariableKey.PUZZLE_ATTRIBUTES(plugin), plugin.getEnabledPuzzleAttributes());
		configuration.setPoint(VariableKey.SCRAMBLE_VIEW_LOCATION, scramblePopup.getLocation());
		configuration.setDimension(VariableKey.MAIN_FRAME_DIMENSION, this.getSize());
		configuration.setPoint(VariableKey.MAIN_FRAME_LOCATION, this.getLocation());
		if(client != null)
			client.saveToConfiguration();

		for(JSplitPane jsp : splitPanes)
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jsp.getName(), true, configuration.getXMLGUILayout()), jsp.getDividerLocation());
		for(JTabbedPane jtp : tabbedPanes)
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(jtp.getName(), true, configuration.getXMLGUILayout()), jtp.getSelectedIndex());
		timesTable.saveToConfiguration();
		sessionsTable.saveToConfiguration();
	}

	JFrame fullscreenFrame;
	boolean isFullscreen = false;
	void setFullScreen(boolean b) {
		isFullscreen = fullscreenFrame.isVisible();
		if(b == isFullscreen)
			return;
		isFullscreen = b;
		if(isFullscreen) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gs = ge.getScreenDevices();
			GraphicsDevice gd = gs[configuration.getInt(VariableKey.FULLSCREEN_DESKTOP, false)];
			DisplayMode screenSize = gd.getDisplayMode();
			fullscreenFrame.setSize(screenSize.getWidth(), screenSize.getHeight());
			fullscreenFrame.validate();
			bigTimersDisplay.requestFocusInWindow();
		}
		fullscreenFrame.setVisible(isFullscreen);
	}

	@Override
	public void tableChanged(TableModelEvent event) {
		final SolveTime latestTime = statsModel.getCurrentStatistics().get(-1);
		if(latestTime != null) {
			sendUserstate();
		}
		if(event != null && event.getType() == TableModelEvent.INSERT) {
			ScrambleString curr = scramblesList.getCurrent();
			latestTime.setScramble(curr.getScramble());
			boolean outOfScrambles = curr.isImported(); //This is tricky, think before you change it
			outOfScrambles = !scramblesList.getNext().isImported() && outOfScrambles;
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

			if(configuration.getBoolean(VariableKey.SPEAK_TIMES, false)) {
				new Thread(() -> {
                    try {
                        numberSpeaker.getCurrentSpeaker().speak(latestTime);
                    } catch (Exception e) {
                        LOG.info("unexpected exception", e);
                    }
                }).start();
			}
		}
		repaintTimes();
	}

	private Timer sendStateTimer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			actionMap.syncUserStateNOW();
			client.broadcastUserstate();
			sendStateTimer.stop();
		}
	});

	//this will start a timer to transmit the cct state every one second, if there's new information
	void sendUserstate() {
		if(client == null || !client.isConnected()) {
			sendStateTimer.stop();
			return;
		}
		if(!sendStateTimer.isRunning()) {
			sendStateTimer.start();
		}
	}

	private void loadXMLGUI() {
		SwingUtilities.invokeLater(() -> {
            refreshCustomGUIMenu();
            Component focusedComponent = CALCubeTimer.this.getFocusOwner();
            parseXML_GUI(configuration.getXMLGUILayout());
            Dimension size = configuration.getDimension(VariableKey.MAIN_FRAME_DIMENSION, false);
            if(size == null)
                CALCubeTimer.this.pack();
            else
                CALCubeTimer.this.setSize(size);
            Point location = configuration.getPoint(VariableKey.MAIN_FRAME_LOCATION, false);
            if(location == null)
                CALCubeTimer.this.setLocationRelativeTo(null);
            else {
                if(location.y < 0) //on windows, it is really bad if we let the window appear above the screen
                    location.y = 0;
                CALCubeTimer.this.setLocation(location);
            }
            CALCubeTimer.this.validate(); //this is needed to get the dividers to show up in the right place

            if(!configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) //This is to ensure that the keyboard is focused
                timeLabel.requestFocusInWindow();
            else if(focusedComponent != null)
                focusedComponent.requestFocusInWindow();
            else
                scrambleArea.requestFocusInWindow();
            timeLabel.componentResized(null);

            //dispose the old fullscreen frame, and create a new one
            if(fullscreenFrame != null)
                fullscreenFrame.dispose();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            GraphicsDevice gd = gs[configuration.getInt(VariableKey.FULLSCREEN_DESKTOP, false)];
            fullscreenFrame = new JFrame(gd.getDefaultConfiguration());
            //TODO - this is causing a nullpointer in SubstanceRootPaneUI, possible substance bug?
            fullscreenFrame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
            fullscreenFrame.setResizable(false);
            fullscreenFrame.setUndecorated(true);
            fullscreenFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            fullscreenPanel.putClientProperty(SubstanceLookAndFeel.WATERMARK_VISIBLE, Boolean.TRUE);
            fullscreenFrame.add(fullscreenPanel);
            setFullScreen(isFullscreen);

            repaintTimes();
            refreshActions();
        });
	}

	private void refreshActions(){
		boolean stackmatEnabled = configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false);
		AbstractAction a;
		if((a = actionMap.getRawAction("keyboardtiming")) != null) a.putValue(Action.SELECTED_KEY, !stackmatEnabled);
		if((a = actionMap.getRawAction("togglestatuslight")) != null) a.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY, false));
		if((a = actionMap.getRawAction("togglehidescrambles")) != null) a.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.HIDE_SCRAMBLES, false));
		if((a = actionMap.getRawAction("togglespacebarstartstimer")) != null) a.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false));
		if((a = actionMap.getRawAction("togglefullscreen")) != null) a.putValue(Action.SELECTED_KEY, configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false));
	}

	@Override
	public void configurationChanged(Profile currentProfile) {
		//we need to notify the security manager ourself, because it may not have any reference to
		//Configuration for the cctbot to work.
		Main.SCRAMBLE_SECURITY_MANAGER.configurationChanged();

		refreshActions();

		List<Profile> profiles = profileDao.getProfiles(configuration);
		profilesComboBox.setModel(new DefaultComboBoxModel<>(profiles.toArray(new Profile[profiles.size()])));
		safeSelectItem(profilesComboBox, profileDao.getSelectedProfile());
		languages.setSelectedItem(configuration.getDefaultLocale()); //this will force an update of the xml gui
		updateWatermark();

		scramblePlugin.reloadLengthsFromConfiguration(false);
		ScrambleCustomization newCustom = scramblePlugin.getCurrentScrambleCustomization(currentProfile);
		scrambleChooser.setSelectedItem(newCustom);
		
		//we need to notify the stackmatinterpreter package because it has been rewritten to
		//avoid configuration entirely (which makes it easier to separate & use as a library)
		StackmatState.setInverted(configuration.getBoolean(VariableKey.INVERTED_MINUTES, false),
				configuration.getBoolean(VariableKey.INVERTED_SECONDS, false),
				configuration.getBoolean(VariableKey.INVERTED_HUNDREDTHS, false));

		stackmatTimer.initialize(configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE, false),
				configuration.getInt(VariableKey.MIXER_NUMBER, false),
				configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false),
				configuration.getInt(VariableKey.SWITCH_THRESHOLD, false));
		configuration.setLong(VariableKey.MIXER_NUMBER, stackmatTimer.getSelectedMixerIndex());

		timingListener.stackmatChanged(); //force the stackmat label to refresh
	}

	// Actions section {{{
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
			timeLabel.reset();
			bigTimersDisplay.reset();
			scramblesList.clear();
			updateScramble();
			statsModel.getCurrentStatistics().clear();
		}
	}

	public void importScrambles(ScrambleVariation sv, List<Scramble> scrambles, Profile profile) {
		if(!((ScrambleCustomization)scrambleChooser.getSelectedItem()).getScrambleVariation().equals(sv))
			scramblesList.setScrambleCustomization(scramblePlugin.getCustomizationFromString(profile, "" + sv.toString()));
		scrambleChooser.setSelectedItem(scramblesList.getScrambleCustomization());
		scramblesList.importScrambles(scrambles);
		updateScramble();
	}
	public void importScrambles(ScrambleCustomization sc, ArrayList<Scramble> scrambles) {
		scramblesList.setScrambleCustomization(sc);
		scramblesList.importScrambles(scrambles);
		scrambleChooser.setSelectedItem(scramblesList.getScrambleCustomization());
		updateScramble();
	}

	public void exportScramblesAction(Profile selectedProfile) {
		new ScrambleExportDialog(this, scramblesList.getScrambleCustomization().getScrambleVariation(),
				scramblePlugin, configuration, selectedProfile, profileDao);
	}

	public void showDocumentation() {
		try {
			URI uri = configuration.getDocumentationFile();
			Desktop.getDesktop().browse(uri);
		} catch(Exception error) {
			Utils.showErrorDialog(this, error);
		}
	}

	public void showConfigurationDialog() {
		saveToConfiguration();
		if(configurationDialog == null) {
			configurationDialog = new ConfigurationDialog(this, true, configuration, profileDao, scramblePlugin, statsModel, numberSpeaker, stackmatTimer, tickTock, timesTable);
		}
		SwingUtilities.invokeLater(() -> {
            configurationDialog.syncGUIwithConfig(false);
            configurationDialog.setVisible(true);
        });
	}

	public void keyboardTimingAction() {
		boolean selected = (Boolean)actionMap.get("keyboardtiming").getValue(Action.SELECTED_KEY);
		configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !selected);
		timeLabel.configurationChanged(profileDao.getSelectedProfile());
		bigTimersDisplay.configurationChanged(profileDao.getSelectedProfile());
		stackmatTimer.enableStackmat(!selected);
		stopInspection();
		timeLabel.reset();
		bigTimersDisplay.reset();
		timingListener.stackmatChanged();
		if(selected)
			timeLabel.requestFocusInWindow();
		else
			timingListener.timerAccidentlyReset(null); //when the keyboard timer is disabled, we reset the timer
	}

	@NotNull
	Session createNewSession(Profile p, String customization) {
		PuzzleStatistics ps = p.getPuzzleDatabase().getPuzzleStatistics(customization);
		Session s = new Session(LocalDateTime.now(), configuration, scramblePlugin, statsModel);
		ps.addSession(s, p);
		return s;
	}

	public void statusLightAction(){
		configuration.setBoolean(VariableKey.LESS_ANNOYING_DISPLAY, (Boolean) actionMap.get("togglestatuslight").getValue(Action.SELECTED_KEY));
		timeLabel.repaint();
	}

	public void hideScramblesAction(){
		configuration.setBoolean(VariableKey.HIDE_SCRAMBLES, (Boolean) actionMap.get("togglehidescrambles").getValue(Action.SELECTED_KEY));
		scrambleArea.refresh();
	}

	public void requestScrambleAction(){
		scramblesList.getNext();
		updateScramble();
	}
	// End actions section }}}

	private long lastSplit;
	void addSplit(TimerState state) {
		long currentTime = System.currentTimeMillis();
		if((currentTime - lastSplit) / 1000. > configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)) {
			String hands = "";
			if(state instanceof StackmatState) {
				hands += ((StackmatState) state).leftHand() ? StringAccessor.getString("CALCubeTimer.lefthand") : StringAccessor.getString("CALCubeTimer.righthand");
			}
			splits.add(state.toSolveTime(hands, null));
			lastSplit = currentTime;
		}
	}

	void startMetronome() {
		tickTock.setDelay(configuration.getInt(VariableKey.METRONOME_DELAY, false));
		tickTock.start();
	}
	void stopMetronome() {
		tickTock.stop();
	}

	@Inject
	private StackmatState lastAccepted;

	private List<SolveTime> splits = new ArrayList<>();

	boolean addTime(TimerState addMe) {
		SolveTime protect = addMe.toSolveTime(null, splits);
		if(penalty == null)
			protect.clearType();
		else
			protect.setTypes(Arrays.asList(penalty));
		penalty = null;
		splits = new ArrayList<>();
		boolean sameAsLast = addMe.compareTo(lastAccepted) == 0;
		if(sameAsLast) {
			int choice = Utils.showYesNoDialog(this, addMe.toString() + "\n" + StringAccessor.getString("CALCubeTimer.confirmduplicate"));
			if(choice != JOptionPane.YES_OPTION)
				return false;
		}
		int choice = JOptionPane.YES_OPTION;
		if(configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME, false) && !sameAsLast) {
			String[] OPTIONS = { StringAccessor.getString("CALCubeTimer.accept"), SolveType.PLUS_TWO.toString(), SolveType.DNF.toString() };
			//This leaves +2 and DNF enabled, even if the user just got a +2 or DNF from inspection.
			//I don't really care however, since I doubt that anyone even uses this feature. --Jeremy
			choice = JOptionPane.showOptionDialog(null,
					StringAccessor.getString("CALCubeTimer.yourtime") + protect.toString() + StringAccessor.getString("CALCubeTimer.newtimedialog"),
					StringAccessor.getString("CALCubeTimer.confirm"),
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					OPTIONS,
					OPTIONS[0]);
		}
		switch (choice) {
			case JOptionPane.YES_OPTION:
				break;
			case JOptionPane.NO_OPTION:
				protect.setTypes(Arrays.asList(SolveType.PLUS_TWO));
				break;
			case JOptionPane.CANCEL_OPTION:
				protect.setTypes(Arrays.asList(SolveType.DNF));
				break;
			default:
				return false;
		}
		statsModel.getCurrentStatistics().add(protect);
		return true;
	}

	private static final long INSPECTION_TIME = 15;
	private static final long FIRST_WARNING = 8;
	private static final long FINAL_WARNING = 12;
	private long previousInpection = -1;
	//this returns the amount of inspection remaining (in seconds), and will speak to the user if necessary
	public long getInpectionValue() {
		long inspectionDone = Duration.between(inspectionStart, Instant.now()).getSeconds();
		if(inspectionDone != previousInpection && configuration.getBoolean(VariableKey.SPEAK_INSPECTION, false)) {
			previousInpection = inspectionDone;
			if(inspectionDone == FIRST_WARNING) {
				sayInspectionWarning(FIRST_WARNING);
			} else if(inspectionDone == FINAL_WARNING) {
				sayInspectionWarning(FINAL_WARNING);
			}
		}
		return INSPECTION_TIME - inspectionDone;
	}

	private void sayInspectionWarning(long seconds) {
		new Thread(() -> {
			try {
				numberSpeaker.getCurrentSpeaker().speak(false, seconds * 100);
			} catch (Exception e) {
				LOG.info("unexpected exception", e);
			}
		}).start();
	}

	Instant inspectionStart = null;
	Timer updateInspectionTimer = new Timer(90, e -> updateInspection());

	void stopInspection() {
		inspectionStart = null;
		updateInspectionTimer.stop();
	}
	boolean isInspecting() {
		return inspectionStart != null;
	}

	void updateInspection() {
		long inspection = getInpectionValue();
		String time;
		if(inspection <= -2) {
			penalty = SolveType.DNF;
			time = StringAccessor.getString("CALCubeTimer.disqualification");
		} else if(inspection <= 0) {
			penalty = SolveType.PLUS_TWO;
			time = StringAccessor.getString("CALCubeTimer.+2penalty");
		} else
			time = "" + inspection;
		timeLabel.setText(time);
		if(isFullscreen)
			bigTimersDisplay.setText(time);
	}

	SolveType penalty = null;

}

class StatisticsAction extends AbstractAction{
	private StatsDialogHandler statsHandler;
	private StatisticsTableModel model;
	private AverageType type;
	private int num;
	public StatisticsAction(CALCubeTimer cct, StatisticsTableModel model, AverageType type, int num,
							Configuration configuration){
		statsHandler = new StatsDialogHandler(cct, configuration);
		this.model = model;
		this.type = type;
		this.num = num;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		statsHandler.syncWithStats(model, type, num);
		statsHandler.setVisible(true);
	}
}
class AddTimeAction extends AbstractAction{
	private CALCubeTimer cct;
	public AddTimeAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.addTimeAction();
	}
}
class ResetAction extends AbstractAction{
	private CALCubeTimer cct;
	public ResetAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.resetAction();
	}
}
class ExportScramblesAction extends AbstractAction{
	private CALCubeTimer cct;
	private final ProfileDao profileDao;

	public ExportScramblesAction(CALCubeTimer cct, ProfileDao profileDao){
		this.cct = cct;
		this.profileDao = profileDao;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.exportScramblesAction(profileDao.getSelectedProfile());
	}
}
class ExitAction extends AbstractAction{
	private CALCubeTimer cct;
	public ExitAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.dispose();
	}
}
class AboutAction extends AbstractAction {
	private static final Logger LOG = Logger.getLogger(AboutAction.class);
	private AboutScrollFrame makeMeVisible;
	public AboutAction() {
		try {
			makeMeVisible = new AboutScrollFrame(CALCubeTimer.class.getResource("about.html"), CALCubeTimer.cubeIcon.getImage());
			setEnabled(true);
		} catch (Exception e1) {
			LOG.info("unexpected exception", e1);
			setEnabled(false);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e){
		makeMeVisible.setTitle(StringAccessor.getString("CALCubeTimer.about") + CALCubeTimer.CCT_VERSION);
		makeMeVisible.setVisible(true);
	}
}
class DocumentationAction extends AbstractAction{
	private CALCubeTimer cct;
	public DocumentationAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.showDocumentation();
	}
}

class ShowConfigurationDialogAction extends AbstractAction{
	private CALCubeTimer cct;
	public ShowConfigurationDialogAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.showConfigurationDialog();
	}
}

class KeyboardTimingAction extends AbstractAction{
	private CALCubeTimer cct;
	public KeyboardTimingAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.keyboardTimingAction();
	}
}
class SpacebarOptionAction extends AbstractAction{
	private final net.gnehzr.cct.configuration.Configuration configuration;

	public SpacebarOptionAction(Configuration configuration){
		this.configuration = configuration;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.SPACEBAR_ONLY, ((AbstractButton)e.getSource()).isSelected());
	}
}
class FullScreenTimingAction extends AbstractAction{
	private final Configuration configuration;

	public FullScreenTimingAction(Configuration configuration){
		this.configuration = configuration;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		configuration.setBoolean(VariableKey.FULLSCREEN_TIMING, ((AbstractButton)e.getSource()).isSelected());
	}
}
class HideScramblesAction extends AbstractAction{
	private CALCubeTimer cct;
	public HideScramblesAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.hideScramblesAction();
	}
}
class StatusLightAction extends AbstractAction{
	private CALCubeTimer cct;
	public StatusLightAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.statusLightAction();
	}
}
class RequestScrambleAction extends AbstractAction{
	private CALCubeTimer cct;
	public RequestScrambleAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.requestScrambleAction();
	}
}
