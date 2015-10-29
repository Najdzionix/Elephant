package com.pinktwins.elephant.editor.panel;

import com.pinktwins.elephant.ElephantWindow;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.editor.*;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.panel.BackgroundPanel;
import com.pinktwins.elephant.panel.CustomScrollPane;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class NoteEditorsPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(NoteEditorsPanel.class.getName());
    private static Image NOTE_TOP_SHADOW = Images.loadImage(Images.NOTE_TOP_SHADOW);
    private static final int kNoteOffset = 64;
    private static final int kMinNoteSize = 288;
    private static final int kBorder = 14;
    private static final Color lineColor = Color.decode("#b4b4b4");

    private Map<NoteEditorType, Editable> editors;
    private NoteEditor noteEditor;
    private JPanel editorPanel;
    private ScrollablePanel areaHolder;
    private BorderLayout areaHolderLayout;
    private BackgroundPanel scrollHolder;
    private CustomScrollPane scroll;
    private CustomEditor editor;
    private HtmlNoteEditor htmlEditor;
    private NoteEditorType activeEditorType;

    public NoteEditorsPanel(NoteEditor noteEditor) {
        this.noteEditor = noteEditor;
        setLayout(null);
        setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));
        createPanels();
        add(scrollHolder);
        register();
    }

    private void createPanels() {
        loadEditors();
        editorPanel.setBounds(kBorder, kBorder, 200, kMinNoteSize);

        final int topBorderOffset = 2;
        areaHolderLayout = new BorderLayout();
        areaHolder = new ScrollablePanel();
        areaHolder.setLayout(areaHolderLayout);
        areaHolder.setBorder(BorderFactory.createEmptyBorder(kBorder - topBorderOffset, kBorder - 1, kBorder, kBorder));
        areaHolder.add(editorPanel, BorderLayout.NORTH);

        scrollHolder = new BackgroundPanel();
        scrollHolder.setOpaque(false);

        scroll = new CustomScrollPane(areaHolder);
        scroll.setOpaque(false);
        scroll.setBorder(ElephantWindow.emptyBorder);
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        scroll.getHorizontalScrollBar().setUnitIncrement(10);

        scrollHolder.add(scroll, BorderLayout.CENTER);
    }

    private void register() {
        scroll.addMouseListener(new CustomMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                noteEditor.unfocus();

                // If we switch out from markdown-editing to rich display,
                // the unfocus happens too late to actually save edits.
                // This UIEvent marks a savepoint.
                new UIEvent(UIEvent.Kind.editorWillChangeNote).post();

                if (noteEditor.getCurrentNote().isMarkdown()) {
                    noteEditor.getWindow().showNote(noteEditor.getCurrentNote());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        JScrollBar v = scroll.getVerticalScrollBar();
        if (!v.isVisible()) {
            g.drawImage(NOTE_TOP_SHADOW, 0, kNoteOffset + 1, getWidth(), 4, null);
        } else {
            if (v.getValue() < 4) {
                int adjust = scroll.isLocked() ? 0 : kBorder + 1;
                g.drawImage(NOTE_TOP_SHADOW, 0, kNoteOffset + 1, getWidth() - adjust, 4, null);
            } else {
                g.drawImage(NOTE_TOP_SHADOW, 0, kNoteOffset + 1, getWidth(), 2, null);
            }
        }

        g.setColor(lineColor);
        g.drawLine(0, 0, 0, getHeight());
    }

    private void loadEditors() {
        editors = new HashMap<>();
//        editors.put(NoteEditorType.HTML, new HtmlNoteEditor());
//        editors.put(NoteEditorType.OLD, new CustomEditor());
        //todo rest editor load
        editorPanel = new JPanel();
        editorPanel.setLayout(new BorderLayout());
        editorPanel.setBackground(Color.WHITE);

        CustomEditor oldEditor = new CustomEditor();
        editor = oldEditor;
        oldEditor.setEditorEventListener(noteEditor);
//        showEditor(NoteEditorType.HTML);

    }

    public void showEditor(Note note) {
//        CardLayout cl = (CardLayout) (editorPanel.getLayout());
        editorPanel.removeAll();
        activeEditorType = note.getNoteType();
        if (note.getNoteType() == NoteEditorType.HTML) {
            htmlEditor = new HtmlNoteEditor(note);
           editorPanel.add(htmlEditor, BorderLayout.CENTER);
        } else {
            editorPanel.add(editor, BorderLayout.CENTER);
            editor.reload(note);
        }

    }

    public String getNoteFromEditor() {
        if(activeEditorType == NoteEditorType.HTML) {
            System.out.println(htmlEditor.getText());
            return  htmlEditor.getText();
        }
        return "";
    }

    public JPanel getEditorPanel() {
        return editorPanel;
    }

    public BackgroundPanel getScrollHolder() {
        return scrollHolder;
    }

    public ScrollablePanel getAreaHolder() {
        return areaHolder;
    }

    public CustomScrollPane getScroll() {
        return scroll;
    }

    public CustomEditor getEditor() {
        return editor;
    }
}
