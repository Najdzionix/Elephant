package com.pinktwins.elephant.editor.panel;

import com.pinktwins.elephant.ElephantWindow;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.editor.NoteEditor;
import com.pinktwins.elephant.editor.UIComponentFactory;
import com.pinktwins.elephant.panel.BackgroundPanel;
import com.pinktwins.elephant.panel.TagEditorPane;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorToolsPanel extends BackgroundPanel {
    private static final long serialVersionUID = -7285142017724975923L;
    private static final Image BACKGROUND_IMAGE = Images.loadImage(Images.NOTE_EDITOR);
    private static Image NOTE_TOOLS_NOTEBOOK = Images.loadImage(Images.NOTE_TOOLS_NOTEBOOK);
    private static Image NOTE_TOOLS_TRASH = Images.loadImage(Images.NOTE_TOOLS_TRASH);
    private NoteEditor noteEditor;
    private JButton currNotebook;
    private JButton trash;
    private TagEditorPane tagPane;
    private JLabel noteCreated;
    private JLabel noteUpdated;

    public EditorToolsPanel(NoteEditor noteEditor) {
        super(BACKGROUND_IMAGE);
        this.noteEditor = noteEditor;
        createContent();
        registerListeners();
    }

    private void registerListeners() {
        currNotebook.addActionListener(e -> noteEditor.openNotebookChooserForMoving());

        trash.addActionListener(e -> noteEditor.getWindow().deleteSelectedNote());
    }

    private void createContent() {
        setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        setBounds(1, 0, 1920, 65); //todo width ?

        JPanel toolsTop = UIComponentFactory.createBorderPanel();

        JPanel toolsTopLeft = UIComponentFactory.createBorderPanel();
        JPanel toolsTopRight = UIComponentFactory.createBorderPanel();

        currNotebook = UIComponentFactory.createJButton("", NOTE_TOOLS_NOTEBOOK);
        currNotebook.setForeground(ElephantWindow.colorTitleButton);
        currNotebook.setFont(ElephantWindow.fontMediumPlus);

        tagPane = new TagEditorPane();
        tagPane.setEditorEventListener(noteEditor);

        trash = UIComponentFactory.createJButton("", NOTE_TOOLS_TRASH);

        JPanel toolsTopLeftWest = new JPanel(new GridBagLayout());
        toolsTopLeftWest.setOpaque(false);

        toolsTopLeftWest.add(currNotebook);
        toolsTopLeftWest.add(tagPane.getComponent());

        toolsTopLeft.add(toolsTopLeftWest, BorderLayout.WEST);
        toolsTopRight.add(trash, BorderLayout.EAST);
        toolsTop.add(toolsTopLeft, BorderLayout.WEST);
        toolsTop.add(toolsTopRight, BorderLayout.EAST);

        JPanel toolsBot = new JPanel(new FlowLayout(FlowLayout.LEFT));

        noteCreated = UIComponentFactory.createJLabel("Created: xxxxxx", ElephantWindow.fontMedium,
                ElephantWindow.colorTitleButton, BorderFactory.createEmptyBorder(0, 4, 4, 10));

        noteUpdated = UIComponentFactory.createJLabel("Updated: xxxxxx", ElephantWindow.fontMedium,
                ElephantWindow.colorTitleButton, BorderFactory.createEmptyBorder(0, 4, 4, 20));

        toolsBot.add(noteCreated);
        toolsBot.add(noteUpdated);

        add(toolsTop, BorderLayout.NORTH);
        add(toolsBot, BorderLayout.SOUTH);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(BACKGROUND_IMAGE, 14, 32, getWidth() - 28, 2, null);
    }

    public void updateNote(Note note) {
        noteCreated.setText("Created: " + note.createdStr());
        noteUpdated.setText("Updated: " + note.updatedStr());
    }

    public TagEditorPane getTagPane() {
        return tagPane;
    }

    public JButton getTrash() {
        return trash;
    }

    public JButton getCurrNotebook() {
        return currNotebook;
    }
}
