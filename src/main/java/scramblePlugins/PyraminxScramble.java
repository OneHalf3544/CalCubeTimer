package scramblePlugins;

import net.gnehzr.cct.scrambles.Scramble;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.List;

public class PyraminxScramble extends Scramble {

	private static final Logger LOG = Logger.getLogger(PyraminxScramble.class);

	public PyraminxScramble() throws InvalidScrambleException {
		this(0, Collections.<String>emptyList());
	}

	public PyraminxScramble(String s, List<String> attrs) throws InvalidScrambleException {
		super("Pyraminx", true, s);
		if(!setAttributes(attrs)) {
			throw new InvalidScrambleException(s);
		}
	}

	public PyraminxScramble(int length, List<String> attrs) {
		super("Pyraminx", true);
		this.length = length;
		setAttributes(attrs);
	}

	@Override
	public final String[][] getFaceNamesColors() {
		return new String[][] {
				{ "F", "D", "L", "R" },
	  			{ "ff0000", "0000ff", "00ff00", "ffff00" }
		};
	}

	@Override
	public Scramble importScramble(String variation, String scramble, String generatorGroup, List<String> attributes) throws InvalidScrambleException {
		return new PyraminxScramble(scramble, attributes);
	}

	@Override
	protected Scramble createScramble(String variation, int length, String generatorGroup, List<String> attributes) {
		return new PyraminxScramble(length, attributes);
	}

	@Override
	public String htmlify(String formatMe) {
		return formatMe;
	}

	@NotNull
	@Override
	public List<String> getAttributes() {
		return NULL_SCRAMBLE.getAttributes();
	}

	@NotNull
	@Override
	public List<String> getDefaultAttributes() {
		return NULL_SCRAMBLE.getDefaultAttributes();
	}

	@Override
	public String[] getDefaultGenerators() {
		return NULL_SCRAMBLE.getDefaultGenerators();
	}

	@NotNull
	@Override
	public final String[] getVariations() {
		return new String[]{"Pyraminx"};
	}

	@NotNull
	@Override
	public final int[] getDefaultLengths() {
		return new int[]{25};
	}

	@Override
	public final int getDefaultUnitSize() {
		return 30;
	}

	@Override
	public final Pattern getTokenRegex() {
		return Pattern.compile("^([ULRBulrb]'?)(.*)$");
	}
	
	private int[][] image;

	private boolean setAttributes(List<String> attributes){
		initializeImage();
		
		if(scramble != null) {
			return validateScramble();
		} else if(length == 0) { //we rely on scrambles of length zero for displaying the image
			scramble = "";
		} else {
			//hacked in optimal scrambles for now
			scramble = scramble();
		}
		return validateScramble();
	}

	private void initializeImage() {
		image = new int[4][9];
		for(int i = 0; i < image.length; i++){
			for(int j = 0; j < image[0].length; j++){
				image[i][j] = i;
			}
		}
	}

	private static String regexp = "^[ULRBulrb]'?$";
	private boolean validateScramble() {
		String[] strs = scramble.split("\\s+");
		length = strs.length;

		for (String str : strs) {
			if (!str.matches(regexp)) return false;
		}

		try{
			for(int i = 0; i < strs.length; i++){
				int face = "ULRBulrb".indexOf(strs[i].charAt(0));
				if(face == -1) return false;
				int dir = (strs[i].length() == 1 ? 1 : 2);
				if(face >= 4) turnTip(face - 4, dir);
				else turn(face, dir);
			}
		} catch(Exception e){
			LOG.info("unexpected exception", e);
			return false;
		}

		return true;
	}

	List<String> b = new ArrayList<>();
	static List<Integer> g = new ArrayList<>();
	static List<Integer> f = new ArrayList<>();
	List<Integer> k = new ArrayList<>();
	List<Integer> h = new ArrayList<>();
	List<Integer> i = new ArrayList<>();
	static List<List<Integer>> d = new ArrayList<>();
	static List<List<Integer>> e = new ArrayList<>();

	static {
		int c, p, q, l, m;
		for (p = 0; p < 720; p++) {
			g.add(-1);
			d.add(p, new ArrayList<>());
			for (m = 0; m < 4; m++)
				d.get(p).add(m, w(p, m));
		}
		g.set(0, 0);
		for (l = 0; l <= 6; l++)
			for (p = 0; p < 720; p++)
				if (g.get(p) == l)
					for (m = 0; m < 4; m++) {
						q = p;
						for (c = 0; c < 2; c++) {
							q = d.get(q).get(m);
							if (g.get(q) == -1)
								g.set(q, l + 1);
						}
					}
		for (p = 0; p < 2592; p++) {
			f.add(-1);
			e.add(new ArrayList<>());
			for (m = 0; m < 4; m++)
				e.get(p).add(x(p, m));
		}
		f.set(0, 0);
		for (l = 0; l <= 5; l++)
			for (p = 0; p < 2592; p++)
				if (f.get(p) == l)
					for (m = 0; m < 4; m++) {
						q = p;
						for (c = 0; c < 2; c++) {
							q = e.get(q).get(m);
							if (f.get(q) == -1)
								f.set(q, l + 1);
						}
					}
	}

	private String scramble() {
		k = new ArrayList<>();
		int t = 0, s = 0, q = 0, m, l, p;
		h = new ArrayList<>();
		h.add(0);
		h.add(1);
		h.add(2);
		h.add(3);
		h.add(4);
		h.add(5);
		for (m = 0; m < 4; m++) {
			p = m + n(6 - m);
			l = h.get(m);
			h.set(m, h.get(p));
			h.set(p, l);
			if (m != p)
				s++;
		}
		if (s % 2 == 1) {
			l = h.get(4);
			h.set(4, h.get(5));
			h.set(5, l);
		}
		s = 0;
		i = new ArrayList<>();
		for (m = 0; m < 5; m++) {

			i.set(m, n(2));
			s += i.get(m);
		}
		i.set(5, s % 2);
		for (m = 6; m < 10; m++) {
			i.set(m, n(3));
		}
		for (m = 0; m < 6; m++) {
			l = 0;
			for (p = 0; p < 6; p++) {
				if (h.get(p) == m)
					break;
				if (h.get(p) > m)
					l++;
			}
			q = q * (6 - m) + l;
		}
		for (m = 9; m >= 6; m--)
			t = t * 3 + i.get(m);
		for (m = 4; m >= 0; m--)
			t = t * 2 + i.get(m);
		if (q != 0 || t != 0)
			for (m = 0; m < 12; m++)
				if (v(q, t, m, -1))
					break;

		String scramble = "";
		for (p = 0; p < k.size(); p++)
			scramble += new String[] { "U", "L", "R", "B" }[k.get(p) & 7]
					+ new String[] { "", "'" }[(k.get(p) & 8) / 8] + " ";
		String[] tips = new String[] { "l", "r", "b", "u" };
		for (p = 0; p < 4; p++) {
			q = n(3);
			if (q < 2)
				scramble += tips[p] + new String[] { "", "'" }[q] + " ";
		}
		return scramble;
	}

	private boolean v(int q, int t, int l, int c) {
		if (l == 0) {
			if (q == 0 && t == 0)
				return true;
		} else {
			if (g.get(q) > l || f.get(t) > l)
				return false;
			int p, s, a, m;
			for (m = 0; m < 4; m++)
				if (m != c) {
					p = q;
					s = t;
					for (a = 0; a < 2; a++) {
						p = d.get(p).get(m);
						s = e.get(s).get(m);
						k.set(k.size(), m + 8 * a);
						if (v(p, s, l - 1, m))
							return true;
						k.remove(k.size() - 1);
					}
				}
		}
		return false;
	}

	private static int w(int p, int m) {
		int a, l, c, q = p;
		List<Integer> s = new ArrayList<>();
		s.add(null); s.add(null); s.add(null); s.add(null); s.add(null); s.add(null); s.add(null);

		for (a = 1; a <= 6; a++) {
			c = (int) Math.floor(q / a);
			l = q - a * c;
			q = c;
			for (c = a - 1; c >= l; c--) {
				int val = (c < s.size() && s.get(c) != null) ? s.get(c) : 0;
				s.set(c + 1, val);
			}
			s.set(l, 6 - a);
		}
		if (m == 0)
			y(s, 0, 3, 1);
		if (m == 1)
			y(s, 1, 5, 2);
		if (m == 2)
			y(s, 0, 2, 4);
		if (m == 3)
			y(s, 3, 4, 5);
		q = 0;
		for (a = 0; a < 6; a++) {
			l = 0;
			for (c = 0; c < 6; c++) {
				if (s.get(c) == a)
					break;
				if (s.get(c) > a)
					l++;
			}
			q = q * (6 - a) + l;
		}
		return q;
	}

	private static int x(int p, int m) {
		int a, l, c, t = 0, q = p;
		List<Integer> s = new ArrayList<>();
		for (a = 0; a <= 4; a++) {
			s.add(q & 1);
			q >>= 1;
			t ^= s.get(a);
		}
		s.add(t);
		for (a = 6; a <= 9; a++) {
			c = (int) Math.floor(q / 3);
			l = q - 3 * c;
			q = c;
			s.add(l);
		}
		if (m == 0) {
			s.set(6, s.get(6) + 1);
			if (s.get(6) == 3)
				s.set(6, 0);
			y(s, 0, 3, 1);
			s.set(1, s.get(1) ^ 1);
			s.set(3, s.get(3) ^ 1);
		}
		if (m == 1) {
			s.set(7, s.get(7) + 1);
			if (s.get(7) == 3)
				s.set(7, 0);
			y(s, 1, 5, 2);
			s.set(2, s.get(2) ^ 1);
			s.set(5, s.get(5) ^ 1);
		}
		if (m == 2) {
			s.set(8, s.get(8) + 1);
			if (s.get(8) == 3)
				s.set(8, 0);
			y(s, 0, 2, 4);
			s.set(0, s.get(0) ^ 1);
			s.set(2, s.get(2) ^ 1);
		}
		if (m == 3) {
			s.set(9, s.get(9) + 1);
			if (s.get(9) == 3)
				s.set(9, 0);
			y(s, 3, 4, 5);
			s.set(3, s.get(3) ^ 1);
			s.set(4, s.get(4) ^ 1);
		}
		q = 0;
		for (a = 9; a >= 6; a--)
			q = q * 3 + s.get(a);
		for (a = 4; a >= 0; a--)
			q = q * 2 + s.get(a);
		return q;
	}
	 
	private static void y(List<Integer> p, int a, int c, int t) {
	 int s = p.get(a);
	 p.set(a, p.get(c));
	 p.set(c, p.get(t));
	 p.set(t, s);
	}
	 
	private static int n(int c) {
	 return (int) Math.floor(Math.random()*c);
	}

	private void turn(int side, int dir){
		for(int i = 0; i < dir; i++){
			turn(side);
		}
	}

	private void turnTip(int side, int dir){
		for(int i = 0; i < dir; i++){
			turnTip(side);
		}
	}

	private void turn(int s){
		switch(s){
			case 0:
				swap(0, 8, 3, 8, 2, 2);
				swap(0, 1, 3, 1, 2, 4);
				swap(0, 2, 3, 2, 2, 5);
				break;
			case 1:
				swap(2, 8, 1, 2, 0, 8);
				swap(2, 7, 1, 1, 0, 7);
				swap(2, 5, 1, 8, 0, 5);
				break;
			case 2:
				swap(3, 8, 0, 5, 1, 5);
				swap(3, 7, 0, 4, 1, 4);
				swap(3, 5, 0, 2, 1, 2);
				break;
			case 3:
				swap(1, 8, 2, 2, 3, 5);
				swap(1, 7, 2, 1, 3, 4);
				swap(1, 5, 2, 8, 3, 2);
				break;
		}
		turnTip(s);
	}

	private void turnTip(int s){
		switch(s){
			case 0:
				swap(0, 0, 3, 0, 2, 3);
				break;
			case 1:
				swap(0, 6, 2, 6, 1, 0);
				break;
			case 2:
				swap(0, 3, 1, 3, 3, 6);
				break;
			case 3:
				swap(1, 6, 2, 0, 3, 3);
				break;
		}
	}

	private void swap(int f1, int s1, int f2, int s2, int f3, int s3){
		int temp = image[f1][s1];
		image[f1][s1] = image[f2][s2];
		image[f2][s2] = image[f3][s3];
		image[f3][s3] = temp;
	}

	public BufferedImage getScrambleImage(int gap, int pieceSize, Color[] colorScheme) {
		Dimension dim = getImageSize(gap, pieceSize, null);
		BufferedImage buffer = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		drawMinx(buffer.createGraphics(), gap, pieceSize, colorScheme);
		return buffer;
	}

	@Override
	public Dimension getImageSize(int gap, int pieceSize, String variation) {
		return new Dimension(getPyraminxViewWidth(gap, pieceSize), getPyraminxViewHeight(gap, pieceSize));
	}

	private void drawMinx(Graphics2D g, int gap, int pieceSize, Color[] colorScheme){
		drawTriangle(g, 2*gap+3*pieceSize, gap+Math.sqrt(3)*pieceSize, true, image[0], pieceSize, colorScheme);
		drawTriangle(g, 2*gap+3*pieceSize, 2*gap+2*Math.sqrt(3)*pieceSize, false, image[1], pieceSize, colorScheme);
		drawTriangle(g, gap+1.5*pieceSize, gap+Math.sqrt(3)/2*pieceSize, false, image[2], pieceSize, colorScheme);
		drawTriangle(g, 3*gap+4.5*pieceSize, gap+Math.sqrt(3)/2*pieceSize,  false, image[3], pieceSize, colorScheme);
	}

	private void drawTriangle(Graphics2D g, double x, double y, boolean up, int[] state, int pieceSize, Color[] colorScheme){
		GeneralPath p = triangle(up, pieceSize);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		p.transform(AffineTransform.getTranslateInstance(x, y));

		double[] xpoints = new double[3];
		double[] ypoints = new double[3];
		PathIterator iter = p.getPathIterator(null);
		for(int ch = 0; ch < 3; ch++) {
			double[] coords = new double[6];
			int type = iter.currentSegment(coords);
			if(type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
				xpoints[ch] = coords[0];
				ypoints[ch] = coords[1];
			}
			iter.next();
		}

		double[] xs = new double[6];
		double[] ys = new double[6];
		for(int i = 0; i < 3; i++){
			xs[i]=1/3.*xpoints[(i+1)%3]+2/3.*xpoints[i];
			ys[i]=1/3.*ypoints[(i+1)%3]+2/3.*ypoints[i];
			xs[i+3]=2/3.*xpoints[(i+1)%3]+1/3.*xpoints[i];
			ys[i+3]=2/3.*ypoints[(i+1)%3]+1/3.*ypoints[i];
		}

		GeneralPath[] ps = new GeneralPath[9];
		for(int i = 0; i < ps.length; i++){
			ps[i] = new GeneralPath();
		}

		Point2D.Double center = getLineIntersection(xs[0], ys[0], xs[4], ys[4], xs[2], ys[2], xs[3], ys[3]);

		for(int i = 0; i < 3; i++){
			ps[3*i].moveTo(xpoints[i], ypoints[i]);
			ps[3*i].lineTo(xs[i], ys[i]);
			ps[3*i].lineTo(xs[3+(2+i)%3], ys[3+(2+i)%3]);
			ps[3*i].closePath();

			ps[3*i+1].moveTo(xs[i], ys[i]);
			ps[3*i+1].lineTo(xs[3+(i+2)%3], ys[3+(i+2)%3]);
			ps[3*i+1].lineTo(center.x, center.y);
			ps[3*i+1].closePath();

			ps[3*i+2].moveTo(xs[i], ys[i]);
			ps[3*i+2].lineTo(xs[i+3], ys[i+3]);
			ps[3*i+2].lineTo(center.x, center.y);
			ps[3*i+2].closePath();
		}

		for(int i = 0; i < ps.length; i++){
			g.setColor(colorScheme[state[i]]);
			g.fill(ps[i]);
			g.setColor(Color.BLACK);
			g.draw(ps[i]);
		}
	}

	private static GeneralPath triangle(boolean pointup, int pieceSize){
		int rad = (int)(Math.sqrt(3) * pieceSize);
		double[] angs = { 7/6., 11/6., .5 };
		if(pointup) for(int i = 0; i < angs.length; i++) angs[i] += 1/3.;
		for(int i = 0; i < angs.length; i++) angs[i] *= Math.PI;
		double[] x = new double[angs.length];
		double[] y = new double[angs.length];
		for(int i = 0; i < x.length; i++){
			x[i] = rad * Math.cos(angs[i]);
			y[i] = rad * Math.sin(angs[i]);
		}
		GeneralPath p = new GeneralPath();
		p.moveTo(x[0], y[0]);
		for(int ch = 1; ch < x.length; ch++) {
			p.lineTo(x[ch], y[ch]);
		}
		p.closePath();
		return p;
	}

	public static Point2D.Double getLineIntersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4){
		return new Point2D.Double(
			det(det(x1, y1, x2, y2), x1 - x2,
					det(x3, y3, x4, y4), x3 - x4)/
				det(x1 - x2, y1 - y2, x3 - x4, y3 - y4),
			det(det(x1, y1, x2, y2), y1 - y2,
					det(x3, y3, x4, y4), y3 - y4)/
				det(x1 - x2, y1 - y2, x3 - x4, y3 - y4));
	}

	public static double det(double a, double b, double c, double d){
		return a * d - b * c;
	}

	private static int getPyraminxViewWidth(int gap, int pieceSize) {
		return (int)(2 * 3 * pieceSize + 4 * gap);
	}
	private static int getPyraminxViewHeight(int gap, int pieceSize) {
		return (int)(2 * 1.5 * Math.sqrt(3) * pieceSize + 3 * gap);
	}
	public int getNewUnitSize(int width, int height, int gap, String variation) {
		return (int) Math.round(Math.min((width - 4*gap) / (3 * 2),
				(height - 3*gap) / (3 * Math.sqrt(3))));
	}

	public Shape[] getFaces(int gap, int pieceSize, String variation) {
		return new Shape[] {
			getTriangle(2*gap+3*pieceSize, gap+Math.sqrt(3)*pieceSize, pieceSize, true),
			getTriangle(2*gap+3*pieceSize, 2*gap+2*Math.sqrt(3)*pieceSize, pieceSize, false),
			getTriangle(gap+1.5*pieceSize, gap+Math.sqrt(3)/2*pieceSize, pieceSize, false),
			getTriangle(3*gap+4.5*pieceSize, gap+Math.sqrt(3)/2*pieceSize, pieceSize, false)
		};
	}
	private static Shape getTriangle(double x, double y, int pieceSize, boolean up) {
		GeneralPath p = triangle(up, pieceSize);
		p.transform(AffineTransform.getTranslateInstance(x, y));
		return p;
	}
}
