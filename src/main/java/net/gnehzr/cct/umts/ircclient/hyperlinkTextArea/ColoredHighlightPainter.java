package net.gnehzr.cct.umts.ircclient.hyperlinkTextArea;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class ColoredHighlightPainter implements HighlightPainter {

	private boolean underline;
	private Color color;

	public ColoredHighlightPainter(boolean underline, boolean isClickable, Color color) {
		this.underline = underline;
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	@Override
	public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
		if(underline) {
			Rectangle r0 = null;
			Rectangle r1 = null;
			try {
				r0 = c.modelToView(p0);
				r1 = c.modelToView(p1);
			} catch(BadLocationException e) {
				return;
			}
			g.setColor(color);
			int startY = r0.y + r0.height;
			int endY = r1.y + r1.height;
			for(int y = startY; y <= endY; y += r0.height)
				g.drawLine(y == startY ? r0.x : 0, y, y == endY ? r1.x : bounds.getBounds().width, y);
		}
	}
}
