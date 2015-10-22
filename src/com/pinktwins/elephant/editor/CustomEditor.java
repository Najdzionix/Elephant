package com.pinktwins.elephant.editor;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.*;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.eventbus.StyleCommandEvent;
import com.pinktwins.elephant.eventbus.UndoRedoStateUpdateRequest;
import com.pinktwins.elephant.model.AttachmentInfo;
import com.pinktwins.elephant.panel.BrowserPane;
import com.pinktwins.elephant.panel.BrowserPane.BrowserEventListener;
import com.pinktwins.elephant.panel.CustomTextPane;
import com.pinktwins.elephant.panel.HtmlPane;
import com.pinktwins.elephant.panel.RoundPanel;
import com.pinktwins.elephant.ui.AutoIndentAction;
import com.pinktwins.elephant.ui.HomeAction;
import com.pinktwins.elephant.ui.ShiftTabAction;
import com.pinktwins.elephant.ui.TabAction;
import com.pinktwins.elephant.util.*;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class CustomEditor extends RoundPanel implements Editable{

    private static final Logger LOG = Logger.getLogger(CustomEditor.class.getName());

    public static final String ELEM = AbstractDocument.ElementNameAttribute;
    public static final String ICON = StyleConstants.IconElementName;
    public static final String COMP = StyleConstants.ComponentElementName;

    private JTextField title;
    private CustomTextPane note;
    private HtmlPane htmlPane;
    private BrowserPane browserPane;
    private JPanel padding;
    private UndoManager undoManager = new UndoManager();

    public boolean isRichText, isMarkdown;
    private boolean maybeImporting;

    private int frozenSelectionStart, frozenSelectionEnd;

    final Color kDividerColor = Color.decode("#dbdbdb");

    private final FocusListener editorFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            if (eeListener != null) {
                eeListener.editingFocusGained();
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (eeListener != null) {
                eeListener.editingFocusLost();
            }
        }
    };

    private final AbstractAction boldAction = new AbstractAction() {
        StyledEditorKit.BoldAction a = new StyledEditorKit.BoldAction();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isMarkdown) {
                isRichText = true;
                a.actionPerformed(e);
            } else {
                markdownStyleCommand("**", "**");
            }
        }
    };

    private final AbstractAction italicAction = new AbstractAction() {
        StyledEditorKit.ItalicAction a = new StyledEditorKit.ItalicAction();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isMarkdown) {
                isRichText = true;
                a.actionPerformed(e);
            } else {
                markdownStyleCommand("_", "_");
            }
        }
    };

    private final AbstractAction underlineAction = new AbstractAction() {
        StyledEditorKit.UnderlineAction a = new StyledEditorKit.UnderlineAction();

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isMarkdown) {
                isRichText = true;
                a.actionPerformed(e);
            } else {
                markdownStyleCommand("<u>", "</u>");
            }
        }
    };

    private final AbstractAction strikethroughAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isMarkdown) {
                StyledEditorKit kit = (StyledEditorKit) note.getEditorKit();
                MutableAttributeSet as = kit.getInputAttributes();
                boolean b = (StyleConstants.isStrikeThrough(as)) ? false : true;
                StyleConstants.setStrikeThrough(as, b);
                note.setCharacterAttributes(as, false);

                isRichText = true;
            } else {
                markdownStyleCommand("<strike>", "</strike>");
            }
        }
    };

    // Rearrange lines based on strikethrough. ST lines will 'fall' to bottom of document.
    private final AbstractAction strikethroughRearrangeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (isRichText) {
                try {
                    Document doc = note.getDocument();
                    int pos = doc.getLength() - 1;
                    int insertPoint = pos;

                    for (; pos >= 0; pos--) {
                        String s = doc.getText(pos > 0 ? pos - 1 : pos, 1);
                        if ("\n".equals(s) || pos == 0) {
                            AttributeSet as = getAttributes(pos);
                            if (StyleConstants.isStrikeThrough(as)) {
                                s = doc.getText(pos, doc.getLength() - pos).split("\n")[0];
                                int len = s.length();
                                doc.insertString(insertPoint, "\n", null);
                                doc.insertString(insertPoint, s, as);
                                doc.remove(pos > 0 ? pos - 1 : pos, len + 1);
                                insertPoint -= len + 1;
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    LOG.severe("Fail: " + e);
                }
            }
        }
    };

    private final AbstractAction increaseFontSizeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            shiftFontSize(1);
        }
    };

    private final AbstractAction decreaseFontSizeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            shiftFontSize(-1);
        }
    };

    private EditorEventListener eeListener;
    private NoteAttachmentTransferHandler attachmentTransferHandler;

    class NoteAttachmentTransferHandler extends AttachmentTransferHandler {
        public NoteAttachmentTransferHandler(EditorEventListener listener) {
            super(listener);
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            if (isShowingMarkdown()) {
                return false;
            }

            maybeImporting = true;
            return true;
        }
    }

    private final CustomMouseListener paddingClick = new CustomMouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
            note.requestFocusInWindow();
        }
    };

    public void setMarkdown(boolean b) {
        isMarkdown = b;
    }

    public boolean maybeImporting() {
        return maybeImporting;
    }

    public JTextPane getTextPane() {
        return note;
    }

    public void setEditorEventListener(EditorEventListener l) {
        eeListener = l;
        attachmentTransferHandler = new NoteAttachmentTransferHandler(eeListener);
    }

    @SuppressWarnings("serial")
    public CustomEditor() {
        super();

        Elephant.eventBus.register(this);

        this.setDoubleBuffered(true);

        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.setBackground(kDividerColor);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));

        title = new JTextField();
        title.setFont(ElephantWindow.fontEditorTitle);
        title.setBorder(BorderFactory.createEmptyBorder(2, 0, 12, 0));
        title.addFocusListener(editorFocusListener);
        TextComponentUtil.insertListenerForHintText(title, "Untitled");

        final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        title.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    manager.focusNextComponent();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        titlePanel.add(title, BorderLayout.CENTER);

        title.setText("");
//        add(new HtmlNoteEditor(), BorderLayout.NORTH);
		add(titlePanel, BorderLayout.NORTH);

        createNote();

        // resize padding so note is at least kMinNoteSize height
        addComponentListener(new ResizeListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                int h = getHeight();
                int needed = NoteEditor.kMinNoteSize - h;
                int preferred = h + needed - (padding.getLocation().y + 12);

                if (needed > 0) {
                    if (preferred < 0) {
                        createPadding();
                        preferred = 10;
                    }
                    revalidate();
                } else {
                    padding.setVisible(false);

                    if (preferred > 0) {
                        createPadding();
                    }
                }

                if (preferred > 0) {
                    padding.setVisible(true);
                    padding.setPreferredSize(new Dimension(10, preferred));
                } else {
                    padding.setVisible(false);
                }
            }
        });
    }

    void insertNewline(int position) {
        try {
            note.getDocument().insertString(position, "\n", null);
        } catch (BadLocationException e) {
            LOG.severe("Fail: " + e);
        }
    }

    public AttributeSet getAttributes(int position) {
        return ((CustomDocument) note.getDocument()).getCharacterElement(position).getAttributes();
    }

    private void createNote() {

        if (note != null) {
            remove(note);
        }

        if (htmlPane != null) {
            remove(htmlPane);
            htmlPane = null;
        }

        if (browserPane != null && browserPane.getParent() != null) {
            remove(browserPane);
            browserPane.clear();
        }

        note = new CustomTextPane(this);
        note.setDocument(new CustomDocument());
        note.addFocusListener(editorFocusListener);
        note.setFont(ElephantWindow.fontEditor);
        note.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        note.setDragEnabled(true);

        // enable AutoIndent as described at http://www.jroller.com/santhosh/entry/autoindent_for_jtextarea
        if (Elephant.settings.getAutoBullet()) {
            note.registerKeyboardAction(new AutoIndentAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        }

        // enable Tab and Shift-Tab behavior for bullet lists
        note.registerKeyboardAction(new TabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), JComponent.WHEN_FOCUSED);
        note.registerKeyboardAction(new ShiftTabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                JComponent.WHEN_FOCUSED);

        note.registerKeyboardAction(new HomeAction(), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), JComponent.WHEN_FOCUSED);

        maybeImporting = false;

        note.setTransferHandler(attachmentTransferHandler);

        note.setCaret(new SelectionPreservingCaret());

        note.addCaretListener(e -> {
            if (eeListener != null) {
                eeListener.caretChanged(note);
            }
        });

        note.getDocument().addUndoableEditListener(new UndoEditListener());

        note.addMouseListener(new AttachmentDragMouseListener(this, note) {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (eeListener != null && attachmentObject != null) {
                    eeListener.attachmentClicked(event, attachmentObject);
                }
            }

            @Override
            public void attachmentMoved(AttachmentInfo info) {
                if (eeListener != null) {
                    eeListener.attachmentMoved(info);
                }
            }
        });

        InputMap inputMap = note.getInputMap();

        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, ElephantWindow.menuMask);
        inputMap.put(ks, boldAction);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_I, ElephantWindow.menuMask);
        inputMap.put(ks, italicAction);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_U, ElephantWindow.menuMask);
        inputMap.put(ks, underlineAction);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_K, ElephantWindow.menuMask | KeyEvent.CTRL_DOWN_MASK);
        inputMap.put(ks, strikethroughAction);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_K, ElephantWindow.menuMask | KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        inputMap.put(ks, strikethroughRearrangeAction);

        ks = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ElephantWindow.menuMask);
        inputMap.put(ks, increaseFontSizeAction);
        ks = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ElephantWindow.menuMask);
        inputMap.put(ks, decreaseFontSizeAction);

        add(note, BorderLayout.CENTER);

        createPadding();
    }

    public void saveSelection() {
        if (note != null) {
            frozenSelectionStart = note.getSelectionStart();
            frozenSelectionEnd = note.getSelectionEnd();
        }
    }

    public void restoreSelection() {
        if (note != null && frozenSelectionStart != frozenSelectionEnd) {
            note.setSelectionStart(frozenSelectionStart);
            note.setSelectionEnd(frozenSelectionEnd);
            ((DefaultCaret) note.getCaret()).setSelectionVisible(true);
        }
    }

    private void markdownStyleCommand(String codeStart, String codeEnd) {
        int lenStart = codeStart.length();
        int lenEnd = codeEnd.length();

        if (note.getSelectionStart() == note.getSelectionEnd()) {
            try {
                note.getDocument().insertString(note.getCaretPosition(), codeStart + codeEnd, null);
                note.setCaretPosition(note.getCaretPosition() - lenEnd);
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        } else {
            try {
                int codeEnding = Math.max(note.getSelectionStart() + lenStart, note.getSelectionEnd());
                boolean codeCouldFit = codeEnding < note.getDocument().getLength();

                if (codeCouldFit && note.getText(note.getSelectionStart(), lenStart).equals(codeStart)
                        && note.getText(note.getSelectionEnd() - lenEnd, lenEnd).equals(codeEnd)) {
                    note.getDocument().remove(note.getSelectionEnd() - lenEnd, lenEnd);
                    note.getDocument().remove(note.getSelectionStart(), lenStart);
                } else {
                    note.getDocument().insertString(note.getSelectionEnd(), codeEnd, null);
                    note.getDocument().insertString(note.getSelectionStart(), codeStart, null);
                    note.setSelectionStart(note.getSelectionStart() - lenStart);
                }
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        }
    }

    private void shiftFontSize(final int delta) {
        if (!isMarkdown) {
            StyledEditorKit kit = (StyledEditorKit) note.getEditorKit();
            MutableAttributeSet as = kit.getInputAttributes();
            int size = StyleConstants.getFontSize(as);
            StyleConstants.setFontSize(as, size + delta);
            note.setCharacterAttributes(as, false);

            isRichText = true;
        } else {
            try {
                String s = note.getText(0, note.getCaretPosition());
                int lastLf = s.lastIndexOf("\n");
                if (lastLf == -1) {
                    lastLf = 0;
                } else {
                    lastLf++;
                }
                if (delta > 0) {
                    note.getDocument().insertString(lastLf, "#", null);
                } else {
                    System.out.println(lastLf);
                    if (s.length() > lastLf && s.charAt(lastLf) == '#') {
                        note.getDocument().remove(lastLf, 1);
                    }
                }
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        }
    }

    @Subscribe
    public void handleStyleCommandEvent(StyleCommandEvent e) {
        String cmd = e.event.getActionCommand();
        if ("Bold".equals(cmd)) {
            boldAction.actionPerformed(e.event);
            return;
        }
        if ("Italic".equals(cmd)) {
            italicAction.actionPerformed(e.event);
            return;
        }
        if ("Underline".equals(cmd)) {
            underlineAction.actionPerformed(e.event);
            return;
        }
        if ("Strikethrough".equals(cmd)) {
            strikethroughAction.actionPerformed(e.event);
            return;
        }
        if ("Bigger".equals(cmd)) {
            increaseFontSizeAction.actionPerformed(e.event);
            return;
        }
        if ("Smaller".equals(cmd)) {
            decreaseFontSizeAction.actionPerformed(e.event);
            return;
        }
        if ("Make Plain Text".equals(cmd)) {
            // handled directly from ElephantWindow -> noteEditor
        }
    }

    public void turnToPlainText() {
        note.getStyledDocument().setCharacterAttributes(0, note.getDocument().getLength(), new SimpleAttributeSet(), true);
        note.requestFocusInWindow();
        isRichText = false;
    }

    private void createPadding() {
        if (padding != null) {
            padding.setVisible(false);
            remove(padding);
        }

        padding = new JPanel(null);
        padding.setBackground(Color.WHITE);
        padding.addMouseListener(paddingClick);
        padding.setPreferredSize(new Dimension(0, 0));
        add(padding, BorderLayout.SOUTH);
    }

    public void setTitle(String s) {
        title.setText(s);
        title.setCaretPosition(0);
        title.setSelectionEnd(0);
    }

    public static boolean setTextRtfOrPlain(JTextPane textPane, String s) {
        boolean rich = false;

        if (s != null && s.length() > 0) {
            if (s.indexOf("{\\rtf") == 0) {
                try {
                    RtfUtil.putRtf(textPane.getDocument(), s, 0);
                    if (textPane.getDocument().getLength() == 0) {
                        textPane.setText(s);
                    } else {
                        rich = true;
                    }
                } catch (IOException e) {
                    LOG.severe("Fail: " + e);
                    textPane.setText(s);
                } catch (BadLocationException e) {
                    LOG.severe("Fail: " + e);
                    textPane.setText(s);
                }
            } else {
                textPane.setText(s);
            }
        }

        return rich;
    }

    public void setText(String s) {
        note.setText("");

        isRichText = setTextRtfOrPlain(note, s);

        note.setCaretPosition(0);
    }

    public String getTitle() {
        return title.getText();
    }

    @Override
    public void load(Note note) {

    }

    public String getText() throws BadLocationException {
        Document doc = note.getDocument();
        String plain = doc.getText(0, doc.getLength());
        String rtf = RtfUtil.getRtf(doc);

        return rtf != null && rtf.length() > 0 && isRichText ? rtf : plain;
    }

    @Override
    public Note getNote() {
        return null;
    }

    public void clear() {
        setTitle("");

        // replace JTextPane with new instance to get rid of old styles.
        createNote();
        discardUndoBuffer();
    }

    public boolean hasFocus() {
        return note.hasFocus() || title.hasFocus();
    }

    public void initialFocus() {
        note.setCaretPosition(0);
        note.requestFocusInWindow();
    }

    public void focusTitle() {
        title.setCaretPosition(0);
        title.requestFocusInWindow();
    }

    public List<AttachmentInfo> getAttachmentInfo() {
        List<AttachmentInfo> list = Factory.newArrayList();

        ElementIterator iterator = new ElementIterator(note.getDocument());
        Element element;
        while ((element = iterator.next()) != null) {
            AttributeSet as = element.getAttributes();
            if (as.containsAttribute(ELEM, ICON)) {
                AttachmentInfo info = new AttachmentInfo();
                info.setObject(StyleConstants.getIcon(as));
                info.setStartPosition(element.getStartOffset());
                info.setEndPosition(element.getEndOffset());
                list.add(info);
            }

            if (as.containsAttribute(ELEM, COMP)) {
                AttachmentInfo info = new AttachmentInfo();
                info.setObject(StyleConstants.getComponent(as));
                info.setStartPosition(element.getStartOffset());
                info.setEndPosition(element.getEndOffset());
                list.add(info);
            }
        }

        return list;
    }

    // remove icon/file elements from document.
    // Returns list with correct element positions
    // after removal for possible reimport.

    public List<AttachmentInfo> removeAttachmentElements(List<AttachmentInfo> info) {
        List<AttachmentInfo> info_reverse = new ArrayList<AttachmentInfo>(info);

        Collections.reverse(info_reverse);
        for (AttachmentInfo i : info_reverse) {
            int tagLen = i.getEndPosition() - i.getStartPosition();
            if (tagLen < 5) { // might be unneccessary safety
                try {
                    getTextPane().getDocument().remove(i.getStartPosition(), tagLen);

                    // Correct attachment position in the document
                    // WITHOUT
                    // any attachment element markers:
                    int n = info.indexOf(i);
                    i.setStartPosition(i.getStartPosition() - n);
                    i.setEndPosition(i.getEndPosition() - n);
                } catch (BadLocationException e) {
                    LOG.severe("Fail: " + e);
                }
            }
        }

        return info_reverse;
    }

    protected class UndoEditListener implements UndoableEditListener {
        public void undoableEditHappened(UndoableEditEvent e) {
            // Remember the edit and update the menus
            undoManager.addEdit(e.getEdit());
            new UndoRedoStateUpdateRequest(undoManager).post();
        }
    }

    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
        new UndoRedoStateUpdateRequest(undoManager).post();
    }

    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
        new UndoRedoStateUpdateRequest(undoManager).post();
    }

    public void discardUndoBuffer() {
        undoManager.discardAllEdits();
        new UndoRedoStateUpdateRequest(undoManager).post();
    }

    public void displayHtml(final File noteFile, final String html) {
        if (htmlPane == null) {
            htmlPane = new HtmlPane(noteFile, new Runnable() {
                @Override
                public void run() {
                    // Executed when mouseClick does not open a link
                    // -> go to edit mode
                    CustomEditor.this.remove(htmlPane);
                    CustomEditor.this.add(note, BorderLayout.CENTER);
                    CustomEditor.this.revalidate();

                    htmlPane = null;
                }
            });
        }

        htmlPane.setText(html);

        remove(note);
        add(htmlPane, BorderLayout.CENTER);
    }

    public boolean isShowingMarkdown() {
        return htmlPane != null;
    }

    public void displayBrowser(final File noteFile) {
        if (browserPane == null) {
            browserPane = new BrowserPane();
            browserPane.setBrowserEventListener(new BrowserEventListener() {
                @Override
                public void mouseWheelEvent(MouseWheelEvent e) {
                    Container c = CustomEditor.this.getParent();
                    c.dispatchEvent(e);
                }
            });
        }

        try {
            URL url = noteFile.toURI().toURL();
            browserPane.loadURL(url.toExternalForm());

            remove(note);
            add(browserPane, BorderLayout.CENTER);
        } catch (MalformedURLException e) {
            LOG.severe("Fail: " + e);
            clear();
        }
    }
}
