package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.panel.CustomTextPane;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

/**
 * Created by Kamil Nadłonek on 23.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class SimpleTextEditor {

    public static void shiftFontSize(final int delta, CustomTextPane textPane) {
        StyledEditorKit kit = (StyledEditorKit) textPane.getEditorKit();
        MutableAttributeSet as = kit.getInputAttributes();
        int size = StyleConstants.getFontSize(as);
        StyleConstants.setFontSize(as, size + delta);
        textPane.setCharacterAttributes(as, false);
    }
}
