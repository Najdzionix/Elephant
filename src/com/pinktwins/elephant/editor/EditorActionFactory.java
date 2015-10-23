package com.pinktwins.elephant.editor;

import javax.swing.*;
import javax.swing.text.StyledEditorKit;
import java.awt.event.ActionEvent;

/**
 * Created by Kamil Nadłonek on 22.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorActionFactory {


    public static AbstractAction createAction(String markdown, CustomEditor editor, StyledEditorKit.StyledTextAction textAction) {
        return createAction(markdown, markdown, editor, textAction);
    }

    public static AbstractAction createAction(String startMarkdown, String closeMarkdown, CustomEditor editor, StyledEditorKit.StyledTextAction textAction) {
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

    public static AbstractAction createAction(int fontSize, CustomEditor editor) {
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
}
