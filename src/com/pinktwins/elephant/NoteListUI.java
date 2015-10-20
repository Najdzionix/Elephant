package com.pinktwins.elephant;

import com.pinktwins.elephant.util.Images;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

public class NoteListUI {

	private static Image iAllNotes;

	static {
		Iterator<Image> i = Images.iterator(new String[] { "allNotes" });
		iAllNotes = i.next();
	}

	JScrollPane scroll;
	JPanel main, allNotesPanel, fillerPanel, sep;
	JLabel currentName;
	JButton allNotes;

	public NoteListUI(Container parent) {
		final JPanel title = new JPanel(new BorderLayout());
		title.setBorder(ElephantWindow.emptyBorder);

		allNotes = new JButton("");
		allNotes.setIcon(new ImageIcon(iAllNotes));
		allNotes.setBorderPainted(false);
		allNotes.setContentAreaFilled(false);

		allNotesPanel = new JPanel(new GridLayout(1, 1));
		allNotesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		allNotesPanel.add(allNotes);

		fillerPanel = new JPanel(new GridLayout(1, 1));

		currentName = new JLabel("", JLabel.CENTER);
		currentName.setBorder(BorderFactory.createEmptyBorder(13, 0, 9, 0));
		currentName.setFont(ElephantWindow.fontTitle);
		currentName.setForeground(ElephantWindow.colorTitle);

		sep = new JPanel(null);
		sep.setBounds(0, 0, 1920, 1);
		sep.setBackground(Color.decode("#cccccc"));

		title.add(allNotesPanel, BorderLayout.WEST);
		title.add(currentName, BorderLayout.CENTER);
		title.add(fillerPanel, BorderLayout.EAST);
		title.add(sep, BorderLayout.SOUTH);

		// main notes area
		main = new JPanel();
		main.setLayout(null);

		scroll = new JScrollPane(main);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(ElephantWindow.emptyBorder);
		scroll.getVerticalScrollBar().setUnitIncrement(5);

		parent.add(title, BorderLayout.NORTH);
		parent.add(scroll, BorderLayout.CENTER);
	}
}
