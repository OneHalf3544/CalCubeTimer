package net.gnehzr.cct.scrambles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Deprecated // todo get list from solutions in the current session
@Singleton
public class ScrambleList {

	private final ScramblePluginManager scramblePluginManager;
	private LinkedList<ScrambleString> scrambles = new LinkedList<>();
	private PuzzleType currentScrambleCustomisation;
	private int scrambleNumber = 0;

	@Inject
	public ScrambleList(ScramblePluginManager scramblePluginManager) {
		this.scramblePluginManager = scramblePluginManager;
		this.currentScrambleCustomisation = scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
	}

	@NotNull
	public PuzzleType getCurrentScrambleCustomization() {
		return currentScrambleCustomisation;
	}

	//this should only be called if we're on the last scramble in this list
	public void setCurrentScrambleCustomization(PuzzleType puzzleType) {
		checkArgument(isLastScrambleInList());

		if(puzzleType == null) {
			puzzleType = scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
		}
		if(currentScrambleCustomisation == null || !puzzleType.getScrambleVariation().equals(currentScrambleCustomisation.getScrambleVariation())) {
			removeLatestAndFutureScrambles();
		}
		if(!puzzleType.equals(scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION)) {
			currentScrambleCustomisation = puzzleType;
		}
	}

	public void removeLatestAndFutureScrambles() {
		int removeNumber = scrambles.size() - scrambleNumber;
		for(int c = 0; c < removeNumber; c++) {
			scrambles.removeLast();
		}
	}
	
	public void setScrambleLength(int length) {
		ScrambleVariation scrambleVariation = currentScrambleCustomisation.getScrambleVariation();
		if(length != scrambleVariation.getLength()) {
			scrambleVariation.setLength(length);
			removeLatestAndFutureScrambles();
		}
	}

	public void updateGeneratorGroup(String generatorGroup) {
		currentScrambleCustomisation.setGenerator(generatorGroup);
		scramblePluginManager.saveGeneratorToConfiguration(currentScrambleCustomisation);

		ScrambleString scramble = scrambles.getLast();
		removeLatestAndFutureScrambles();
		if (scramble != null) {
			//TODO - hack to make changing the generator group not change the scramble
			scrambles.add(scramble);
		}
	}

	public void clear() {
		scrambleNumber = 0;
		scrambles.clear();
	}
	
	public int size() {
		return scrambles.size();
	}
	
	public void addScramble(ScrambleString scramble) {
		scrambles.add(scramble);
	}
	
	public ScrambleString getCurrent() {
		if(isLastScrambleInList()) {
			scrambles.add(scrambleNumber, currentScrambleCustomisation.generateScramble());
		}
		return scrambles.get(scrambleNumber);
	}

	private boolean isLastScrambleInList() {
		return scrambleNumber == scrambles.size();
	}

	public void importScrambles(List<ScrambleString> scrambles) {
		removeLatestAndFutureScrambles();
		this.scrambles.addAll(scrambles);
	}

	public ScrambleString getNext() {
		scrambleNumber++;
		return getCurrent();
	}

	public int getScrambleNumber() {
		return scrambleNumber + 1;
	}

	public void setScrambleNumber(int scrambleNumber) {
		this.scrambleNumber = scrambleNumber - 1;
	}
}
