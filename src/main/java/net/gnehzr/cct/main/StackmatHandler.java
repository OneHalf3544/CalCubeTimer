package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.stackmatInterpreter.StackmatInterpreter;
import net.gnehzr.cct.stackmatInterpreter.StackmatState;

import java.beans.PropertyChangeEvent;
import java.time.Instant;

@Singleton
public class StackmatHandler {

	private final SolvingProcessListener solvingProcessListener;
	private final TimingListener timingListener;

	private final Configuration configuration;
    private final SolvingProcess solvingProcess;

    @Inject
	public StackmatHandler(TimingListener timingListener, StackmatInterpreter stackmatInterpreter,
                           SolvingProcessListener solvingProcessListener,
                           Configuration configuration, SolvingProcess solvingProcess) {
		this.timingListener = timingListener;
        this.solvingProcessListener = solvingProcessListener;
        this.configuration = configuration;
        this.solvingProcess = solvingProcess;
        stackmatInterpreter.addPropertyChangeListener(this::stackmatStateChanged);
		reset();
	}

	public void reset() {
		leftHandStart = null;
		rightHandStart = null;
		stackmatInspecting = false;
	}

	private Instant leftHandStart;
	private Instant rightHandStart;

	private boolean stackmatInspecting;

	private void stackmatStateChanged(PropertyChangeEvent evt) {
		String event = evt.getPropertyName();
		boolean stackmatEnabled = configuration.isPropertiesLoaded() && configuration.getBoolean(VariableKey.STACKMAT_ENABLED);
		timingListener.stackmatChanged();
		if(!stackmatEnabled)
			return;

		if(evt.getNewValue() instanceof StackmatState) {
			StackmatState current = (StackmatState) evt.getNewValue();
			if (event.equals("Reset")) {
				if (current.oneHand()) {
					processOneHandState(current);
					return;
				}
				if (current.noHands()) {
					if(!stackmatInspecting
                            && (itsTimeToStartAfterInspection(leftHandStart)
                            || itsTimeToStartAfterInspection(rightHandStart))) {
                        stackmatInspecting = true;
                        solvingProcessListener.inspectionStarted();
                    }
				}
				timingListener.refreshDisplay(current);
			} else {
				timingListener.refreshDisplay(current);
				stackmatInspecting = false;
				switch (event) {
					case "TimeChange":
						solvingProcessListener.timerStarted();
						break;
					case "Split":
						solvingProcessListener.timerSplit(current);
						break;
					case "New Time":
                        solvingProcessListener.timerStopped(current);
						break;
					case "Current Display":
						break;
					case "Accident Reset":
                        solvingProcessListener.timerAccidentlyReset((StackmatState) evt.getOldValue());
						break;
				}
			}
			leftHandStart = null;
			rightHandStart = null;
		}
	}

	private void processOneHandState(StackmatState current) {
		if(stackmatInspecting) {
            timingListener.refreshDisplay(current);
            return;
        }
		if(current.leftHand()) {
            rightHandStart = null;
            if(leftHandStart == null) {
				leftHandStart = Instant.now();
			}
            else if(itsTimeToStartAfterInspection(leftHandStart)) {
				current.clearLeftHand();
			}
        } else { //the right hand is down
            leftHandStart = null;
            if (rightHandStart == null) {
				rightHandStart = Instant.now();
			}
            else if(itsTimeToStartAfterInspection(rightHandStart)) {
				current.clearRightHand();
			}
        }
		timingListener.refreshDisplay(current);
	}

	private boolean itsTimeToStartAfterInspection(Instant startTime) {
		if(startTime == null || !configuration.getBoolean(VariableKey.COMPETITION_INSPECTION)) {
			return false;
		}
		Instant endInspectionTime = startTime.plus(configuration.getDuration(VariableKey.DELAY_UNTIL_INSPECTION, false));
		return !endInspectionTime.isAfter(Instant.now());
	}
}
