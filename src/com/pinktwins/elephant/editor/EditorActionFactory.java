package com.pinktwins.elephant.editor;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * Created by Kamil Nadłonek on 22.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorActionFactory {
    private static final Logger LOG = Logger.getLogger(EditorActionFactory.class.getName());
    public static final String NEW_LINE = "\n";

    public static AbstractAction createShiftFontSizeAction(String markdown, CustomEditor editor, StyledEditorKit.StyledTextAction textAction) {
        return createShiftFontSizeAction(markdown, markdown, editor, textAction);
    }

    public static AbstractAction createShiftFontSizeAction(String startMarkdown, String closeMarkdown, CustomEditor editor, StyledEditorKit.StyledTextAction textAction) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!editor.isMarkdown()) {
                    editor.setIsRichText(true);
                    textAction.actionPerformed(e);
                } else {
                    MarkdownEditor.markdownStyleCommand(startMarkdown, closeMarkdown, editor.getCustomTextPane());
                }
            }
        };
    }

    public static AbstractAction createShiftFontSizeAction(int fontSize, CustomEditor editor) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (editor.isMarkdown()) {
                    MarkdownEditor.shiftFontSize(fontSize, editor.getCustomTextPane());
                } else {
                    SimpleTextEditor.shiftFontSize(fontSize, editor.getCustomTextPane());
                }
            }
        };
    }

    public static AbstractAction createStrikethroughAction(CustomEditor editor) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!editor.isMarkdown()) {
                    StyledEditorKit kit = (StyledEditorKit) editor.getCustomTextPane().getEditorKit();
                    MutableAttributeSet as = kit.getInputAttributes();
                    boolean b = (!StyleConstants.isStrikeThrough(as));
                    StyleConstants.setStrikeThrough(as, b);
                    editor.getCustomTextPane().setCharacterAttributes(as, false);
                    editor.setIsRichText(true);

                } else {
                    MarkdownEditor.markdownStyleCommand("<strike>", "</strike>", editor.getCustomTextPane());
                }
            }
        };
    }

    // Rearrange lines based on strikethrough. ST lines will 'fall' to bottom of document.
    public static AbstractAction createStrikethroughRearrangeAction(CustomEditor editor) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (editor.isRichText()) {
                    try {
                        Document doc = editor.getCustomTextPane().getDocument();
                        int pos = doc.getLength() - 1;
                        int insertPoint = pos;

                        for (; pos >= 0; pos--) {
                            String s = doc.getText(pos > 0 ? pos - 1 : pos, 1);
                            if (NEW_LINE.equals(s) || pos == 0) {
                                AttributeSet as = editor.getDocAttributes(pos);
                                if (StyleConstants.isStrikeThrough(as)) {
                                    s = doc.getText(pos, doc.getLength() - pos).split(NEW_LINE)[0];
                                    int len = s.length();
                                    doc.insertString(insertPoint, NEW_LINE, null);
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
    }

}
