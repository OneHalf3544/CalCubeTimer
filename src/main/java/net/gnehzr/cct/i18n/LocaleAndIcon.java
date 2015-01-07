package net.gnehzr.cct.i18n;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Locale;

public class LocaleAndIcon {

	private static final Logger LOG = Logger.getLogger(LocaleAndIcon.class);

	private final File flagFolder;
	private Locale l;
	private String language;
	private ImageIcon flag;
	public LocaleAndIcon(File flagFolder, Locale l, String language) {
		this.flagFolder = flagFolder;
		this.l = l;
		if(language != null)
			this.language = language;
		else
			this.language = l.getDisplayLanguage(l);
	}
	public Locale getLocale() {
		return l;
	}
	public ImageIcon getFlag() {
		if(flag == null) {
			try {
				flag = new ImageIcon(new File(flagFolder, l.getCountry() + ".png").toURI().toURL());
			} catch (MalformedURLException e) {
				LOG.info("unexpected exception", e);
				flag = new ImageIcon();
			}
		}
		return flag;
	}
	public int hashCode() {
		return l.hashCode();
	}
	public boolean equals(Object o) {
		if(o instanceof LocaleAndIcon)
			return l.equals(((LocaleAndIcon) o).l);
		return false;
	}
	public String toString() {
		return language;
	}
}
