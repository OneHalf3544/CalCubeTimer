package net.gnehzr.cct.main;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.lafwidget.LafWidget;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scramble text field component
 */
@Singleton
public class ScrambleHyperlinkArea extends JScrollPane implements ComponentListener, HyperlinkListener, MouseListener, MouseMotionListener {

	private static final Logger LOG = LogManager.getLogger(ScrambleHyperlinkArea.class);
	private static final Pattern NULL_SCRAMBLE_REGEX = Pattern.compile("^(.+)()$");

	private ScramblePopupFrame scramblePopup;
	private JEditorPane scramblePane = null;
	private JPopupMenu success;
	private JLabel successMsg;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;

	private ScrambleString currentScramble;
	private String incrementScramble;
	private ScrambleString fullScramble;
	private PuzzleType currentCustomization;
	private StringBuilder part1, part2, part3;
	private int moveNum;
	private String backgroundColor;

	@Inject
	public ScrambleHyperlinkArea(ScramblePopupFrame scramblePopup, Configuration configuration, ScramblePluginManager scramblePluginManager) {
		super(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scramblePopup = scramblePopup;
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		scramblePane = new JEditorPane("text/html", null) { 
			@Override
			public void updateUI() {
				Border t = getBorder();
				super.updateUI();
				setBorder(t);
			}
		};
		scramblePane.setEditable(false);
		scramblePane.setBorder(null);
		scramblePane.setOpaque(false);
		scramblePane.addHyperlinkListener(this);
		setViewportView(scramblePane);
		setOpaque(false);
		setBorder(null);
		getViewport().setOpaque(false);
		resetPreferredSize();
		addComponentListener(this);
		scramblePane.setFocusable(false); //this way, we never steal focus from the keyboard timer
		scramblePane.addMouseListener(this);
		scramblePane.addMouseMotionListener(this);
		scramblePane.putClientProperty(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.FALSE);
		
		success = new JPopupMenu();
		success.setFocusable(false);
		success.add(successMsg = new JLabel());
		updateStrings();
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount() == 2) {
	        StringSelection ss = new StringSelection(currentScramble.getScramble());
	        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);

			success.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
		}
	}
	public void updateStrings() {
		scramblePane.setToolTipText(StringAccessor.getString("ScrambleArea.tooltip"));
		successMsg.setText(StringAccessor.getString("ScrambleArea.copymessage"));
		success.pack();
	}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {
		success.setVisible(false);
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		success.setVisible(false);
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		success.setVisible(false);
	}

	@Override
	public void updateUI() {
		Border t = getBorder();
		super.updateUI();
		setBorder(t);
	}

	public void resetPreferredSize() {
		setPreferredSize(new Dimension(0, 100));
	}

	public void setScramble(ScrambleString newScramble, PuzzleType sc) {
		currentCustomization = sc;
		fullScramble = newScramble;
		currentScramble = fullScramble;

		Font font = configuration.getFont(VariableKey.SCRAMBLE_FONT, false);
		StringBuilder fontStyle = new StringBuilder(); 
		if(font.isItalic())
			fontStyle.append("font-style: italic; "); 
		else if(font.isPlain())
			fontStyle.append("font-style: normal; "); 
		if(font.isBold())
			fontStyle.append("font-weight: bold; "); 
		else
			fontStyle.append("font-weight: normal; "); 
		
		String selected = Utils.colorToString(configuration.getColor(VariableKey.SCRAMBLE_SELECTED, false));
		String unselected = Utils.colorToString(configuration.getColor(VariableKey.SCRAMBLE_UNSELECTED, false));
		part1 = new StringBuilder("<html><head><style type=\"text/css\">") 
			.append("a { color: #").append(unselected).append("; text-decoration: none; }")  
			.append("a#");
		part2 = new StringBuilder(" { color: #").append(selected).append("; }")  
			.append("span { font-family: ").append(font.getFamily()).append("; font-size: ").append(font.getSize()).append("; ").append(fontStyle).append("; }")    
			.append("sub { font-size: ").append(font.getSize() / 2 + 1).append("; }")  
			.append("</style></head>");
		part3 = new StringBuilder("<center>");
		String s = currentScramble.getScramble();
		StringBuilder plainScramble = new StringBuilder();
		Matcher m;
		int num = 0;
		Pattern regex = currentCustomization.getScramblePlugin().getTokenRegex();
		if(regex == null || fullScramble == null) {
			regex = NULL_SCRAMBLE_REGEX;
		}
		
		while((m = regex.matcher(s)).matches()){
			String str = m.group(1).trim();
			plainScramble.append(" ").append(str);
			part3.append("<a id='").append(num).append("' href=\"").append(num).append(plainScramble).append("\"><span>").append(currentCustomization.getScramblePlugin().htmlify(" " + str)).append("</span></a>");
			s = m.group(2).trim();
			num++;
		}
		part3.append("</center></body></html>"); 
		scramblePane.setCaretPosition(0);
		String description = (num - 1) + plainScramble.toString();
		hyperlinkUpdate(new HyperlinkEvent(scramblePane, HyperlinkEvent.EventType.ACTIVATED, null, description));
		setProperSize();
		Container par = getParent();
		if(par != null)
			par.validate();
	}

	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			incrementScramble = e.getDescription();
			String[] moveAndScramble = incrementScramble.split(" ", 2);
			if(moveAndScramble.length != 2) { //this happens if we have an empty null scramble
				incrementScramble = "";
				scramblePane.setText(incrementScramble);
			} else {
				moveNum = Integer.parseInt(moveAndScramble[0]);
				incrementScramble = moveAndScramble[1];
			}
			updateScramblePane();
			scramblePopup.setScramble(incrementalScramble(), fullScramble, currentCustomization.getScrambleVariation());
		}
	}

	private ScrambleString incrementalScramble() {
		try {
            return currentCustomization.importScramble(incrementScramble);
		}
		catch(InvalidScrambleException e0) { //this could happen if a null scramble is imported
			LOG.info("unexpected exception", e0);
            currentCustomization = scramblePluginManager.NULL_SCRAMBLE_CUSTOMIZATION;
            try {
                return currentCustomization.importScramble(incrementScramble);
            } catch (InvalidScrambleException e1) {
                LOG.info("unexpected exception", e1);
				return null;
            }
        }
	}

	private void updateScramblePane() {
		int caretPos = scramblePane.getCaretPosition();
		scramblePane.setDocument(new HTMLEditorKit().createDefaultDocument());
		String bgColor = "";
		if(backgroundColor != null) {
			bgColor = " bgcolor='" + backgroundColor + "'";
		}

		scramblePane.setText("");
		
		scramblePane.setText(String.valueOf(part1) + moveNum + part2 + "<body" + bgColor + ">" + part3);
		scramblePane.setCaretPosition(caretPos);
	}

	private boolean focused;

	public void refresh() {
		setTimerFocused(focused);
		setScramble(currentScramble, currentCustomization);
	}
	//this will be called by the KeyboardTimer to hide scrambles when necessary
	public void setTimerFocused(boolean focused) {
		this.focused = focused;
		backgroundColor = (!focused && configuration.getBoolean(VariableKey.HIDE_SCRAMBLES)) ? "black" : null;
		updateScramblePane();
	}

	private void setProperSize() {
		if(scramblePane.getDocument().getLength() == 0) {
			setPreferredSize(new Dimension(0, 0));
			return;
		}
		try {
			int height = 0;
			if(getBorder() != null) {
				Insets i = getBorder().getBorderInsets(this);
				height += i.top + i.bottom;
			}
			Rectangle r = scramblePane.modelToView(scramblePane.getDocument().getLength());
			if(r != null) {
				setPreferredSize(new Dimension(0, height + r.y + r.height + 5));
			} else
				resetPreferredSize(); //this will call setProperSize() again, with r != null
		} catch (BadLocationException e) {
			LOG.info("unexpected exception", e);
		}
	}
	
	@Override
	public void componentHidden(ComponentEvent arg0) {}
	@Override
	public void componentMoved(ComponentEvent arg0) {}
	@Override
	public void componentResized(ComponentEvent arg0) {
		setProperSize();
	}
	@Override
	public void componentShown(ComponentEvent arg0) {}
}
