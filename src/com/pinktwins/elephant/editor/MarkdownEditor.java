package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.panel.CustomTextPane;

import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import java.util.logging.Logger;

/**
 * Created by Kamil Nadłonek on 22.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class MarkdownEditor {
    private static final Logger LOG = Logger.getLogger(MarkdownEditor.class.getName());

    public static void markdownStyleCommand(String codeStart, String codeEnd,CustomTextPane textPane) {
        int lenStart = codeStart.length();
        int lenEnd = codeEnd.length();

        if (textPane.getSelectionStart() == textPane.getSelectionEnd()) {
            try {
                textPane.getDocument().insertString(textPane.getCaretPosition(), codeStart + codeEnd, null);
                textPane.setCaretPosition(textPane.getCaretPosition() - lenEnd);
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        } else {
            try {
                int codeEnding = Math.max(textPane.getSelectionStart() + lenStart, textPane.getSelectionEnd());
                boolean codeCouldFit = codeEnding < textPane.getDocument().getLength();

                if (codeCouldFit && textPane.getText(textPane.getSelectionStart(), lenStart).equals(codeStart)
                        && textPane.getText(textPane.getSelectionEnd() - lenEnd, lenEnd).equals(codeEnd)) {
                    textPane.getDocument().remove(textPane.getSelectionEnd() - lenEnd, lenEnd);
                    textPane.getDocument().remove(textPane.getSelectionStart(), lenStart);
                } else {
                    textPane.getDocument().insertString(textPane.getSelectionEnd(), codeEnd, null);
                    textPane.getDocument().insertString(textPane.getSelectionStart(), codeStart, null);
                    textPane.setSelectionStart(textPane.getSelectionStart() - lenStart);
                }
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        }
    }
     //TODO does not work
    public static void shiftFontSize(final int delta, CustomTextPane textPane) {
        try {
            String s = textPane.getText(0, textPane.getCaretPosition());
            int lastLf = s.lastIndexOf("\n");
            if (lastLf == -1) {
                lastLf = 0;
            } else {
                lastLf++;
            }
            if (delta > 0) {
                textPane.getDocument().insertString(lastLf, "#", null);
            } else {
                LOG.info(String.valueOf(lastLf)); //TODO change log level to debug
                if (s.length() > lastLf && s.charAt(lastLf) == '#') {
                    textPane.getDocument().remove(lastLf, 1);
                }
            }
        } catch (BadLocationException e) {
            LOG.severe("Fail: " + e);
        }

    }
}
