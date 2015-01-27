package net.gnehzr.cct.umts.ircclient.hyperlinkTextArea;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.View;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HyperlinkTextArea extends JTextArea implements DocumentListener, MouseMotionListener, MouseListener {

	private static final Logger LOG = Logger.getLogger(HyperlinkTextArea.class);

	// TODO - there may be some bug where coloring is getting lost
	private Document doc;
	public HyperlinkTextArea() {
		doc = getDocument();
		setUI(new BasicTextAreaUI() {
			@Override
			public View create(Element elem) {
				return new EllipsisWrappedView(elem, getWrapStyleWord(), getHighlighter());
			}
		});
		doc.addDocumentListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		refreshBorder();
		getCaret().setSelectionVisible(true);
	}
	@Override
	public void setFont(Font f) {
		super.setFont(f);
		refreshBorder();
	}
	private void refreshBorder() {
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, getFontMetrics(getFont()).stringWidth("...") + 1));
	}

	public static interface HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkTextArea source, String url, int linkNum);
	}
	private ArrayList<HyperlinkListener> hyperlinkListeners = new ArrayList<>();
	public void addHyperlinkListener(HyperlinkListener l) {
		hyperlinkListeners.add(l);
	}
	public void removeHyperlinkListener(HyperlinkListener l) {
		hyperlinkListeners.remove(l);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(getMousePosition() != null) {
			int pos = viewToModel(getMousePosition());
			for(int linkNum = 0; linkNum < clickableLinks.size(); linkNum++) {
				Highlight h = clickableLinks.get(linkNum);
				int start = h.getStartOffset();
				int end = h.getEndOffset();
				if(start <= pos && pos <= end) {
					try {
						String text = doc.getText(start, end - start);
						for(HyperlinkListener l : hyperlinkListeners)
							l.hyperlinkUpdate(this, text, linkNum);
					} catch(BadLocationException e1) {
						LOG.info("unexpected exception", e1);
					}
					return;
				}
			}
		}
		setCursor(Cursor.getDefaultCursor());
	}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {
		int start = getSelectionStart();
		int end = getSelectionEnd();
		if(start != end) {
			StringSelection ss = new StringSelection(getSelectedText());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
			if(!isFocusable())
				select(start, start);
		}
		repaint();
	}
	@Override
	public String getSelectedText() {
		return super.getSelectedText().replaceAll(ZWSP, "");
	}
	public boolean isSelectingText() {
		return getSelectionStart() != getSelectionEnd();
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		repaint();
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		updateCursor();
	}

	private void updateCursor() {
		Point mousePosition = getMousePosition();
		if (mousePosition != null) {
			Integer textCursorPosition = viewToModel(mousePosition);
			for(Highlight h : clickableLinks) {
				if(h.getStartOffset() <= textCursorPosition && textCursorPosition <= h.getEndOffset()) {
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					return;
				}
			}
		}
		setCursor(Cursor.getDefaultCursor());
	}
	@Override
	public void changedUpdate(DocumentEvent e) {}
	@Override
	public void insertUpdate(DocumentEvent e) {
		updateCursor();
		repaint(); //need to update highlights
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		updateCursor();
		repaint(); //need to update highlights
	}

	private ArrayList<Highlight> clickableLinks = new ArrayList<>();
	public void clear() {
		setText("");
		getHighlighter().removeAllHighlights();
		clickableLinks.clear();
		SwingUtilities.invokeLater(this::updateCursor);
	}
	
	public boolean appendToLink(int linkNumber, String str) {
		try {
			insert(str, clickableLinks.get(linkNumber).getEndOffset());
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	public int getLineOfLink(int linkNumber) throws BadLocationException {
		return getLineOfOffset(clickableLinks.get(linkNumber).getStartOffset());
	}
	
	@Override
	public void append(String str) {
		append(str, null);
	}

	private static final String ZWSP = "\u200B";
	private static final Pattern URL_PATTERN = Pattern.compile("(?:<(\\S+)>)?.*?\\b(?:(http://\\S+)\\b|(cct://[^\"\\n]+))");
	public HashMap<Integer, CCTLink> append(String str, Color c) {
		//this is probably ruining the synchronized nature of our superclass's method
		int length = doc.getLength();
		int end = length + str.length();
		super.append(str);

		HashMap<Integer, CCTLink> cctlinks = new HashMap<>();
		try {
			int offset = 0;
			Matcher m = URL_PATTERN.matcher(str);
			while(m.find()) {
				String nick = m.group(1);
				String httpURL = m.group(2);
				String cctURL = m.group(3);
				int group = httpURL != null ? 2 : 3;
				if(c != null)
					getHighlighter().addHighlight(length + offset, length + m.start(group), new ColoredHighlightPainter(false, false, c));
				offset = m.end(group);
				boolean htmlLink = group == 2;
				if(!htmlLink)
					cctlinks.put(clickableLinks.size(), new CCTLink(cctURL, nick));
				clickableLinks.add((Highlight) getHighlighter().addHighlight(length + m.start(group), length + offset, new ColoredHighlightPainter(true, true, Color.BLUE)));
			}
			if(c != null) {
				super.append(ZWSP); //need this to prevent highlights from expanding
				getHighlighter().addHighlight(length + offset, end, new ColoredHighlightPainter(false, false, c));
			}
		} catch(BadLocationException e) {
			LOG.info("unexpected exception", e);
		}
		return cctlinks;
	}
	
	public static class CCTLink {
		public int set, number = 0;
		public String scramble, nick, variation = "";
		public boolean fragmentation;
		public CCTLink(String url, String nick) {
			this.scramble = url.substring(6);
			this.nick = nick;

			fragmentation = scramble.startsWith("*");
			if(fragmentation)
				scramble = scramble.substring(1);

			int temp;
			if(scramble.startsWith("#")) {
				try {
					temp = scramble.indexOf(':');
					number = Integer.parseInt(scramble.substring(1, temp));
					scramble = scramble.substring(temp + 1);
				} catch(Exception ee) {
					LOG.info("ignored exception", ee);
				}
			}

			if((temp = scramble.indexOf(':')) != -1) {
				variation = scramble.substring(0, temp);
				scramble = scramble.substring(temp + 1);
			}
		}
	}
}
