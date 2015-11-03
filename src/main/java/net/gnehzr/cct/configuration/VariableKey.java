package net.gnehzr.cct.configuration;

import com.google.common.base.Throwables;
import net.gnehzr.cct.main.CalCubeTimerGui;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePlugin;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class VariableKey<H> {

	public static final VariableKey<Integer> STATS_DIALOG_FONT_SIZE = new VariableKey<>("GUI_StatsDialog_fontSize");
	public static final VariableKey<Duration> DELAY_UNTIL_INSPECTION = new VariableKey<>("GUI_Timer_delayUntilInspection");
	public static final VariableKey<Integer> DELAY_BETWEEN_SOLVES = new VariableKey<>("GUI_Timer_delayBetweenSolves");
	public static final VariableKey<Integer> SWITCH_THRESHOLD = new VariableKey<>("Stackmat_switchThreshold");
	public static final VariableKey<Integer> MIXER_NUMBER = new VariableKey<>("Stackmat_mixerNumber");
	public static final VariableKey<Integer> SPLIT_KEY = new VariableKey<>("Splits_splitKey");
	public static final VariableKey<Integer> STACKMAT_EMULATION_KEY1 = new VariableKey<>("GUI_Timer_stackmatEmulationKey1");
	public static final VariableKey<Integer> STACKMAT_EMULATION_KEY2 = new VariableKey<>("GUI_Timer_stackmatEmulationKey2");
	public static final VariableKey<Integer> STACKMAT_SAMPLING_RATE = new VariableKey<>("Stackmat_samplingRate");
	public static final VariableKey<Integer> POPUP_GAP = new VariableKey<>("Scramble_Popup_gap");
	public static final VariableKey<Integer> METRONOME_DELAY_MIN = new VariableKey<>("Misc_Metronome_delayMin");
	public static final VariableKey<Integer> METRONOME_DELAY_MAX = new VariableKey<>("Misc_Metronome_delayMax");
	public static final VariableKey<Integer> METRONOME_DELAY = new VariableKey<>("Misc_Metronome_delay");
	public static final VariableKey<Integer> MAX_FONTSIZE = new VariableKey<>("Scramble_fontMaxSize");
	public static final VariableKey<Integer> SCRAMBLE_COMBOBOX_ROWS = new VariableKey<>("Scramble_comboboxRows");
	public static final VariableKey<Integer> FULLSCREEN_DESKTOP = new VariableKey<>("Misc_fullscreenDesktop");
	public static VariableKey<Integer> UNIT_SIZE(PuzzleType variation) {
		return new VariableKey<>("Scramble_Popup_unitSize_" + variation.getVariationName());
	}
	public static VariableKey<Integer> scrambleLength(String variationName) {
		checkArgument(!Objects.equals(variationName, ScramblePluginManager.NULL_SCRAMBLE_VARIATION_NAME));
		return new VariableKey<>("Puzzle_ScrambleLength_" + variationName);
	}

	public static VariableKey<Integer> RA_SIZE(@NotNull RollingAverageOf index, @NotNull PuzzleType puzzleType) {
		return new VariableKey<>(defaultRaSizeKey(index) + "_" + puzzleType.getVariationName());
	}

	public static VariableKey<Integer> defaultRaSize(RollingAverageOf index) {
		return new VariableKey<>(defaultRaSizeKey(index));
	}

	@NotNull
	private static String defaultRaSizeKey(RollingAverageOf index) {
		return "Puzzle_RA" + index.getCode() + "Size";
	}

	public static VariableKey<Integer> JCOMPONENT_VALUE(String componentID, boolean xmlSpecific, File xmlguiLayout) {
		String key = "GUI_xmlLayout"; 
		if(xmlSpecific)
			key += "_" + xmlguiLayout.getName();
		key += "_component"+ componentID; 
		return new VariableKey<>(key);
	}

	public static VariableKey<Integer[]> JTABLE_COLUMN_ORDERING(String componentID) {
		return new VariableKey<>("GUI_xmlLayout_" + componentID + "_columns");
	}
	
	public static final VariableKey<String> LANGUAGE = new VariableKey<>("GUI_I18N_language");
	public static final VariableKey<String> REGION = new VariableKey<>("GUI_I18N_region");
	public static final VariableKey<String> VOICE = new VariableKey<>("Misc_Voices_person");
	public static final VariableKey<String> DATE_FORMAT = new VariableKey<>("Misc_dateFormat");

	public static final VariableKey<String> BEST_RA_STATISTICS = new VariableKey<>("Statistics_String_bestRA");
	public static final VariableKey<String> CURRENT_AVERAGE_STATISTICS = new VariableKey<>("Statistics_String_currentAverage");
	public static final VariableKey<String> SESSION_STATISTICS = new VariableKey<>("Statistics_String_session");
	public static final VariableKey<String> LAST_VIEWED_FOLDER = new VariableKey<>("Misc_lastViewedFolder");
	public static final VariableKey<String> DEFAULT_SCRAMBLE_URL = new VariableKey<>("Misc_defaultScrambleURL");
	public static final VariableKey<String> XML_LAYOUT = new VariableKey<>("GUI_xmlLayout_file");
	public static final VariableKey<String> DEFAULT_SCRAMBLE_CUSTOMIZATION = new VariableKey<>("Scramble_Default_scrambleCustomization");

	public static VariableKey<String> scrambleGeneratorKey(PuzzleType puzzleType) {
		return new VariableKey<>("Puzzle_ScrambleGenerator_" + puzzleType.toString());
	}

	public static final VariableKey<List<String>> SOLVE_TAGS = new VariableKey<>("Misc_solveTags");
	public static final VariableKey<List<String>> IMPORT_URLS = new VariableKey<>("Misc_scrambleURLs");
	public static VariableKey<List<String>> PUZZLE_ATTRIBUTES(ScramblePlugin plugin) {
		return new VariableKey<>("Puzzle_Attributes_" + plugin.getPuzzleName());
	}

	static {
		try {
			Font lcdFont = Font.createFont(Font.TRUETYPE_FONT,
					CalCubeTimerGui.class.getResourceAsStream("Digiface Regular.ttf"));
			GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(lcdFont);
		} catch (FontFormatException | IOException e) {
			throw Throwables.propagate(e);
		}
    }
	public static final VariableKey<Font> TIMER_FONT = new VariableKey<>("Timer_font");
	public static final VariableKey<Font> SCRAMBLE_FONT = new VariableKey<>("Scramble_font");

	public static final VariableKey<Boolean> SIDE_BY_SIDE_SCRAMBLE = new VariableKey<>("GUI_ScrambleView_sideBySide");
	public static final VariableKey<Boolean> SPEAK_INSPECTION = new VariableKey<>("Misc_Voices_readInspection");
	public static final VariableKey<Boolean> SPEAK_TIMES = new VariableKey<>("Misc_Voices_readTimes");
	public static final VariableKey<Boolean> COMPETITION_INSPECTION = new VariableKey<>("GUI_Timer_competitionInspection");
	public static final VariableKey<Boolean> FOCUSABLE_BUTTONS = new VariableKey<>("GUI_focusableButtons");
	public static final VariableKey<Boolean> CLOCK_FORMAT = new VariableKey<>("Misc_isClockFormat");
	public static final VariableKey<Boolean> INVERTED_HUNDREDTHS = new VariableKey<>("Stackmat_isInvertedHundredths");
	public static final VariableKey<Boolean> INVERTED_SECONDS = new VariableKey<>("Stackmat_isInvertedSeconds");
	public static final VariableKey<Boolean> INVERTED_MINUTES = new VariableKey<>("Stackmat_isInvertedMinutes");
	public static final VariableKey<Boolean> PROMPT_FOR_NEW_TIME = new VariableKey<>("Misc_isPromptForNewTime");
	public static final VariableKey<Boolean> SCRAMBLE_POPUP = new VariableKey<>("Scramble_Popup_isEnabled");
	public static final VariableKey<Boolean> TIMING_SPLITS = new VariableKey<>("Splits_isEnabled");
	public static final VariableKey<Boolean> STACKMAT_ENABLED = new VariableKey<>("Stackmat_isEnabled");
	public static final VariableKey<Boolean> HIDE_SCRAMBLES = new VariableKey<>("GUI_Timer_isHideScrambles");
	public static final VariableKey<Boolean> SPACEBAR_ONLY = new VariableKey<>("GUI_Timer_isSpacebarOnly");
	public static final VariableKey<Boolean> STACKMAT_EMULATION = new VariableKey<>("GUI_Timer_stackmatEmulation");
	public static final VariableKey<Boolean> LESS_ANNOYING_DISPLAY = new VariableKey<>("GUI_Timer_isLessAnnoyingDisplay");
	public static final VariableKey<Boolean> FULLSCREEN_TIMING = new VariableKey<>("GUI_Timer_isFullScreenWhileTiming");
	public static final VariableKey<Boolean> METRONOME_ENABLED = new VariableKey<>("Misc_Metronome_isEnabled");
	public static VariableKey<Boolean> RA_TRIMMED(RollingAverageOf index, PuzzleType var) {
		String key = "Puzzle_RA" + index.getCode() + "Trimmed";
		if(var != null)
			key += "_" + var.toString();
		return new VariableKey<>(key);
	}
	public static VariableKey<Boolean> COLUMN_VISIBLE(JTable src, int index) {
		return new VariableKey<>("GUI_xmlLayout_" + src.getName() + index);
	}

	public static final VariableKey<Dimension> STATS_DIALOG_DIMENSION = new VariableKey<>("GUI_StatsDialog_dimension");
	public static final VariableKey<Dimension> MAIN_FRAME_DIMENSION = new VariableKey<>("GUI_MainFrame_dimension");

	public static final VariableKey<Point> SCRAMBLE_VIEW_LOCATION = new VariableKey<>("GUI_ScrambleView_location");
	public static final VariableKey<Point> MAIN_FRAME_LOCATION = new VariableKey<>("GUI_MainFrame_location");

	public static final VariableKey<Color> TIMER_BG = new VariableKey<>("GUI_Timer_Color_background");
	public static final VariableKey<Color> TIMER_FG = new VariableKey<>("GUI_Timer_Color_foreground");
	public static final VariableKey<Color> SCRAMBLE_UNSELECTED = new VariableKey<>("Scramble_Color_unselected");
	public static final VariableKey<Color> SCRAMBLE_SELECTED = new VariableKey<>("Scramble_Color_selected");
	public static final VariableKey<Color> BEST_RA = new VariableKey<>("Statistics_Color_bestRA");
	public static final VariableKey<Color> BEST_TIME = new VariableKey<>("Statistics_Color_bestTime");
	public static final VariableKey<Color> CURRENT_AVERAGE = new VariableKey<>("Statistics_Color_currentAverage");
	public static final VariableKey<Color> WORST_TIME = new VariableKey<>("Statistics_Color_worstTime");
	public static VariableKey<Color> PUZZLE_COLOR(ScramblePlugin plugin, String faceName) {
		return new VariableKey<>("Puzzle_Color_" + plugin.getPuzzleName() + "_face" + faceName);
	}

	public static final VariableKey<Double> MIN_SPLIT_DIFFERENCE = new VariableKey<>("Splits_minimumSplitDifference");

	private final String propsName;

	VariableKey(String propertiesName) {
		propsName = propertiesName;
	}

	public String toKey() {
		return propsName;
	}

	@Override
	public String toString() {
		return propsName;
	}
}
