package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@Singleton
public class StackmatHandler implements PropertyChangeListener {
	private TimingListener tl;
	private final Configuration configuration;

	@Inject
	public StackmatHandler(TimingListener timingListener, StackmatInterpreter si, Configuration configuration) {
		this.tl = timingListener;
		this.configuration = configuration;
		si.addPropertyChangeListener(this);
		reset();
	}
	
	public void reset() {
		leftStart = rightStart = 0;
		stackmatInspecting = false;
	}

	private long leftStart, rightStart;
	private boolean stackmatInspecting;
	public void propertyChange(PropertyChangeEvent evt) {
		String event = evt.getPropertyName();
		boolean stackmatEnabled = configuration.getBoolean(VariableKey.STACKMAT_ENABLED, false);
		tl.stackmatChanged();
		if(!stackmatEnabled)
			return;

		if(evt.getNewValue() instanceof StackmatState) {
			StackmatState current = (StackmatState) evt.getNewValue();
			if(event.equals("Reset")) { 
				if (current.oneHand()) {
					processOneHandState(current);
					return;
				}
				if (!current.bothHands()) {
					if(!stackmatInspecting && (timeToStart(leftStart) || timeToStart(rightStart))) {
                        stackmatInspecting = true;
                        tl.inspectionStarted();
                    }
				}
				tl.refreshDisplay(current);
			} else {
				tl.refreshDisplay(current);
				stackmatInspecting = false;
				switch (event) {
					case "TimeChange":
						tl.timerStarted();
						break;
					case "Split":
						tl.timerSplit(current);
						break;
					case "New Time":
						tl.timerStopped(current);
						break;
					case "Current Display":
						break;
					case "Accident Reset":
						tl.timerAccidentlyReset((StackmatState) evt.getOldValue());
						break;
				}
			}
			leftStart = current.leftHand() ? -1 : 0;
			rightStart = current.rightHand() ? -1 : 0;
		}
	}

	private void processOneHandState(StackmatState current) {
		if(stackmatInspecting) {
            tl.refreshDisplay(current);
            return;
        }
		if(current.leftHand()) {
            rightStart = 0;
            if(leftStart <= 0)
                leftStart = System.currentTimeMillis();
            else if(timeToStart(leftStart))
                current.clearLeftHand();
        } else { //the right hand is down
            leftStart = 0;
            if(rightStart <= 0)
                rightStart = System.currentTimeMillis();
            else if(timeToStart(rightStart))
                current.clearRightHand();
        }
		tl.refreshDisplay(current);
		return;
	}

	private boolean timeToStart(long time) {
		if(time <= 0 || !configuration.getBoolean(VariableKey.COMPETITION_INSPECTION, false))
			return false;
		return (System.currentTimeMillis() - time >= configuration.getInt(VariableKey.DELAY_UNTIL_INSPECTION, false));
	}
}
