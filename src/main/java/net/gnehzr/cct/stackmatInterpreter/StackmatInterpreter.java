package net.gnehzr.cct.stackmatInterpreter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class StackmatInterpreter extends SwingWorker<Void, StackmatState> {

    private static final Logger LOG = LogManager.getLogger(StackmatInterpreter.class);

	private static final int BYTES_PER_SAMPLE = 2;
	private static final int FRAMES = 64;

    private int samplingRate = 0;
    private int noiseSpikeThreshold;
    private int newPeriod;
    private int switchThreshold;
    private double signalLengthPerBit;

    private AudioFormat format;
    public DataLine.Info info;

    private TargetDataLine line;

    private StackmatState state = null;

    private boolean enabled = true;

    private static Mixer.Info[] aInfos = AudioSystem.getMixerInfo();

    @Inject
	public StackmatInterpreter(Configuration configuration) {
		/*int samplingRate = configuration.getInt(VariableKey.STACKMAT_SAMPLING_RATE);
        int mixerNumber = configuration.getInt(VariableKey.MIXER_NUMBER);
        boolean stackmat = configuration.getBoolean(VariableKey.STACKMAT_ENABLED);
        int switchThreshold = configuration.getInt(VariableKey.SWITCH_THRESHOLD);
		initialize(samplingRate, mixerNumber, stackmat, switchThreshold);*/
	}

    public void initialize(int samplingRate, int mixerNum, boolean enabled, int switchThreshold) {
		if(this.samplingRate == samplingRate && getSelectedMixerIndex() == mixerNum) return;
        this.samplingRate = samplingRate;
        this.enabled = enabled;
        this.switchThreshold = switchThreshold;
        this.noiseSpikeThreshold = samplingRate * 25 / 44100;
        this.newPeriod = samplingRate / 44;
        this.signalLengthPerBit = samplingRate * 38 / 44100.;

        format = new AudioFormat(samplingRate, BYTES_PER_SAMPLE * 8, 2, true, false);
        info = new DataLine.Info(TargetDataLine.class, format);

        if (mixerNum >= 0) {
            changeLine(mixerNum);
        }
        else {
            try {
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                synchronized (this) {
                    notify();
                }
            } catch (IllegalArgumentException e) {
                //This is thrown when there is no configuration file
                LOG.info("unexpected exception", e);
            } catch (LineUnavailableException e) {
                LOG.info("unexpected exception", e);
                cleanup();
            }
        }
    }

    private void cleanup() {
        if (line != null) {
            line.stop();
            line.close();
        }
        line = null;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public int getSelectedMixerIndex() {
        if (line == null) {
            return aInfos.length;
        }
        for (int i = 0; i < aInfos.length; i++) {
            Mixer mixer = AudioSystem.getMixer(aInfos[i]);
            Line[] openLines = mixer.getTargetLines();
            for (Line openLine : openLines) {
                if (line == openLine) {
                    return i;
                }
            }
        }
        return aInfos.length;
    }

    private void changeLine(int mixerNum) {
        if (mixerNum < 0 || mixerNum >= aInfos.length) {
            if (line != null) {
                cleanup();
            }
            return;
        }

        try {
            Mixer mixer = AudioSystem.getMixer(aInfos[mixerNum]);
            if (mixer.isLineSupported(info)) {
                if (line != null) {
                    cleanup();
                }
                line = (TargetDataLine) mixer.getLine(info);
                line.open(format);
                line.start();
            }
        } catch (LineUnavailableException e) {
            cleanup();
        }

		synchronized(this){
			notify();
	    }
	}

	public void enableStackmat(boolean enable){
		if(!enabled && enable){
			enabled = true;
            synchronized(this){
				notify();
            }
        }
		else {
            enabled = enable;
        }
    }

	public String[] getMixerChoices(String mixer, String desc, String nomixer) {
		String[] items = new String[aInfos.length+1];
		for(int i = 0; i < aInfos.length; i++)
			items[i] = mixer + i + ": " + aInfos[i].getName() + desc + aInfos[i].getDescription();
		items[items.length-1] = nomixer;
		return items;
					}

    public boolean isMixerEnabled(int index) {
        return index >= aInfos.length || AudioSystem.getMixer(aInfos[index]).isLineSupported(info);
    }

	//otherwise returns true if the stackmat is on
	public Boolean isOn() {
		return on;
    }

    private boolean on;

    @Override
    public Void doInBackground() {
        int timeSinceLastFlip = 0;
        int lastSample = 0;
        int currentSample;
        int lastBit = 0;
        byte[] buffer = new byte[BYTES_PER_SAMPLE * FRAMES];

        List<Integer> currentPeriod = new ArrayList<>(100);
        StackmatValue stackmatValue = new StackmatValue(
                timeSinceLastFlip, lastSample, lastBit, buffer, currentPeriod,
                new StackmatState(null, currentPeriod), /*previousWasSplit*/false);

        while (!isCancelled()) {
            if (!enabled || line == null) {
                on = false;
                firePropertyChange("Off", null, null);
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignore) { }
                continue;
            }

            updateStackmatValue(stackmatValue, stackmatValue.buffer);
        }
        return null;
    }

    public void updateStackmatValue(StackmatValue stackmatValue, byte[] buffer) {
        if (line.read(buffer, 0, buffer.length) <= 0) {
            return;
        }

        int currentSample;
        for (int c = 0; c < buffer.length / BYTES_PER_SAMPLE; c += 2) {
            //we increment by 2 to mask out 1 channel
            //little-endian encoding, bytes are in increasing order
            currentSample = 0;
            int j;
            for (j = 0; j < BYTES_PER_SAMPLE - 1; j++) {
                currentSample |= (255 & buffer[BYTES_PER_SAMPLE * c + j]) << (j * 8);
            }
            currentSample |= buffer[BYTES_PER_SAMPLE * c + j] << (j * 8); //we don't mask with 255 so we don't lost the sign
            if (stackmatValue.timeSinceLastFlip < newPeriod * 4) {
                stackmatValue.timeSinceLastFlip++;
            }
            else if (stackmatValue.timeSinceLastFlip == newPeriod * 4) {
                state = new StackmatState(state, Collections.<Integer>emptyList());
                stackmatValue.timeSinceLastFlip++;
                on = false;
                firePropertyChange("Off", null, null);
            }

            if (Math.abs(stackmatValue.lastSample - currentSample) > switchThreshold << (BYTES_PER_SAMPLE * 4) && stackmatValue.timeSinceLastFlip > noiseSpikeThreshold) {
                if (stackmatValue.timeSinceLastFlip > newPeriod) {
                    if (stackmatValue.currentPeriod.size() < 1) {
                        stackmatValue.lastBit = stackmatValue.bitValue(currentSample - stackmatValue.lastSample);
                        stackmatValue.timeSinceLastFlip = 0;
                        continue;
                    }

                    StackmatState newState = new StackmatState(state, stackmatValue.currentPeriod);
                    if (state != null && state.isRunning() && newState.isReset()) { //this is indicative of an "accidental reset"
                        firePropertyChange("Accident Reset", state, newState);
                    }
                    StackmatState oldState = this.state;
                    state = newState;
                    //This is to be able to identify new times when they are "equal" to the last time
                    if(state.isReset() || state.isRunning()) stackmatValue.old = oldState;

                    boolean thisIsSplit = state.isRunning() && state.oneHand();
                    if (thisIsSplit && !stackmatValue.previousWasSplit) {
                        firePropertyChange("Split", null, state);
                    }
                    stackmatValue.previousWasSplit = thisIsSplit;
                    if (state.isReset())
                        firePropertyChange("Reset", null, state);
                    else if (state.isRunning())
                        firePropertyChange("TimeChange", null, state);
                    else if (state.compareTo(stackmatValue.old) != 0) {
                        stackmatValue.old = state;
                        firePropertyChange("New Time", null, state);
                    } else { //So we can always get the current time
                        firePropertyChange("Current Display", null, state);
                    }
                    on = true;
                    stackmatValue.currentPeriod = new ArrayList<>(100);
                } else {
                    for (int i = 0; i < Math.round(stackmatValue.timeSinceLastFlip / signalLengthPerBit); i++) {
                        stackmatValue.currentPeriod.add(stackmatValue.lastBit);
                    }
                }
                stackmatValue.lastBit = stackmatValue.bitValue(currentSample - stackmatValue.lastSample);
                stackmatValue.timeSinceLastFlip = 0;
            }
            stackmatValue.lastSample = currentSample;
        }
    }

    public int getStackmatValue() {
        return switchThreshold;
    }

    private static class StackmatValue {

        private int timeSinceLastFlip;
        private int lastSample;
        private int lastBit;
        private byte[] buffer;
        private List<Integer> currentPeriod;
        private StackmatState old;
        private boolean previousWasSplit;

        public StackmatValue(int timeSinceLastFlip, int lastSample, int lastBit, byte[] buffer, List<Integer> currentPeriod, StackmatState old, boolean previousWasSplit) {
            this.timeSinceLastFlip = timeSinceLastFlip;
            this.lastSample = lastSample;
            this.lastBit = lastBit;
            this.buffer = buffer;
            this.currentPeriod = currentPeriod;
            this.old = old;
            this.previousWasSplit = previousWasSplit;
        }

        public void setTimeSinceLastFlip(int timeSinceLastFlip) {
            this.timeSinceLastFlip = timeSinceLastFlip;
        }

        public void setLastSample(int lastSample) {
            this.lastSample = lastSample;
        }

        public void setLastBit(int lastBit) {
            this.lastBit = lastBit;
        }

        public void setBuffer(byte[] buffer) {
            this.buffer = buffer;
        }

        public void setCurrentPeriod(List<Integer> currentPeriod) {
            this.currentPeriod = currentPeriod;
        }

        public void setOld(StackmatState old) {
            this.old = old;
        }

        public void setPreviousWasSplit(boolean previousWasSplit) {
            this.previousWasSplit = previousWasSplit;
        }

        private int bitValue(int x) {
            return (x > 0) ? 1 : 0;
        }

        public int getTimeSinceLastFlip() {
            return timeSinceLastFlip;
        }

        public int getLastSample() {
            return lastSample;
        }

        public int getLastBit() {
            return lastBit;
        }

        public List<Integer> getCurrentPeriod() {
            return currentPeriod;
        }

        public StackmatState getOld() {
            return old;
        }

        public boolean isPreviousWasSplit() {
            return previousWasSplit;
        }

    }
}
