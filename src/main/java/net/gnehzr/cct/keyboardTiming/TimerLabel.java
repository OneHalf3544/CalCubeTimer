package net.gnehzr.cct.keyboardTiming;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.JColorComponent;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CalCubeTimerGui;
import net.gnehzr.cct.main.ScrambleHyperlinkArea;
import net.gnehzr.cct.main.SolvingProcess;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;
import net.gnehzr.cct.stackmatInterpreter.TimerState;
import net.gnehzr.cct.statistics.Profile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class TimerLabel extends JColorComponent {

	private static final Logger LOG = LogManager.getLogger(TimerLabel.class);

	private static final Dimension MIN_SIZE = new Dimension(0, 150);

	private static final BufferedImage RED_STATUS_IMAGE = loadImage("red-button.png");
	private static final BufferedImage GREEN_STATUS_IMAGE = loadImage("green-button.png");
    private Color solvingForegroundColor;
	private final ComponentAdapter componentResizeListener;


	private static BufferedImage loadImage(String imageName) {
		try {
			//can't use TimerLabel.class because the class hasn't been loaded yet
			return ImageIO.read(CalCubeTimerGui.class.getResourceAsStream(imageName));
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private BufferedImage currentStatusImage;

	public boolean greenLight;
	private TimerState time;
	private Font font;

	private final KeyboardHandler keyHandler;
	@Inject
	private ScrambleHyperlinkArea scrambleHyperlinkArea;
	@Inject
	private SolvingProcess solvingProcess;
	private final Configuration configuration;

	@Inject
	public TimerLabel(KeyboardHandler keyHandler, Configuration configuration) {
		super("");
        this.keyHandler = keyHandler;
        LOG.debug("TimerLabel created");
		this.configuration = configuration;
        componentResizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (font != null) { //this is to avoid an exception before showing the component
                    String newTime = getText();
                    Insets border = getInsets();
                    Rectangle2D bounds = font.getStringBounds(newTime, new FontRenderContext(null, true, true));
                    double height = (getHeight() - border.top - border.bottom) / bounds.getHeight();
                    double width = (getWidth() - border.left - border.right) / (bounds.getWidth() + 10);
                    double ratio = Math.min(width, height);
                    TimerLabel.super.setFont(font.deriveFont(AffineTransform.getScaleInstance(ratio, ratio)));
                }
            }
        };
        addComponentListener(componentResizeListener);
		setFocusable(true);
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				refreshTimer();
			}

			@Override
			public void focusLost(FocusEvent e) {
				refreshTimer();
			}
		});
		addKeyListener(this.keyHandler.createKeyListener(configuration, this));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				requestFocusInWindow();
			}
		});
		setFocusTraversalKeysEnabled(false);
	}

	@Inject
	public void initializeDisplay() {
		updateHandsState(TimerState.ZERO);
		setTime(TimerState.ZERO);
	}

	public void updateHandsState(TimerState newTime) {
		if(newTime instanceof StackmatState) {
			StackmatState newState = (StackmatState) newTime;
			keyHandler.setHands(newState.leftHand(), newState.rightHand());
			setStackmatGreenLight(newState.isGreenLight());
		}
	}

	public void setTime(TimerState time) {
        setForeground(solvingForegroundColor);
		this.time = time;
		super.setText(time.toString(configuration));
		componentResizeListener.componentResized(null);
	}

	public void configurationChanged() {
        solvingForegroundColor = configuration.getColor(VariableKey.TIMER_FG, false);
        setForeground(solvingForegroundColor);
		setBackground(configuration.getColorNullIfInvalid(VariableKey.TIMER_BG, false));

		setFont(configuration.getFont(VariableKey.TIMER_FONT, false));
		if(time != null) {//this will deal with any internationalization issues, if appropriate
			setTime(time);
		}
		else {
			setInspectionText(getText());
		}
		refreshTimer();
	}

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

	public void setStackmatGreenLight(boolean greenLight) {
		this.greenLight = greenLight;
	}

	public void reset() {
        keyHandler.setStackmatOn(false);
        greenLight = false;
		keyHandler.leftHand = false;
        keyHandler.rightHand = false;
		solvingProcess.resetProcess();
		refreshTimer();
	}

	public void setInspectionText(String s) {
		setForeground(Color.RED);
		time = null;
		super.setText(s);
        if (componentResizeListener != null) {
            componentResizeListener.componentResized(null);
        }
	}

	@Override
	public void setFont(Font font) {
		this.font = font;
		super.setFont(font);
	}

	@Override
	public void paintComponent(Graphics g) {
		if(configuration.getBoolean(VariableKey.LESS_ANNOYING_DISPLAY)) {
			g.drawImage(currentStatusImage, 10, 20, null);
		}
		g.drawImage(getImageForHand(keyHandler.leftHand), 10, getHeight() - 50, null);
		g.drawImage(getImageForHand(keyHandler.rightHand), getWidth() - 50, getHeight() - 50, null);
		super.paintComponent(g);
	}

	//see StackmatState for an explanation
	private BufferedImage getImageForHand(Boolean hand) {
		if(!keyHandler.isStackmatOn()) {
			return null;
		}
		if(hand == null) {
			return GREEN_STATUS_IMAGE;
		}
		return hand ? RED_STATUS_IMAGE : null;
	}

	private boolean canStartTimer() {
		boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION);
		boolean spacebarOnly = configuration.getBoolean(VariableKey.SPACEBAR_ONLY);
		return solvingProcess.canStartProcess()
                //checking if we are in a position to start the timer
				&& !solvingProcess.isRunning()
                //checking if the right keys are down to start the timer
				&& keyHandler.toggleStartKeysPressed(stackmatEmulation, spacebarOnly);
	}

	void refreshTimer() {
		boolean inspectionEnabled = configuration.getBoolean(VariableKey.COMPETITION_INSPECTION);

		String title;
        Color borderColor;
		boolean lowered = false;
        if (keyHandler.stackmatEnabled()) {
            title = StringAccessor.getString("TimerLabel.keyboardoff");
            if (keyHandler.isStackmatOn()) {
                currentStatusImage = GREEN_STATUS_IMAGE;
                if(greenLight) {
                    lowered = true;
                    borderColor = Color.GREEN;
                } else {
                    borderColor = Color.RED;
                }
            } else {
                currentStatusImage = RED_STATUS_IMAGE;
                borderColor = Color.GRAY;
            }
        } else {
            boolean focused = isFocusOwner();
            scrambleHyperlinkArea.setTimerFocused(focused);
            if(focused) {
                currentStatusImage = GREEN_STATUS_IMAGE;
                if(keyHandler.keysDown) {
                    lowered = true;
                }

                borderColor = keyHandler.keysDown && canStartTimer() ? Color.GREEN : Color.RED;

                if(solvingProcess.isRunning()) {
                    title = StringAccessor.getString("TimerLabel.stoptimer");
                }
                else if(solvingProcess.isInspecting() || !inspectionEnabled) {
                    title = StringAccessor.getString("TimerLabel.starttimer");
                }
                else {
                    title = StringAccessor.getString("TimerLabel.startinspection");
                }
            } else {
                currentStatusImage = RED_STATUS_IMAGE;
                title = StringAccessor.getString("TimerLabel.clickme");
                borderColor = Color.GRAY;
                keyHandler.releaseAllKeys();
            }
        }
        Border b = BorderFactory.createBevelBorder(lowered ? BevelBorder.LOWERED : BevelBorder.RAISED, borderColor, borderColor.darker().darker());
		setBorder(BorderFactory.createTitledBorder(b, title, TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, null, Utils.invertColor(getBackground())));
		repaint();
	}

	@Override
	public String toString() {
		return "TimerLabel{" +
				"time=" + time +
				'}';
	}

    public ComponentListener getComponentResizeListener() {
        return componentResizeListener;
    }
}
