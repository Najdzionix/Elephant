package com.pinktwins.elephant.editor;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.*;
import com.pinktwins.elephant.data.AttachmentInfo2;
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

public class CustomEditor extends RoundPanel implements Editable {

    private static final Logger LOG = Logger.getLogger(CustomEditor.class.getName());

    public static final String ELEM = AbstractDocument.ElementNameAttribute;
    public static final String ICON = StyleConstants.IconElementName;
    public static final String COMP = StyleConstants.ComponentElementName;

    private JTextField title;
    private CustomTextPane customTextPane;
    private HtmlPane htmlPane;
    private BrowserPane browserPane;
    private JPanel padding;
    private UndoManager undoManager = new UndoManager();

    public boolean isRichText, isMarkdown;
    private boolean maybeImporting;

    private int frozenSelectionStart, frozenSelectionEnd;

    final Color kDividerColor = Color.decode("#dbdbdb");

    private AbstractAction increaseFontSizeAction, decreaseFontSizeAction;
    private AbstractAction boldAction, italicAction, underlineAction;
    private AbstractAction strikethroughAction, strikethroughRearrangeAction;

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
            customTextPane.requestFocusInWindow();
        }
    };

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
        registerActions();
    }

    private void registerActions() {
        boldAction = EditorActionFactory.createShiftFontSizeAction("**", this, new StyledEditorKit.BoldAction());
        italicAction = EditorActionFactory.createShiftFontSizeAction("_", this, new StyledEditorKit.ItalicAction());
        underlineAction = EditorActionFactory.createShiftFontSizeAction("<u>", "</u>", this, new StyledEditorKit.UnderlineAction());
        increaseFontSizeAction = EditorActionFactory.createShiftFontSizeAction(1, this);
        decreaseFontSizeAction = EditorActionFactory.createShiftFontSizeAction(-1, this);
        strikethroughAction = EditorActionFactory.createStrikethroughAction(this);
        strikethroughRearrangeAction = EditorActionFactory.createStrikethroughRearrangeAction(this);
    }

    public AttributeSet getDocAttributes(int position) {
        return customTextPane.getCustomDocument().getAttributeSetByPosition(position);
    }

    private void createNote() {

        if (customTextPane != null) {
            remove(customTextPane);
        }

        if (htmlPane != null) {
            remove(htmlPane);
            htmlPane = null;
        }

        if (browserPane != null && browserPane.getParent() != null) {
            remove(browserPane);
            browserPane.clear();
        }

        customTextPane = new CustomTextPane(this);
        customTextPane.setDocument(new CustomDocument());
        customTextPane.addFocusListener(editorFocusListener);
        customTextPane.setFont(ElephantWindow.fontEditor);
        customTextPane.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        customTextPane.setDragEnabled(true);

        // enable AutoIndent as described at http://www.jroller.com/santhosh/entry/autoindent_for_jtextarea
        if (Elephant.settings.getAutoBullet()) {
            customTextPane.registerKeyboardAction(new AutoIndentAction(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        }

        // enable Tab and Shift-Tab behavior for bullet lists
        customTextPane.registerKeyboardAction(new TabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), JComponent.WHEN_FOCUSED);
        customTextPane.registerKeyboardAction(new ShiftTabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK),
                JComponent.WHEN_FOCUSED);

        customTextPane.registerKeyboardAction(new HomeAction(), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), JComponent.WHEN_FOCUSED);

        maybeImporting = false;

        customTextPane.setTransferHandler(attachmentTransferHandler);

        customTextPane.setCaret(new SelectionPreservingCaret());

        customTextPane.addCaretListener(e -> {
            if (eeListener != null) {
                eeListener.caretChanged(customTextPane);
            }
        });

        customTextPane.getDocument().addUndoableEditListener(new UndoEditListener());

        customTextPane.addMouseListener(new AttachmentDragMouseListener(customTextPane) {
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

        InputMap inputMap = customTextPane.getInputMap();

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

        add(customTextPane, BorderLayout.CENTER);

        createPadding();
    }

    public void saveSelection() {
        if (customTextPane != null) {
            frozenSelectionStart = customTextPane.getSelectionStart();
            frozenSelectionEnd = customTextPane.getSelectionEnd();
        }
    }

    public void restoreSelection() {
        if (customTextPane != null && frozenSelectionStart != frozenSelectionEnd) {
            customTextPane.setSelectionStart(frozenSelectionStart);
            customTextPane.setSelectionEnd(frozenSelectionEnd);
            (customTextPane.getCaret()).setSelectionVisible(true);
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
        customTextPane.getStyledDocument().setCharacterAttributes(0, customTextPane.getDocument().getLength(), new SimpleAttributeSet(), true);
        customTextPane.requestFocusInWindow();
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
        customTextPane.setText("");

        isRichText = setTextRtfOrPlain(customTextPane, s);

        customTextPane.setCaretPosition(0);
    }

    public String getTitle() {
        return title.getText();
    }

    @Override
    public void load(Note note) {

    }

    public void reload(Note note) {
        setTitle(note.getMeta().title());
        setText(note.contents());
        setMarkdown(note.isMarkdown());
        loadAttachments(note.getAttachmentList());
        discardUndoBuffer();

        if (note.isMarkdown()) {
            String contents = note.contents();
            String html = MarkdownEditor.markdownToHtml(isRichText() ? Note.plainTextContents(contents) : contents);
            displayHtml(note.file(), html);
        }

        if (note.isHtml()) {
            displayBrowser(note.file());
        }

    }

    private void loadAttachments(List<AttachmentInfo2> info) {
        if (!info.isEmpty()) {
            // We need to insert attachments from end to start - thus, sort.
            Collections.reverse(info);
            for (AttachmentInfo2 ap : info) {

                // If position to insert attachment into would have
                // component content already, it would be overwritten.
                // Make sure there is none.
                AttributeSet as = getDocAttributes(ap.position);
                if (as instanceof AbstractDocument.LeafElement) {
                    AbstractDocument.LeafElement l = (AbstractDocument.LeafElement) as;
                    if (!"content".equals(l.getName())) {
                        getCustomTextPane().insertNewline(ap.position);
                    }
                }
            }
        }
    }


    public String getText() throws BadLocationException {
        Document doc = customTextPane.getDocument();
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
        return customTextPane.hasFocus() || title.hasFocus();
    }

    public void initialFocus() {
        customTextPane.setCaretPosition(0);
        customTextPane.requestFocusInWindow();
    }

    public void focusTitle() {
        title.setCaretPosition(0);
        title.requestFocusInWindow();
    }

    public List<AttachmentInfo> getAttachmentsInfo() {
        List<AttachmentInfo> list = Factory.newArrayList();

        ElementIterator iterator = new ElementIterator(customTextPane.getDocument());
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
                    CustomEditor.this.add(customTextPane, BorderLayout.CENTER);
                    CustomEditor.this.revalidate();

                    htmlPane = null;
                }
            });
        }

        htmlPane.setText(html);

        remove(customTextPane);
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

            remove(customTextPane);
            add(browserPane, BorderLayout.CENTER);
        } catch (MalformedURLException e) {
            LOG.severe("Fail: " + e);
            clear();
        }
    }

    public CustomTextPane getCustomTextPane() {
        return customTextPane;
    }

    public boolean isMarkdown() {
        return isMarkdown;
    }

    public boolean isRichText() {
        return isRichText;
    }

    public void setIsRichText(boolean isRichText) {
        this.isRichText = isRichText;
    }

    public void setMarkdown(boolean b) {
        isMarkdown = b;
    }

    public boolean maybeImporting() {
        return maybeImporting;
    }

    public JTextPane getTextPane() {
        return customTextPane;
    }

}
