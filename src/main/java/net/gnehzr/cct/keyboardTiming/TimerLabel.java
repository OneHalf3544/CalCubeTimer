package net.gnehzr.cct.keyboardTiming;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.ConfigurationChangeListener;
import net.gnehzr.cct.configuration.JColorComponent;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CALCubeTimer;
import net.gnehzr.cct.main.ScrambleArea;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Profile;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

public class TimerLabel extends JColorComponent implements ComponentListener, ConfigurationChangeListener, FocusListener, KeyListener, MouseListener {

	private static final Logger LOG = Logger.getLogger(TimerLabel.class);

	private KeyboardHandler keyHandler;
	private ScrambleArea scrambleArea;
	private final Configuration configuration;

	public TimerLabel(ScrambleArea scrambleArea, Configuration configuration) {
		super("");
		this.scrambleArea = scrambleArea;
		this.configuration = configuration;
		addComponentListener(this);
		setFocusable(true);
		addFocusListener(this);
		addKeyListener(this);
		addMouseListener(this);
		setFocusTraversalKeysEnabled(false);
	}
	public void setKeyboardHandler(KeyboardHandler keyHandler) {
		this.keyHandler = keyHandler;
	}
	
	private static final Dimension MIN_SIZE = new Dimension(0, 150);
	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	@Override
	public Dimension getMinimumSize() {
		return MIN_SIZE;
	}
	@Override
	public Dimension getPreferredSize() {
		return MIN_SIZE;
	}

	private boolean keysDown;
	private boolean on;
	public void setStackmatOn(boolean on) {
		this.on = on;
		if(!on)
			leftHand = rightHand = greenLight = false;
		refreshTimer();
	}
	private boolean greenLight;
	public void setStackmatGreenLight(boolean greenLight) {
		this.greenLight = greenLight;
	}
	public void reset() {
		leftHand = rightHand = greenLight = on = false;
		keyHandler.reset();
		refreshTimer();
	}

	private TimerState time;

	public void setTime(TimerState time) {
		setForeground(configuration.getColor(VariableKey.TIMER_FG, false));
		this.time = time;
		super.setText(time.toString());
		componentResized(null);
	}
	public TimerState getTimerState() {
		return time;
	}
	@Override
	public void setText(String s) {
		setForeground(Color.RED);
		time = null;
		super.setText(s);
		componentResized(null);
	}
	@Override
	public void componentHidden(ComponentEvent arg0) {}
	@Override
	public void componentMoved(ComponentEvent arg0) {}

	private Font font;
	@Override
	public void setFont(Font font) {
		this.font = font;
		super.setFont(font);
	}
	
	@Override
	public void componentResized(ComponentEvent e) {
		if(font != null) { //this is to avoid an exception before showing the component
			String newTime = getText();
			Insets border = getInsets();
			Rectangle2D bounds = font.getStringBounds(newTime, new FontRenderContext(null, true, true));
			double height = (getHeight() - border.top - border.bottom) / bounds.getHeight();
			double width = (getWidth() - border.left - border.right) / (bounds.getWidth()+10);
			double ratio = Math.min(width, height);
			super.setFont(font.deriveFont(AffineTransform.getScaleInstance(ratio, ratio)));
		}
	}
	
	@Override
	public void componentShown(ComponentEvent arg0) {}

	private static BufferedImage curr, red, green;
	static {
		try { //can't use TimerLabel.class because the class hasn't been loaded yet
			red = ImageIO.read(CALCubeTimer.class.getResourceAsStream("red-button.png")); 
			green = ImageIO.read(CALCubeTimer.class.getResourceAsStream("green-button.png")); 
		} catch (IOException e) {
			LOG.info("unexpected exception", e);
		}
	}
	@Override
	public void paintComponent(Graphics g) {
		if(configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY, false))
			g.drawImage(curr, 10, 20, null);
		g.drawImage(getImageForHand(leftHand), 10, getHeight() - 50, null);
		g.drawImage(getImageForHand(rightHand), getWidth() - 50, getHeight() - 50, null);
		super.paintComponent(g);
	}
	private Boolean leftHand, rightHand;
	public void setHands(Boolean leftHand, Boolean rightHand) {
		this.leftHand = leftHand;
		this.rightHand = rightHand;
	}
	//see StackmatState for an explanation
	private BufferedImage getImageForHand(Boolean hand) {
		if(!on)
			return null;
		if(hand == null)
			return green;
		return hand ? red : null;
	}

	@Override
	public void configurationChanged(Profile profile) {
		setBackground(configuration.getColorNullIfInvalid(VariableKey.TIMER_BG, false));
		
		setFont(configuration.getFont(VariableKey.TIMER_FONT, false));
		if(time != null) //this will deal with any internationalization issues, if appropriate
			setTime(time);
		else
			setText(getText());
		refreshTimer();
	}
	@Override
	public void focusGained(FocusEvent e) {
		refreshTimer();
	}
	@Override
	public void focusLost(FocusEvent e) {
		refreshTimer();
	}
	
	private boolean canStartTimer() {
		boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION, false);
		boolean spacebarOnly = configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false);
		return keyHandler.canStartTimer() && keyHandler.isReset() && //checking if we are in a position to start the timer
			(stackmatEmulation && stackmatKeysDown() && atMostKeysDown(2) || //checking if the right keys are down for starting a "stackmat"
					!stackmatEmulation && atMostKeysDown(1) && (spacebarOnly && isKeyDown(KeyEvent.VK_SPACE) || !spacebarOnly)); //checking if the right keys are down to start the timer
	}

	private void refreshTimer() {
		boolean inspectionEnabled = configuration.getBoolean(VariableKey.COMPETITION_INSPECTION, false);
		String title;
		boolean keyboard = !configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false);

		Color borderColor = null;
		boolean lowered = false;
		if(keyboard) {
			boolean focused = isFocusOwner();
			scrambleArea.setTimerFocused(focused);
			if(focused) {
				curr = green;
				if(keysDown)
					lowered = true;
				if(keysDown && canStartTimer())
					borderColor = Color.GREEN;
				else
					borderColor = Color.RED;
				if(keyHandler.isRunning())
					title = StringAccessor.getString("TimerLabel.stoptimer"); 
				else if(keyHandler.isInspecting() || !inspectionEnabled)
					title = StringAccessor.getString("TimerLabel.starttimer"); 
				else
					title = StringAccessor.getString("TimerLabel.startinspection"); 
			} else {
				curr = red;
				title = StringAccessor.getString("TimerLabel.clickme"); 
				borderColor = Color.GRAY;
				releaseAllKeys();
			}
		} else {
			title = StringAccessor.getString("TimerLabel.keyboardoff"); 
			if(on) {
				curr = green;
				if(greenLight) {
					lowered = true;
					borderColor = Color.GREEN;
				} else {
					borderColor = Color.RED;
				}
			} else {
				curr = red;
				borderColor = Color.GRAY;
			}
		}
		Border b = BorderFactory.createBevelBorder(lowered ? BevelBorder.LOWERED : BevelBorder.RAISED, borderColor, borderColor.darker().darker());
		setBorder(BorderFactory.createTitledBorder(b, title, TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, null, Utils.invertColor(getBackground())));
		repaint();
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		requestFocusInWindow();
	}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	
	//What follows is some really nasty code to deal with linux and window's differing behavior for keyrepeats
	private Hashtable<Integer, Long> timeup = new Hashtable<>(KeyEvent.KEY_LAST);
	long getTime(int keycode) {
		Long temp = timeup.get(keycode);
		return (temp == null) ? 0 : temp;
	}
	Hashtable<Integer, Boolean> keyDown = new Hashtable<>(KeyEvent.KEY_LAST);
	boolean isKeyDown(int keycode) {
		Boolean temp = keyDown.get(keycode);
		return (temp == null) ? false : temp;
	}
	@Override
	public void keyReleased(final KeyEvent e) {
		if(configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) return;
		int code = e.getKeyCode();
		timeup.put(code, e.getWhen());
		ActionListener checkForKeyPress = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if(isKeyDown(keyCode) && getTime(keyCode) != 0) {
					keyDown.put(keyCode, false);
					keyReallyReleased(e);
				}
				((Timer) evt.getSource()).stop();
			}
			private int keyCode;
			public ActionListener setKeyCode(int keyCode) {
				this.keyCode = keyCode;
				return this;
			}
		}.setKeyCode(code);

		new Timer(10, checkForKeyPress).start();
	}

	@Override
	public void keyPressed(final KeyEvent e) {
		if(configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false)) return;
		int code = e.getKeyCode();
		if (e.getWhen() - getTime(code) < 10) {
			timeup.put(code, (long) 0);
		} else if(!isKeyDown(code)){
			keyDown.put(code, true);
			keyReallyPressed(e);
		}
		refreshTimer();
	}
	@Override
	public void keyTyped(KeyEvent e) {}

	private boolean atMostKeysDown(int count){
		Enumeration<Boolean> keys = keyDown.elements();
		while(keys.hasMoreElements())
			if(keys.nextElement()) if(--count < 0) return false;
		return true;
	}

	private int stackmatKeysDownCount(){
		return (isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1, false).intValue()) ? 1 : 0) +
			(isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2, false).intValue()) ? 1 : 0);
	}

	private boolean stackmatKeysDown(){
		return isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1, false).intValue()) &&
			isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2, false).intValue());
	}

	//called when a key is physically pressed
	private void keyReallyPressed(KeyEvent e) {
		boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION, false);
		int key = e.getKeyCode();
		if(key == 0) { //ignore unrecognized keys, such as media buttons
		} else if(keyHandler.isRunning() && !keyHandler.isInspecting()) {
			if(configuration.getBoolean(VariableKey.TIMING_SPLITS, false) && key == configuration.getInt(VariableKey.SPLIT_KEY, false)) {
				keyHandler.split();
			} else if(!stackmatEmulation || stackmatEmulation && stackmatKeysDown()){
				keyHandler.stop();
				keysDown = true;
			}
		} else if(key == KeyEvent.VK_ESCAPE) {
			releaseAllKeys();
		} else if(!stackmatEmulation) {
			keysDown = true;
		}
	}
	
	//this will release all keys that we think are down
	private void releaseAllKeys() {
		keyDown.clear();
		timeup.clear();
		keysDown = false;
	}

	//called when a key is physically released
	void keyReallyReleased(KeyEvent e) {
		boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION, false);
		int sekey1 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1, false).intValue();
		int sekey2 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2, false).intValue();

		if(stackmatEmulation && stackmatKeysDownCount() == 1 && (e.getKeyCode() == sekey1 || e.getKeyCode() == sekey2) || !stackmatEmulation && atMostKeysDown(0)){
			keysDown = false;
			if(!keyHandler.isRunning() || keyHandler.isInspecting()) {
				if(!keyHandler.isReset()) {
					keyHandler.fireStop();
				} else if(!ignoreKey(e, configuration.getBoolean(VariableKey.SPACEBAR_ONLY, false), stackmatEmulation, sekey1, sekey2)) {
					keyHandler.startTimer();
				}
			}
		}
		refreshTimer();
	}

	public static boolean ignoreKey(KeyEvent e, boolean spaceBarOnly, boolean stackmatEmulation, int sekey1, int sekey2) {
		int key = e.getKeyCode();
		if(stackmatEmulation){
			return key != sekey1 && key != sekey2;
		}
		if(spaceBarOnly)
			return key != KeyEvent.VK_SPACE;
		return key != KeyEvent.VK_ENTER && (key > 123 || key < 23 || e.isAltDown() || e.isControlDown() || key == KeyEvent.VK_ESCAPE);
	}
}
