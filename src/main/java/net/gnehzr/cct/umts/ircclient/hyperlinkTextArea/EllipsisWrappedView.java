package net.gnehzr.cct.umts.ircclient.hyperlinkTextArea;

import org.apache.log4j.Logger;

import javax.swing.text.*;
import javax.swing.text.Highlighter.Highlight;
import java.awt.*;

public class EllipsisWrappedView extends WrappedPlainView {

	private static final Logger LOG = Logger.getLogger(EllipsisWrappedView.class);

	private Highlighter highlighter;
	public EllipsisWrappedView(Element e, boolean wrapWord, Highlighter highlighter) {
		super(e, wrapWord);
		this.highlighter = highlighter;
	}
	
	public void paint(Graphics g, Shape a) {
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		super.paint(g, a);
	}
	protected void drawLine(int p0, int p1, Graphics g, int x, int y) {
		super.drawLine(p0, p1, g, x, y);
		
		boolean isHardBreak = p1 <= getDocument().getLength();
		try {
			isHardBreak &= !getDocument().getText(p1-1, 1).equals("\n");
		} catch(BadLocationException e) {
			LOG.info("unexpected exception", e);
		}
		if(isHardBreak) {
			Color c = g.getColor();
			g.setColor(Color.RED);
			g.drawString("...", getWidth(), y);
			g.setColor(c);
		}
	}
	
	protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
        return drawLine(g, x, y, p0, p1);
	}
	protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
		return drawLine(g, x, y, p0, p1);
	}
	private int drawLine(Graphics g, int x, int y, int p0, int p1) throws BadLocationException {
		g.setColor(Color.BLACK);

		for(Highlight h : highlighter.getHighlights()) {
			if(!(h.getPainter() instanceof ColoredHighlightPainter))
				continue;
			ColoredHighlightPainter painter = (ColoredHighlightPainter) h.getPainter();
			int start = Math.max(h.getStartOffset(), p0);
			int end = Math.min(h.getEndOffset(), p1);
			//TODO - move highlighter into here?
			if(p0 <= start && end <= p1 && end > start) {
				g.setColor(Color.BLACK);
				x = drawString(g, x, y, p0, start);
				g.setColor(painter.getColor());
				x = drawString(g, x, y, start, end);
				p0 = end;
			}
		}
		g.setColor(Color.BLACK);
		return drawString(g, x, y, p0, p1);
	}
	private int drawString(Graphics g, int x, int y, int start, int end) throws BadLocationException {
		Document doc = getDocument();
		Segment segment = getLineBuffer();
		doc.getText(start, end - start, segment);
		return Utilities.drawTabbedText(segment, x, y, g, null, start);
	}
}
