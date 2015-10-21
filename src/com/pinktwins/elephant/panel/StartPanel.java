package com.pinktwins.elephant.panel;

import com.pinktwins.elephant.ElephantWindow;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.util.IOUtil;
import com.pinktwins.elephant.util.Images;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class StartPanel extends BackgroundPanel {

	private static final Logger LOG = Logger.getLogger(StartPanel.class.getName());

	static Image tile = Images.loadImage(Images.NOTEBOOKS);

	public StartPanel(final Runnable runWhenLocationSet) {
		super(tile);

		setLayout(new FlowLayout());

		JPanel main = new JPanel(new GridLayout(3, 1));
		main.setBorder(BorderFactory.createEmptyBorder(200, 0, 0, 0));

		JLabel welcome = new JLabel("Please choose your note location.", JLabel.CENTER);
		welcome.setForeground(Color.DARK_GRAY);
		welcome.setFont(ElephantWindow.fontStart);
		welcome.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		JButton bLocation = new JButton("Choose folder");

		JLabel hint = new JLabel("Folder 'Elephant' will be created under this folder.", JLabel.CENTER);
		hint.setForeground(Color.DARK_GRAY);
		hint.setFont(ElephantWindow.fontStart);
		hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		main.add(welcome);
		main.add(bLocation);
		main.add(hint);

		add(main);

		bLocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JFileChooser ch = new JFileChooser();
				ch.setCurrentDirectory(new File(System.getProperty("user.home")));
				ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				ch.setMultiSelectionEnabled(false);

				int res = ch.showOpenDialog(StartPanel.this);
				if (res == JFileChooser.APPROVE_OPTION) {
					File f = ch.getSelectedFile();
					if (f.exists()) {
						File folder = new File(f + File.separator + "Elephant");
						if (folder.exists() || folder.mkdirs()) {

							Vault.getInstance().setLocation(folder.getAbsolutePath());

							File inbox = new File(folder + File.separator + "Inbox");
							if (inbox.mkdirs()) {

								addBuiltInNote(inbox + File.separator + "Shortcuts.txt", "Tip #2 - Shortcuts", Note.getResourceNote("shortcuts.txt"));
								addBuiltInNote(inbox + File.separator + "Welcome.txt", "Welcome!", Note.getResourceNote("welcome.txt"));
								addBuiltInNote(inbox + File.separator + "Markdown.md", "Tip #1 - Markdown", Note.getResourceNote("markdown.md"));
								addBuiltInNote(inbox + File.separator + "html_example.html", "HTML Example", Note.getResourceNote("html_example.html"));

								File shortcuts = new File(folder.getAbsolutePath() + File.separator + ".shortcuts");
								try {
									IOUtil.writeFile(shortcuts, "{\"list\": [\"Inbox\", \"Inbox/Welcome.txt\", \"search:Tip\", \"search:tag:Today\"]}");
								} catch (IOException e) {
									LOG.severe("Fail: " + e);
								}
							}

							Vault.getInstance().populate();
							runWhenLocationSet.run();
						}
					}
				}
			}
		});
	}

	private void addBuiltInNote(String filePath, String title, String contents) {
		File note = new File(filePath);
		Note n = new Note(note);
		n.getMeta().title(title);
		try {
			IOUtil.writeFile(note, contents);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}
}
