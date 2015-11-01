package net.gnehzr.cct.stackmatInterpreter;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.statistics.SolveTime;

import java.time.Duration;
import java.util.List;

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

	public StackmatState(StackmatState previous, List<Integer> periodData) {
		super(getCurrentTime(periodData, previous));

		if (periodData.size() == 89) { //all data present
			isValid = true;
			parseHeader(periodData);
			reset = getTime().isZero();
			running = previous == null || this.compareTo(previous) > 0 && !reset;
		} else {
			if (previous != null) { //if corrupt and previous not null, make time equal to previous
				this.rightHand = previous.rightHand;
				this.leftHand = previous.leftHand;
				this.running = previous.running;
				this.reset = previous.reset;
				this.isValid = previous.isValid;
				this.greenLight = previous.greenLight;
			}
		}
	}

	private static Duration getCurrentTime(List<Integer> periodData, StackmatState previous) {
		if (periodData.size() == 89) { //all data present
			return parseTime(periodData);

		} else {
		 	//if corrupt and previous not null, make time equal to previous
			return previous != null ? previous.getTime() : Duration.ZERO;
		}
	}

	private static Duration parseTime(List<Integer> periodData){
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

	private static int parseDigit(List<Integer> periodData, int position, boolean invert){
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

	public boolean noHands() {
		return !bothHands();
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

	@Override
	public boolean isInspecting() {
		return false;
	}

	@Override
	public String toString(Configuration configuration) {
		return Utils.formatTime(new SolveTime(getTime()), configuration.useClockFormat());
	}

	@Override
	public String toString() {
		return Utils.formatTime(new SolveTime(getTime()), true);
	}
}
