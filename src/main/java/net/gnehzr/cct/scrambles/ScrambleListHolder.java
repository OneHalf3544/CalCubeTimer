package net.gnehzr.cct.scrambles;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * Created by OneHalf on 21.06.2016.
 */
@Service
public class ScrambleListHolder implements ScrambleList {

    private ScrambleList scrambleList;
    private ScrambleChangeListener scrambleChangeListener;

    public void setScrambleList(ScrambleList scrambleList) {
        this.scrambleList = scrambleList;
    }

    @NotNull
    @Override
    public PuzzleType getPuzzleType() {
        return scrambleList.getPuzzleType();
    }

    @Override
    public int scramblesCount() {
        return scrambleList.scramblesCount();
    }

    @NotNull
    @Override
    public ScrambleString getCurrentScramble() {
        return scrambleList.getCurrentScramble();
    }

    @Override
    public ScrambleString generateNext() {
        ScrambleString scrambleString = scrambleList.generateNext();
        scrambleChangeListener.scrambleUpdated();
        return scrambleString;
    }

    @Override
    public boolean isGenerating() {
        return scrambleList.isGenerating();
    }

    @Override
    public boolean isImported() {
        return scrambleList.isImported();
    }

    @Override
    public GeneratedScrambleList asGenerating() {
        return scrambleList.asGenerating();
    }

    @Override
    public ImportedScrambleList asImported() {
        return scrambleList.asImported();
    }

    @Override
    public int getScrambleNumber() {
        return scrambleList.getScrambleNumber();
    }

    public void addListener(ScrambleChangeListener scrambleChangeListener) {
        this.scrambleChangeListener = scrambleChangeListener;
    }

    public interface ScrambleChangeListener {
        void scrambleUpdated();
    }
}
