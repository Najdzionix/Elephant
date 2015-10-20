package com.pinktwins.elephant.panel;

import com.pinktwins.elephant.HtmlPaneMouseListener;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class HtmlPane extends JTextPane {

	private static final Logger LOG = Logger.getLogger(HtmlPane.class.getName());

	public HtmlPane(File noteFile, Runnable onTerminalClick) {
		super();

		setFocusable(false);
		setEditable(false);
		setContentType("text/html");

		String base = noteFile.getAbsolutePath() + ".attachments" + File.separator;

		try {
			URL baseUrl = new File(base).toURI().toURL();

			Document doc = getDocument();
			HTMLDocument d;
			if (doc instanceof HTMLDocument) {
				d = (HTMLDocument) doc;
				d.setBase(baseUrl);
				// hint by http://stackoverflow.com/a/19785465/873282
				this.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
			}
		} catch (MalformedURLException e) {
			LOG.severe("Fail: " + e);
		}

		addMouseListener(new HtmlPaneMouseListener(this, base, onTerminalClick));
	}
}
