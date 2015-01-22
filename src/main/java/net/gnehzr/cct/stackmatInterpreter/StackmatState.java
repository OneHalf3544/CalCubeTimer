package net.gnehzr.cct.stackmatInterpreter;

import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;

import com.google.inject.Inject;
import net.gnehzr.cct.configuration.Configuration;

import java.time.Duration;
import java.util.List;

@Singleton
public class StackmatState extends TimerState {

	private Boolean rightHand = false;
	private Boolean leftHand = false;
	private boolean running = false;
	private boolean reset = true;
	private boolean isValid = false;
	private boolean greenLight = false;

	private static boolean invertedMin;
	private static boolean invertedSec;
	private static boolean invertedHun;

	public static void setInverted(boolean minutes, boolean seconds, boolean hundredths) {
		invertedMin = minutes;
		invertedSec = seconds;
		invertedHun = hundredths;
	}

	@Inject
	public StackmatState(Configuration configuration) {
		super(configuration);
	}

	public StackmatState(StackmatState previous, List<Integer> periodData, Configuration configuration) {
		this(configuration);
		if (periodData.size() == 89) { //all data present
			isValid = true;
			Duration value = parseTime(periodData);
			setTime(value);
			running = previous == null || this.compareTo(previous) > 0 && !value.isZero();
			reset = value.isZero();
		} else if (previous != null) { //if corrupt and previous not null, make time equal to previous
			this.rightHand = previous.rightHand;
			this.leftHand = previous.leftHand;
			this.running = previous.running;
			this.reset = previous.reset;
			this.isValid = previous.isValid;
			this.greenLight = previous.greenLight;
			setTime(previous.getTime());
		}
	}

	private Duration parseTime(List<Integer> periodData){
		parseHeader(periodData);
		return Duration
                .ofMinutes(parseDigit(periodData, 1, invertedMin))
                .plus(Duration.ofSeconds(parseDigit(periodData, 2, invertedSec) * 10 + parseDigit(periodData, 3, invertedSec)))
                .plus(Duration.ofMillis((parseDigit(periodData, 4, invertedHun) * 10 + parseDigit(periodData, 5, invertedHun)) * 10));
	}

	private void parseHeader(List<Integer> periodData){
		int temp = 0;
		for(int i = 1; i <= 5; i++) {
			temp += periodData.get(i) << (5 - i);
		}

		leftHand = (temp == 6);
		rightHand = (temp == 9);

		if(temp == 24 || temp == 16) {
			leftHand = true;
			rightHand = true;
		}
		greenLight = temp == 16;
	}

	private int parseDigit(List<Integer> periodData, int position, boolean invert){
		int temp = 0;
		for(int i = 1; i <= 4; i++) {
			temp += periodData.get(position * 10 + i) << (i - 1);
		}
		return invert ? 15 - temp : temp;
	}
	public boolean oneHand() {
		return rightHand ^ leftHand;
	}
	public boolean bothHands() {
		return rightHand && leftHand;
	}
	//Added just for completeness
	public boolean isRedLight() {
		return bothHands();
	}
	public boolean isValid() {
		return isValid;
	}
	public boolean isRunning() {
		return running;
	}
	public boolean isReset(){
		return reset;
	}
	//these are here so we can know if the left or right hand has been down for long enough to start inspection
	public void clearLeftHand() {
		leftHand = null;
	}
	public void clearRightHand() {
		rightHand = null;
	}
	public Boolean leftHand(){
		return leftHand;
	}
	public Boolean rightHand(){
		return rightHand;
	}
	public boolean isGreenLight(){
		return greenLight;
	}

	public String toString() {
		return Utils.formatTime(getTime().toMillis() / 1000.0, configuration.getBoolean(VariableKey.CLOCK_FORMAT, false));
	}
}
