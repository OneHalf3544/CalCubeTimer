package net.gnehzr.cct.misc;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.statistics.SolveTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;

public class Utils {

	private static final Logger LOG = LogManager.getLogger(Utils.class);

	private static DecimalFormat getDecimalFormat() {
		DecimalFormat df = new DecimalFormat("0.00");
		df.setRoundingMode(RoundingMode.HALF_UP);
		df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return df;
	}

	public static String getDecimalSeparator() {
		return "" + getDecimalFormat().getDecimalFormatSymbols().getDecimalSeparator();
	}

	private Utils() {}

	public static <T extends Comparable<T>> boolean lessThan(T v1, T v2) {
		return v1.compareTo(v2) < 0;
	}

	public static <T extends Comparable<T>> boolean moreThan(T v1, T v2) {
		return v1.compareTo(v2) > 0;
	}

	public static boolean equalDouble(double a, double b) {
		return round(a, 2) == round(b, 2);
	}

	private static double round(double c, int decimalPlaces) {
		int pow = (int) Math.pow(10, decimalPlaces);
		return Math.round(c * pow) / (double) pow;
	}

	public static String formatTime(SolveTime solveTime, boolean useClockFormat) {
		if(solveTime.isInfiniteTime()) {
			return "N/A";
		}
		return useClockFormat ? clockFormat(solveTime) : format(solveTime);
	}

	public static String format(SolveTime seconds) {
		return getDecimalFormat().format(seconds.getTime().toMillis());
	}

	@Deprecated
	public static String format(double seconds) {
		return getDecimalFormat().format(seconds);
	}

	static String clockFormat(SolveTime solveTime) {
		Duration time = solveTime.getTime();
		long hours = time.toHours();
		time = time.minusHours(hours);
		long minutes = time.toMinutes();
		time = time.minusMinutes(minutes);
		long seconds = time.toMillis() / 1000;

		return (hours == 0 ? (minutes == 0 ? "" : minutes + ":" + (seconds < 10 ? "0" : "")) :
				hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : ""))
				+ format(time.toMillis() / 1000.0 );
	}

	public static Color invertColor(Color c) {
		if(c == null) {
			return Color.BLACK;
		}
		return new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue());
	}

	public static String colorToString(Color c) {
		if(c == null) {
			return "";
		}
		return padWith0s(Integer.toHexString(c.getRGB() & 0xffffff));
	}

	private static String padWith0s(String s) {
		int pad = 6 - s.length();
		if(pad > 0) {
			for(int i = 0; i < pad; i++) {
				s = "0" + s;
			}
		}
		return s;
	}

	public static Color stringToColor(String s, boolean nullIfInvalid) {
		try {
			return stringToColor(s);
		} catch(Exception e) {
			return nullIfInvalid ? null : Color.WHITE;
		}
	}

	public static Color stringToColor(String s) {
		return new Color(Integer.parseInt(s, 16));
	}

	public static String fontToString(Font f) {
		String style = "";
		if(f.isPlain())
			style = "plain";
		else {
			if(f.isBold())
				style += "bold";
			if(f.isItalic())
				style += "italic";
		}
		return f.getFontName() + "-" + style + "-" + f.getSize();
	}

	public static int showWarningDialog(Component c, String message) {
		String[] ok = new String[] { StringAccessor.getString("Utils.ok") };
		return JOptionPane.showOptionDialog(c, message, StringAccessor.getString("Utils.warning"), JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null, ok,
				ok[0]);
	}

	public static void showErrorDialog(Window c, String message) {
		showErrorDialog(c, null, message, null);
	}
	
	public static void showErrorDialog(Window c, String message, String title) {
		showErrorDialog(c, null, message, title);
	}

	public static void showErrorDialog(Window c, Throwable e) {
		showErrorDialog(c, e, null);
	}

	public static void showErrorDialog(Window window, Throwable e, String message) {
		showErrorDialog(window, e, message, null);
	}
	public static void showErrorDialog(Window window, Throwable e, String message, String title) {
		StringBuilder msg = new StringBuilder();
		if(message != null)
			msg.append(message).append("\n");
		if(e != null) {
			CharArrayWriter caw = new CharArrayWriter();
			e.printStackTrace(new PrintWriter(caw));
			msg.append(caw.toString());
		}
		if(title == null)
			title = StringAccessor.getString("Utils.error");
		new DialogWithDetails(window, title, message, msg.toString()).setVisible(true);
	}

	public static int showYesNoDialog(Component c, String message) {
		String[] yesNo = new String[] { StringAccessor.getString("Utils.yes"), StringAccessor.getString("Utils.no") };
		return JOptionPane.showOptionDialog(c, message, StringAccessor.getString("Utils.confirm"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, yesNo, yesNo[0]);
	}

	public static int showYesNoCancelDialog(Component c, String message) {
		String[] yesNo = new String[] { StringAccessor.getString("Utils.yes"), StringAccessor.getString("Utils.no"), StringAccessor.getString("Utils.cancel") };
		return JOptionPane.showOptionDialog(c, message, StringAccessor.getString("Utils.confirm"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, yesNo, yesNo[0]);
	}

	public static int showConfirmDialog(Component c, String message) {
		String[] yesNo = new String[] { StringAccessor.getString("Utils.ok") };
		return JOptionPane.showOptionDialog(c, message, StringAccessor.getString("Utils.confirm"), JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
				null, yesNo, yesNo[0]);
	}
}
