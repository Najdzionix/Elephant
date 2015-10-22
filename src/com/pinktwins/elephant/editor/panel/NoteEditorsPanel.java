package com.pinktwins.elephant.editor.panel;

import com.pinktwins.elephant.ElephantWindow;
import com.pinktwins.elephant.editor.*;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.panel.BackgroundPanel;
import com.pinktwins.elephant.panel.CustomScrollPane;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;
import com.pinktwins.elephant.util.ResizeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
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
    private final int kNoteOffset = 64;
    private final Color lineColor = Color.decode("#b4b4b4");
    private Map<NoteEditorType, Editable> editors;
    //TODO ?
    private final int kBorder = 14;

    private NoteEditor noteEditor;
    private JPanel editorPanel;
    private ScrollablePanel areaHolder;
    private BorderLayout areaHolderLayout;
    private BackgroundPanel scrollHolder;
    private int kMinNoteSize = 288;
    private CustomScrollPane scroll;
    private CustomEditor editor;

    public NoteEditorsPanel(NoteEditor noteEditor) {
        this.noteEditor = noteEditor;
        setLayout(null);
        setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));
        createPanels();
        add(scrollHolder);
        register();
    }

    private void createPanels() {
        editorPanel = new JPanel();
        editorPanel.setLayout(new GridLayout(1, 1));
        editorPanel.setBackground(Color.WHITE);

        editor = new CustomEditor();
        editor.setEditorEventListener(noteEditor);
        editorPanel.add(editor);
        editorPanel.setBounds(kBorder, kBorder, 200, kMinNoteSize);

        // Swing when you're winning part #1.

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
        editorPanel.setLayout(new CardLayout());
        editorPanel.setBackground(Color.WHITE);

        JPanel htmlEditor = new HtmlNoteEditor();
        JPanel oldEditor = new JPanel();
        oldEditor.add(new JLabel("test"));
        editorPanel.add(htmlEditor, "HTML");
        editorPanel.add(oldEditor, "OLD");
        CardLayout cl = (CardLayout) (editorPanel.getLayout());
        cl.show(editorPanel, "OLD");
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
