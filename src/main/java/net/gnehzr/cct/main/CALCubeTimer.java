package net.gnehzr.cct.main;

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
import net.gnehzr.cct.misc.dynamicGUI.*;
import net.gnehzr.cct.scrambles.*;
import net.gnehzr.cct.scrambles.ScrambleList.ScrambleString;
import net.gnehzr.cct.speaking.NumberSpeaker;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.*;
import net.gnehzr.cct.statistics.Statistics.AverageType;
import net.gnehzr.cct.statistics.Statistics.CCTUndoableEdit;
import net.gnehzr.cct.umts.cctbot.CCTUser;
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
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

//import sun.awt.AppContext;

public class CALCubeTimer extends JFrame implements ActionListener, TableModelListener, ChangeListener, ConfigurationChangeListener, ItemListener, SessionListener {

	private static final Logger LOG = Logger.getLogger(CALCubeTimer.class);

	public static final String CCT_VERSION = CALCubeTimer.class.getPackage().getImplementationVersion();
	public static final ImageIcon cubeIcon = new ImageIcon(CALCubeTimer.class.getResource("cube.png"));

	public final static StatisticsTableModel statsModel = new StatisticsTableModel(); //used in ProfileDatabase

	JLabel onLabel = null;
	DraggableJTable timesTable = null;
	private JScrollPane timesScroller = null;
	private SessionsTable sessionsTable = null;
	private JScrollPane sessionsScroller = null;
	ScrambleArea scrambleArea = null;
	private ScrambleChooserComboBox scrambleChooser = null;
	private JPanel scrambleAttributes = null;
	private JTextField generator;
	JSpinner scrambleNumber;
	private JSpinner scrambleLength = null;
	private DateTimeLabel currentTimeLabel = null;
	private JComboBox profiles = null;
	private LoudComboBox languages = null;
	TimerLabel timeLabel = null;
	//all of the above components belong in this HashMap, so we can find them
	//when they are referenced in the xml gui (type="blah...blah")
	//we also reset their attributes before parsing the xml gui
	ComponentsMap persistentComponents;

	TimerLabel bigTimersDisplay = null;
	JLayeredPane fullscreenPanel = null;
	ScrambleFrame scramblePopup = null;
	ScrambleList scramblesList = new ScrambleList();
	StackmatInterpreter stackmatTimer = null;
	IRCClientGUI client;
	ConfigurationDialog configurationDialog;
	private TimingListener timingListener;
	boolean timing = false;

	public CALCubeTimer() {
		this.setUndecorated(true);
		createActions();
		initializeGUIComponents();
		stackmatTimer.execute();
	}

	public void setSelectedProfile(Profile p) {
		profiles.setSelectedItem(p);
	}

	ActionMap actionMap;
	class ActionMap{
		private CALCubeTimer cct;
		private HashMap<String, AbstractAction> actionMap;

		public ActionMap(CALCubeTimer cct){
			this.cct = cct;
			actionMap = new HashMap<>();
		}

		public void put(String s, AbstractAction a){
			actionMap.put(s.toLowerCase(), a);
		}

		public AbstractAction get(String s){
			s = s.toLowerCase();
			AbstractAction a = actionMap.get(s);
			if(a == null){
				a = initialize(s);
				actionMap.put(s, a);
			}
			return a;
		}

		public AbstractAction getRawAction(String s){
			return actionMap.get(s.toLowerCase());
		}

		private AbstractAction initialize(String s){
			AbstractAction a = null;
			switch (s) {
				case "keyboardtiming":
					a = new KeyboardTimingAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_K);
					a.putValue(Action.SHORT_DESCRIPTION, StringAccessor.getString("CALCubeTimer.stackmatnote"));
					break;
				case "addtime":
					a = new AddTimeAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
					a.putValue(Action.ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
					break;
				case "reset":
					a = new ResetAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
					break;
				case "currentaverage0":
					a = new StatisticsAction(cct, statsModel, AverageType.CURRENT, 0);
					break;
				case "bestaverage0":
					a = new StatisticsAction(cct, statsModel, AverageType.RA, 0);
					break;
				case "currentaverage1":
					a = new StatisticsAction(cct, statsModel, AverageType.CURRENT, 1);
					break;
				case "bestaverage1":
					a = new StatisticsAction(cct, statsModel, AverageType.RA, 1);
					break;
				case "sessionaverage":
					a = new StatisticsAction(cct, statsModel, AverageType.SESSION, 0);
					break;
				case "togglefullscreen":
					a = new AbstractAction() {
						{
							putValue(Action.NAME, "+");
						}

						@Override
						public void actionPerformed(ActionEvent e) {
							setFullScreen(!isFullscreen);
						}
					};
					break;
				case "importscrambles":
					a = new AbstractAction() {
						{
							putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
							putValue(Action.ACCELERATOR_KEY,
									KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
						}

						@Override
						public void actionPerformed(ActionEvent e) {
							new ScrambleImportDialog(cct, scramblesList.getScrambleCustomization());
						}
					};
					break;
				case "exportscrambles":
					a = new ExportScramblesAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
					a.putValue(Action.ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
					break;
				case "connecttoserver":
					a = new AbstractAction() {
						{
							putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
							putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
						}

						@Override
						public void actionPerformed(ActionEvent e) {
							if (e == null) { //this means that the client gui was disposed
								this.setEnabled(true);
							} else {
								if (client == null) {
									client = new IRCClientGUI(cct, this);
									syncUserStateNOW();
								}
								client.setVisible(true);
								this.setEnabled(false);
							}
						}
					};
					break;
				case "showconfiguration":
					a = new ShowConfigurationDialogAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
					a.putValue(Action.ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
					break;
				case "exit":
					a = new ExitAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_X);
					a.putValue(Action.ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
					break;
				case "togglestatuslight":
					a = new StatusLightAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
					break;
				case "togglehidescrambles":
					a = new HideScramblesAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_H);
					break;
				case "togglespacebarstartstimer":
					a = new SpacebarOptionAction();
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
					break;
				case "togglefullscreentiming":
					a = new FullScreenTimingAction();
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
					break;
				case "togglescramblepopup":
					a = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							Configuration.setBoolean(VariableKey.SCRAMBLE_POPUP, ((AbstractButton) e.getSource()).isSelected());
							scramblePopup.refreshPopup();
						}
					};
					break;
				case "undo":
					a = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (timesTable.isEditing())
								return;
							if (statsModel.getCurrentStatistics().undo()) { //should decrement 1 from scramblenumber if possible
								Object prev = scrambleNumber.getPreviousValue();
								if (prev != null) {
									scrambleNumber.setValue(prev);
								}
							}
						}
					};
					a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
					break;
				case "redo":
					a = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (timesTable.isEditing())
								return;
							statsModel.getCurrentStatistics().redo();
						}
					};
					a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
					break;
				case "submitsundaycontest":
					final SundayContestDialog submitter = new SundayContestDialog(cct);
					a = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							submitter.syncWithStats(statsModel.getCurrentStatistics(), AverageType.CURRENT, 0);
							submitter.setVisible(true);
						}
					};
					break;
				case "newsession":
					a = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							if (statsModel.getRowCount() > 0) { //only create a new session if we've added any times to the current one
								statsModel.setSession(createNewSession(Configuration.getSelectedProfile(), scramblesList.getScrambleCustomization().toString()));
								timeLabel.reset();
								scramblesList.clear();
								updateScramble();
							}
						}
					};
					break;
				case "showdocumentation":
					a = new DocumentationAction(cct);
					break;
				case "showabout":
					a = new AboutAction();
					break;
				case "requestscramble":
					a = new RequestScrambleAction(cct);
					a.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
					break;
			}
			return a;
		}
	}
	private void createActions(){
		actionMap = new ActionMap(this);

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
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(e.getActionCommand().equals(SCRAMBLE_ATTRIBUTE_CHANGED)) {
			ArrayList<String> attrs = new ArrayList<>();
			for(DynamicCheckBox attr : attributes)
				if(attr.isSelected())
					attrs.add(attr.getDynamicString().getRawText());
			String[] attributes = attrs.toArray(new String[attrs.size()]);
			scramblesList.getScrambleCustomization().getScramblePlugin().setEnabledPuzzleAttributes(attributes);
			updateScramble();
		} else if(e.getActionCommand().equals(GUI_LAYOUT_CHANGED)) {
			saveToConfiguration();
			String layout = ((JRadioButtonMenuItem) source).getText();
			Configuration.setString(VariableKey.XML_LAYOUT, layout);
			parseXML_GUI(Configuration.getXMLFile(layout));
			this.pack();
			this.setLocationRelativeTo(null);
			for(JSplitPane pane : splitPanes) { //the call to pack() is messing up the jsplitpanes
				pane.setDividerLocation(pane.getResizeWeight());
				Integer divide = Configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true), false);
				if(divide != null)
					pane.setDividerLocation(divide);
			}
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
	private void initializeGUIComponents() {
		//NOTE: all internationalizable text must go in the loadStringsFromDefaultLocale() method
		tickTock = createTickTockTimer();

		currentTimeLabel = new DateTimeLabel();

		scrambleChooser = new ScrambleChooserComboBox(true, true);
		scrambleChooser.addItemListener(this);

		scrambleNumber = new JSpinner(new SpinnerNumberModel(1,	1, 1, 1));
		((JSpinner.DefaultEditor) scrambleNumber.getEditor()).getTextField().setColumns(3);
		scrambleNumber.addChangeListener(this);

		scrambleLength = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		((JSpinner.DefaultEditor) scrambleLength.getEditor()).getTextField().setColumns(3);
		scrambleLength.addChangeListener(this);

		scrambleAttributes = new JPanel();
		generator = new GeneratorTextField();

		scramblePopup = new ScrambleFrame(this, actionMap.get("togglescramblepopup"), false);
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

		timesTable = new DraggableJTable(false, true);
//		timesTable.setFocusable(false); //Man, this is almost perfect for us
		timesTable.setName("timesTable");
		timesTable.setDefaultEditor(SolveTime.class, new SolveTimeEditor());
		timesTable.setDefaultRenderer(SolveTime.class, new SolveTimeRenderer(statsModel));
		timesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		timesTable.setModel(statsModel);
		//TODO - this wastes space, probably not easy to fix...
		timesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		timesScroller = new JScrollPane(timesTable);

		sessionsTable = new SessionsTable(statsModel);
		sessionsTable.setName("sessionsTable");
		//TODO - this wastes space, probably not easy to fix...
		sessionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		sessionsScroller = new JScrollPane(sessionsTable);
		sessionsTable.setSessionListener(this);

		scrambleArea = new ScrambleArea(scramblePopup);
		scrambleArea.setAlignmentX(.5f);

		stackmatTimer = new StackmatInterpreter(Configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE, false),
				Configuration.getInt(VariableKey.MIXER_NUMBER, false),
				Configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false),
				Configuration.getInt(VariableKey.SWITCH_THRESHOLD, false));
		timingListener = new TimingListenerImpl(this);
		new StackmatHandler(timingListener, stackmatTimer);

		timeLabel = new TimerLabel(scrambleArea);
		bigTimersDisplay = new TimerLabel(scrambleArea);

		KeyboardHandler keyHandler = new KeyboardHandler(timingListener);
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

		profiles = new LoudComboBox();
		profiles.addItemListener(this);

		languages = new LoudComboBox();
		languages.setModel(new DefaultComboBoxModel(Configuration.getAvailableLocales().toArray()));
		languages.addItemListener(this);
		languages.setRenderer(new LocaleRenderer());

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
		persistentComponents.put("profilecombobox", profiles);
		persistentComponents.put("sessionslist", sessionsScroller);
		persistentComponents.put("clock", currentTimeLabel);
	}

    private Timer createTickTockTimer() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(Configuration.getString(VariableKey.METRONOME_CLICK_FILE, false));
            checkNotNull(inputStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
            DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
            Timer timer = new Timer(1000, new ActionListener() {
                int i = 0;

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
		for(File file : Configuration.getXMLLayoutsAvailable()) {
			JRadioButtonMenuItem temp = new JRadioButtonMenuItem(file.getName());
			temp.setSelected(file.equals(Configuration.getXMLGUILayout()));
			temp.setActionCommand(GUI_LAYOUT_CHANGED);
			temp.addActionListener(this);
			group.add(temp);
			customGUIMenu.add(temp);
		}
	}

	//if we deleted the current session, should we create a new one, or load the "nearest" session?
	private Session getNextSession() {
		Session nextSesh = statsModel.getCurrentSession();
		Profile p = Configuration.getSelectedProfile();
		String customization = scramblesList.getScrambleCustomization().toString();
		ProfileDatabase pd = p.getPuzzleDatabase();
		PuzzleStatistics ps = pd.getPuzzleStatistics(customization);
		if(!ps.containsSession(nextSesh)) {
			//failed to find a session to continue, so load newest session
			int sessionCount = pd.getRowCount();
			if(sessionCount > 0) {
				nextSesh = Session.OLDEST_SESSION;
				for(int ch = 0; ch < sessionCount; ch++) {
					Session s = pd.getNthSession(ch);
					if(s.getStatistics().getStartDate().after(nextSesh.getStatistics().getStartDate()))
						nextSesh = s;
				}
			} else { //create new session if none exist
				nextSesh = createNewSession(p, customization);
			}
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
			if(!customizationEditsDisabled && s != null) //TODO - changing session? //TODO - deleted customization?
				s.editActions.add(new CustomizationEdit(scramblesList.getScrambleCustomization(), (ScrambleCustomization) scrambleChooser.getSelectedItem()));
			
			scramblesList.setScrambleCustomization((ScrambleCustomization) scrambleChooser.getSelectedItem());
			//send current customization to irc, if connected
			sendUserstate();
			
			//change current session's scramble customization
			if(statsModel.getCurrentSession() != null) {
				statsModel.getCurrentSession().setCustomization(scramblesList.getScrambleCustomization().toString());
			}
			
			//update new scramble generator
			generator.setText(scramblesList.getScrambleCustomization().getGenerator());
			generator.setVisible(scramblesList.getScrambleCustomization().getScramblePlugin().isGeneratorEnabled());
			
			createScrambleAttributes();
			updateScramble();
		} else if(source == profiles) {
			Profile affected = (Profile)e.getItem();
			if(e.getStateChange() == ItemEvent.DESELECTED) {
				prepareForProfileSwitch();
			} else if(e.getStateChange() == ItemEvent.SELECTED) {
				statsModel.removeTableModelListener(this); //we don't want to know about the loading of the most recent session, or we could possibly hear it all spoken

				Configuration.setSelectedProfile(affected);
				if (!ProfileDao.INSTANCE.loadDatabase(affected)) {
					//the user will be notified of this in the profiles combobox
				}
				try {
					Configuration.loadConfiguration(affected.getConfigurationFile());
					Configuration.apply();
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
					Configuration.setDefaultLocale(newLocale);
					languages.setFont(Configuration.getFontForLocale(newLocale)); //for some reason, this cannot be put in the invokeLater() below
					SwingUtilities.invokeLater(this::loadStringsFromDefaultLocale);
				}
			}
		}
	}

	private static boolean loading = false;
	public static void setWaiting(boolean loading) {
		CALCubeTimer.loading = loading;
		cct.setCursor(null);
	}

	private LocaleAndIcon loadedLocale;
	void loadStringsFromDefaultLocale() {
		Utils.doInWaitingState(() -> {
			//this loads the strings for the swing components we use (JColorChooser and JFileChooser)
			UIManager.getDefaults().setDefaultLocale(Locale.getDefault());
			//		AppContext.getAppContext().put("JComponent.defaultLocale", Locale.getDefault());
			try {
				ResourceBundle messages = ResourceBundle.getBundle("languages/javax_swing");
				for(String key : messages.keySet())
					UIManager.put(key, messages.getString(key));
			} catch(MissingResourceException e) {
				LOG.info("unexpected exception", e);
			}

			StringAccessor.clearResources();
			XMLGuiMessages.reloadResources();
			statsModel.fireStringUpdates(); //this is necessary to update the undo-redo actions
//		timeLabel.refreshTimer(); //this is inside of parse_xml

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
		timeLabel.configurationChanged();
		bigTimersDisplay.configurationChanged();

		XMLGuiMessages.reloadResources();

		DefaultHandler handler = new GUIParser(this, this);
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
			Integer divide = Configuration.getInt(VariableKey.JCOMPONENT_VALUE(pane.getName(), true), false);
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
		if(sc == null)	return;
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
			attributes[ch] = new DynamicCheckBox(new DynamicString(attrs[ch], statsModel, ScramblePluginMessages.SCRAMBLE_ACCESSOR));
			attributes[ch].setSelected(selected);
			attributes[ch].setFocusable(Configuration.getBoolean(VariableKey.FOCUSABLE_BUTTONS, false));
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

	static CALCubeTimer cct; //need this instance to be able to easily set the waiting cursor

	//this happens in windows when alt+f4 is pressed
	@Override
	public void dispose() {
		super.dispose();
		System.exit(0);
	}

	static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(new org.jvnet.substance.skin.SubstanceModerateLookAndFeel());
		} catch (Exception e1) {
			LOG.info("unexpected exception", e1);
		}
		updateWatermark();
	}
	private static void updateWatermark() {
		SubstanceWatermark sw;
		if(Configuration.getBoolean(VariableKey.WATERMARK_ENABLED, false)) {
			InputStream in;
			try {
				in = new FileInputStream(Configuration.getString(VariableKey.WATERMARK_FILE, false));
			} catch (FileNotFoundException e) {
				in = CALCubeTimer.class.getResourceAsStream(Configuration.getString(VariableKey.WATERMARK_FILE, true));
			}
			SubstanceImageWatermark siw = new SubstanceImageWatermark(in);
			siw.setKind(SubstanceConstants.ImageWatermarkKind.APP_CENTER);
			siw.setOpacity(Configuration.getFloat(VariableKey.OPACITY, false));
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
		Profile p = Configuration.getSelectedProfile();
		try {
			ProfileDao.INSTANCE.saveDatabase(p);
		} catch (TransformerConfigurationException | IOException | SAXException e1) {
			LOG.info("unexpected exception", e1);
		}
		saveToConfiguration();
		try {
			Configuration.saveConfigurationToFile(p.getConfigurationFile());
		} catch (Exception e) {
			LOG.info("unexpected exception", e);
		}
	}

	private void saveToConfiguration() {
		Configuration.setString(VariableKey.DEFAULT_SCRAMBLE_CUSTOMIZATION, scramblesList.getScrambleCustomization().toString());
		ScramblePlugin.saveLengthsToConfiguration();
		for(ScramblePlugin plugin : ScramblePlugin.getScramblePlugins())
			Configuration.setStringArray(VariableKey.PUZZLE_ATTRIBUTES(plugin), plugin.getEnabledPuzzleAttributes());
		Configuration.setPoint(VariableKey.SCRAMBLE_VIEW_LOCATION, scramblePopup.getLocation());
		Configuration.setDimension(VariableKey.MAIN_FRAME_DIMENSION, this.getSize());
		Configuration.setPoint(VariableKey.MAIN_FRAME_LOCATION, this.getLocation());
		if(client != null)
			client.saveToConfiguration();

		for(JSplitPane jsp : splitPanes)
			Configuration.setInt(VariableKey.JCOMPONENT_VALUE(jsp.getName(), true), jsp.getDividerLocation());
		for(JTabbedPane jtp : tabbedPanes)
			Configuration.setInt(VariableKey.JCOMPONENT_VALUE(jtp.getName(), true), jtp.getSelectedIndex());
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
			GraphicsDevice gd = gs[Configuration.getInt(VariableKey.FULLSCREEN_DESKTOP, false)];
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
			if(outOfScrambles)
				Utils.showWarningDialog(this,
						StringAccessor.getString("CALCubeTimer.outofimported") +
						StringAccessor.getString("CALCubeTimer.generatedscrambles"));
			updateScramble();
			//make the new time visible
			timesTable.invalidate(); //the table needs to be invalidated to force the new time to "show up"!!!
			Rectangle newTimeRect = timesTable.getCellRect(statsModel.getRowCount(), 0, true);
			timesTable.scrollRectToVisible(newTimeRect);

			if(Configuration.getBoolean(VariableKey.SPEAK_TIMES, false)) {
				new Thread(() -> {
                    try {
                        NumberSpeaker.getCurrentSpeaker().speak(latestTime);
                    } catch (Exception e) {
                        LOG.info("unexpected exception", e);
                    }
                }).start();
			}
		}
		repaintTimes();
	}

//	private boolean userStateDirty = false;
	private Timer sendStateTimer = new Timer(1000, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
//			if(!userStateDirty) return;
			syncUserStateNOW();
			client.broadcastUserstate();
//			userStateDirty = false;
			sendStateTimer.stop();
		}
	});
	
	//this will sync the cct state with the client, but will not transmit the data to other users
	private void syncUserStateNOW() {
		CCTUser myself = client.getMyUserstate();
		myself.setCustomization(scrambleChooser.getSelectedItem().toString());
		
		myself.setLatestTime(statsModel.getCurrentStatistics().get(-1));
		
		TimerState state = timeLabel.getTimerState();
		if(!timing) {
			state = null;
		}
		myself.setTimingState(isInspecting(), state);
		
		Statistics stats = statsModel.getCurrentStatistics();
		myself.setCurrentRA(stats.average(AverageType.CURRENT, 0), stats.toTerseString(AverageType.CURRENT, 0, true));
		myself.setBestRA(stats.average(AverageType.RA, 0), stats.toTerseString(AverageType.RA, 0, false));
		myself.setSessionAverage(new SolveTime(stats.getSessionAvg(), null));
		
		myself.setSolvesAttempts(stats.getSolveCount(), stats.getAttemptCount());
		
		myself.setRASize(stats.getRASize(0));
	}
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
            parseXML_GUI(Configuration.getXMLGUILayout());
            Dimension size = Configuration.getDimension(VariableKey.MAIN_FRAME_DIMENSION, false);
            if(size == null)
                CALCubeTimer.this.pack();
            else
                CALCubeTimer.this.setSize(size);
            Point location = Configuration.getPoint(VariableKey.MAIN_FRAME_LOCATION, false);
            if(location == null)
                CALCubeTimer.this.setLocationRelativeTo(null);
            else {
                if(location.y < 0) //on windows, it is really bad if we let the window appear above the screen
                    location.y = 0;
                CALCubeTimer.this.setLocation(location);
            }
            CALCubeTimer.this.validate(); //this is needed to get the dividers to show up in the right place

            if(!Configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) //This is to ensure that the keyboard is focused
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
            GraphicsDevice gd = gs[Configuration.getInt(VariableKey.FULLSCREEN_DESKTOP, false)];
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
		boolean stackmatEnabled = Configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false);
		AbstractAction a;
		if((a = actionMap.getRawAction("keyboardtiming")) != null) a.putValue(Action.SELECTED_KEY, !stackmatEnabled);
		if((a = actionMap.getRawAction("togglestatuslight")) != null) a.putValue(Action.SELECTED_KEY, Configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY, false));
		if((a = actionMap.getRawAction("togglehidescrambles")) != null) a.putValue(Action.SELECTED_KEY, Configuration.getBoolean(VariableKey.HIDE_SCRAMBLES, false));
		if((a = actionMap.getRawAction("togglespacebarstartstimer")) != null) a.putValue(Action.SELECTED_KEY, Configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false));
		if((a = actionMap.getRawAction("togglefullscreen")) != null) a.putValue(Action.SELECTED_KEY, Configuration.getBoolean(VariableKey.FULLSCREEN_TIMING, false));
	}

	@Override
	public void configurationChanged() {
		//we need to notify the security manager ourself, because it may not have any reference to
		//Configuration for the cctbot to work.
		Main.SCRAMBLE_SECURITY_MANAGER.configurationChanged();

		refreshActions();
		
		profiles.setModel(new DefaultComboBoxModel(Configuration.getProfiles().toArray()));
		safeSelectItem(profiles, Configuration.getSelectedProfile());
		languages.setSelectedItem(Configuration.getDefaultLocale()); //this will force an update of the xml gui
		updateWatermark();

		ScramblePlugin.reloadLengthsFromConfiguration(false);
		ScrambleCustomization newCustom = ScramblePlugin.getCurrentScrambleCustomization();
		scrambleChooser.setSelectedItem(newCustom);
		
		//we need to notify the stackmatinterpreter package because it has been rewritten to
		//avoid configuration entirely (which makes it easier to separate & use as a library)
		StackmatState.setInverted(Configuration.getBoolean(VariableKey.INVERTED_MINUTES, false),
				Configuration.getBoolean(VariableKey.INVERTED_SECONDS, false),
				Configuration.getBoolean(VariableKey.INVERTED_HUNDREDTHS, false));

		stackmatTimer.initialize(Configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE, false),
				Configuration.getInt(VariableKey.MIXER_NUMBER, false),
				Configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false), 
				Configuration.getInt(VariableKey.SWITCH_THRESHOLD, false));
		Configuration.setInt(VariableKey.MIXER_NUMBER, stackmatTimer.getSelectedMixerIndex());

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

	public void importScrambles(ScrambleVariation sv, ArrayList<Scramble> scrambles) {
		if(!((ScrambleCustomization)scrambleChooser.getSelectedItem()).getScrambleVariation().equals(sv))
			scramblesList.setScrambleCustomization(ScramblePlugin.getCustomizationFromString("" + sv.toString()));
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

	public void exportScramblesAction() {
		new ScrambleExportDialog(this, scramblesList.getScrambleCustomization().getScrambleVariation());
	}

	public void showDocumentation() {
		try {
			URI uri = Configuration.documentationFile.toURI();
			Desktop.getDesktop().browse(uri);
		} catch(Exception error) {
			Utils.showErrorDialog(this, error);
		}
	}

	public void showConfigurationDialog() {
		saveToConfiguration();
		if(configurationDialog == null)
			configurationDialog = new ConfigurationDialog(this, true, stackmatTimer, tickTock, timesTable);
		SwingUtilities.invokeLater(() -> {
            configurationDialog.syncGUIwithConfig(false);
            configurationDialog.setVisible(true);
        });
	}

	public void keyboardTimingAction() {
		boolean selected = (Boolean)actionMap.get("keyboardtiming").getValue(Action.SELECTED_KEY);
		Configuration.setBoolean(VariableKey.STACKMAT_ENABLED, !selected);
		timeLabel.configurationChanged();
		bigTimersDisplay.configurationChanged();
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
		Session s = new Session(new Date());
		ps.addSession(s);
		return s;
	}

	public void statusLightAction(){
		Configuration.setBoolean(VariableKey.LESS_ANNOYING_DISPLAY, (Boolean)actionMap.get("togglestatuslight").getValue(Action.SELECTED_KEY));
		timeLabel.repaint();
	}

	public void hideScramblesAction(){
		Configuration.setBoolean(VariableKey.HIDE_SCRAMBLES, (Boolean)actionMap.get("togglehidescrambles").getValue(Action.SELECTED_KEY));
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
		if((currentTime - lastSplit) / 1000. > Configuration.getDouble(VariableKey.MIN_SPLIT_DIFFERENCE, false)) {
			String hands = "";
			if(state instanceof StackmatState) {
				hands += ((StackmatState) state).leftHand() ? StringAccessor.getString("CALCubeTimer.lefthand") : StringAccessor.getString("CALCubeTimer.righthand");
			}
			splits.add(state.toSolveTime(hands, null));
			lastSplit = currentTime;
		}
	}

	void startMetronome() {
		tickTock.setDelay(Configuration.getInt(VariableKey.METRONOME_DELAY, false));
		tickTock.start();
	}
	void stopMetronome() {
		tickTock.stop();
	}

	private StackmatState lastAccepted = new StackmatState();

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
		if(Configuration.getBoolean(VariableKey.PROMPT_FOR_NEW_TIME, false) && !sameAsLast) {
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
		if(inspectionDone != previousInpection && Configuration.getBoolean(VariableKey.SPEAK_INSPECTION, false)) {
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
				NumberSpeaker.getCurrentSpeaker().speak(false, seconds * 100);
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
	public StatisticsAction(CALCubeTimer cct, StatisticsTableModel model, AverageType type, int num){
		statsHandler = new StatsDialogHandler(cct);
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
	public ExportScramblesAction(CALCubeTimer cct){
		this.cct = cct;
	}

	@Override
	public void actionPerformed(ActionEvent e){
		cct.exportScramblesAction();
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
	public SpacebarOptionAction(){
	}

	@Override
	public void actionPerformed(ActionEvent e){
		Configuration.setBoolean(VariableKey.SPACEBAR_ONLY, ((AbstractButton)e.getSource()).isSelected());
	}
}
class FullScreenTimingAction extends AbstractAction{
	public FullScreenTimingAction(){
	}

	@Override
	public void actionPerformed(ActionEvent e){
		Configuration.setBoolean(VariableKey.FULLSCREEN_TIMING, ((AbstractButton)e.getSource()).isSelected());
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
