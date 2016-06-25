package net.gnehzr.cct.keyboardTiming;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.main.SolvingProcess;
import net.gnehzr.cct.main.TimerLabelsHolder;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.Map;

@Service
public class KeyboardHandler {

    private final Configuration configuration;

    boolean keysDown;

    private boolean stackmatIsOn;
    Boolean leftHand;
    Boolean rightHand;

    //What follows is some really nasty code to deal with linux and window's differing behavior for keyrepeats
    private final Map<Integer, Long> timeup = new Hashtable<>(KeyEvent.KEY_LAST);

    private final Map<Integer, Boolean> keyDown = new Hashtable<>(KeyEvent.KEY_LAST);

    @Autowired
    private SolvingProcess solvingProcess;

    @Autowired
    private TimerLabelsHolder timerLabelsHolder;

    @Autowired
	public KeyboardHandler(Configuration configuration) {
		this.configuration = configuration;
	}

	public void split() {
		solvingProcess.timeSplit();
	}

    KeyAdapter createKeyListener(final Configuration configuration, TimerLabel timerLabel) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(configuration.getBoolean(VariableKey.STACKMAT_ENABLED)) {
					return;
				}
				int code = e.getKeyCode();
				if (e.getWhen() - getTime(code) < 10) {
					timeup.put(code, (long) 0);
				} else if(!isKeyDown(code)){
					keyDown.put(code, true);
					keyReallyPressed(e);
				}
				timerLabel.refreshTimer();
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if(configuration.getBoolean(VariableKey.STACKMAT_ENABLED)) {
					return;
				}
				int keyCode = e.getKeyCode();
				timeup.put(keyCode, e.getWhen());

				new Timer(10, evt -> {
					if(isKeyDown(keyCode) && getTime(keyCode) != 0) {
						keyDown.put(keyCode, false);
						keyReallyReleased(e);
					}
					((Timer) evt.getSource()).stop();
				}).start();
			}
		};
	}

    boolean toggleStartKeysPressed(boolean stackmatEmulation, boolean spacebarOnly) {
        return stackmatEmulation && stackmatKeysDown() && atMostKeysDown(2) || //checking if the right keys are down for starting a "stackmat"
                !stackmatEmulation && atMostKeysDown(1) && (!spacebarOnly || isKeyDown(KeyEvent.VK_SPACE));
    }

    private boolean isKeyDown(int keycode) {
        Boolean temp = keyDown.get(keycode);
        return (temp == null) ? false : temp;
    }

    private boolean atMostKeysDown(int count){
        for (Boolean down : keyDown.values()) {
            if (down && --count < 0) {
                return false;
            }
        }
        return true;
    }

    private int stackmatKeysDownCount(){
        return (isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1)) ? 1 : 0) +
                (isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2)) ? 1 : 0);
    }

    private boolean stackmatKeysDown(){
        return isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1)) &&
                isKeyDown(configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2));
    }

    //called when a key is physically pressed
    private void keyReallyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key != 0) {
            boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION);
            //ignore unrecognized keys, such as media buttons
            if(solvingProcess.isSolving()) {
                if(configuration.getBoolean(VariableKey.TIMING_SPLITS)
                        && key == configuration.getInt(VariableKey.SPLIT_KEY)) {
                    split();
                } else if(!stackmatEmulation || stackmatKeysDown()){
                    solvingProcess.solvingFinished();
                    keysDown = true;
                }
            } else if(key == KeyEvent.VK_ESCAPE) {
                releaseAllKeys();
            } else if(!stackmatEmulation) {
                keysDown = true;
            }
        }
    }

    //this will release all keys that we think are down
    void releaseAllKeys() {
        keyDown.clear();
        timeup.clear();
        keysDown = false;
    }

    //called when a key is physically released
    private void keyReallyReleased(KeyEvent e) {
        boolean stackmatEmulation = configuration.getBoolean(VariableKey.STACKMAT_EMULATION);
        int sekey1 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY1);
        int sekey2 = configuration.getInt(VariableKey.STACKMAT_EMULATION_KEY2);

        if (stackmatEmulation && stackmatKeysDownCount() == 1
                && (e.getKeyCode() == sekey1 || e.getKeyCode() == sekey2)
                || !stackmatEmulation && atMostKeysDown(0)){
            keysDown = false;
            if (solvingProcess.isInspecting()) {
                solvingProcess.startSolving();
            }
            else if(solvingProcess.isRunning()) {
                solvingProcess.solvingFinished();
            }
            else if(!ignoreKey(e, configuration.getBoolean(VariableKey.SPACEBAR_ONLY), stackmatEmulation, sekey1, sekey2)) {
                solvingProcess.startProcess();
            }
        }
    }

    void setHands(Boolean leftHand, Boolean rightHand) {
        this.leftHand = leftHand;
        this.rightHand = rightHand;
    }

    public static boolean ignoreKey(KeyEvent e, boolean spaceBarOnly, boolean stackmatEmulation, int sekey1, int sekey2) {
        int key = e.getKeyCode();
        if(stackmatEmulation){
            return key != sekey1 && key != sekey2;
        }
        if(spaceBarOnly) {
            return key != KeyEvent.VK_SPACE;
        }
        return key != KeyEvent.VK_ENTER && (key > 123 || key < 23 || e.isAltDown() || e.isControlDown() || key == KeyEvent.VK_ESCAPE);
    }

    public void setStackmatOn(boolean on) {
        this.stackmatIsOn = on;
        if(!stackmatIsOn) {
            setHands(false, false);
            timerLabelsHolder.changeGreenLight(false);
        }
        timerLabelsHolder.refreshTimer("0.00");
    }

    boolean stackmatEnabled() {
        return configuration.getBoolean(VariableKey.STACKMAT_ENABLED);
    }

    long getTime(int keycode) {
        Long temp = timeup.get(keycode);
        return (temp == null) ? 0 : temp;
    }

    public boolean isStackmatOn() {
        return stackmatIsOn;
    }


}
