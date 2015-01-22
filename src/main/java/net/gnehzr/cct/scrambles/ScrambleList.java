package net.gnehzr.cct.scrambles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ScrambleList {

	private final ScramblePluginManager scramblePluginManager;
	private List<ScrambleString> scrambles = new ArrayList<>();
	private ScrambleCustomization scrambleCustomisation;
	private int scrambleNumber = 0;

	@Inject
	public ScrambleList(ScramblePluginManager scramblePluginManager) {
		this.scramblePluginManager = scramblePluginManager;
		this.scrambleCustomisation = scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
	}

	@NotNull
	public ScrambleCustomization getScrambleCustomization() {
		return scrambleCustomisation;
	}
	//this should only be called if we're on the last scramble in this list
	public void setScrambleCustomization(ScrambleCustomization scrambleCustomization) {
		if(scrambleCustomization == null) {
			scrambleCustomization = scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
		}
		if(scrambleCustomisation == null || !scrambleCustomization.getScrambleVariation().equals(scrambleCustomisation.getScrambleVariation())) {
			removeLatestAndFutureScrambles();
		}
		if(!scrambleCustomization.equals(scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION)) {
			scrambleCustomisation = scrambleCustomization;
		}
	}
	
	public void removeLatestAndFutureScrambles() {
		//nullify the current scramble, and anything imported
		//setting to null is easier than modifying the length of the list, we'll generate the scrams in getCurrent() when we need 'em
		for(int c = scrambleNumber; c < scrambles.size(); c++)
			scrambles.set(c, null);
	}
	
	public void setScrambleLength(int l) {
		ScrambleVariation sv = scrambleCustomisation.getScrambleVariation();
		if(l != sv.getLength()) {
			sv.setLength(l);
			removeLatestAndFutureScrambles();
		}
	}

	public void updateGeneratorGroup(String group) {
		scrambleCustomisation.setGenerator(group);
		scrambleCustomisation.saveGeneratorToConfiguration();
		String scramble = null;
		if(scrambleNumber < scrambles.size() && scrambles.get(scrambleNumber) != null)
			scramble = scrambles.get(scrambleNumber).getScramble();
		removeLatestAndFutureScrambles();
		if(scramble != null) //TODO - hack to make changing the generator group not change the scramble
			scrambles.set(scrambleNumber, new ScrambleString(scramble, false, scramble.length()));
	}
	public void clear() {
		scrambleNumber = 0;
		scrambles.clear();
	}
	
	public int size() {
		return scrambles.size();
	}
	
	public void addScramble(String scramble) {
		scrambles.add(new ScrambleString(scramble, false, scramble.length()));
	}
	
	public ScrambleString getCurrent() {
		if(scrambleNumber == scrambles.size())
			scrambles.add(null); //ensure that there's capacity for the current scramble
		ScrambleString c = scrambles.get(scrambleNumber);
		if(c == null) {
			Scramble s = scrambleCustomisation.generateScramble();
			c = new ScrambleString(s.toString(), false, s.getLength());
			scrambles.set(scrambleNumber, c);
		}
		return c;
	}
	
	public void importScrambles(List<Scramble> scrams) {
		removeLatestAndFutureScrambles();
		for(int c = 0; c < scrams.size(); c++) {
			ScrambleString s = new ScrambleString(scrams.get(c).toString(), true, scrams.get(c).getLength());
			if(scrambleNumber + c < scrambles.size())
				scrambles.set(scrambleNumber + c, s);
			else
				scrambles.add(s);
		}
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
