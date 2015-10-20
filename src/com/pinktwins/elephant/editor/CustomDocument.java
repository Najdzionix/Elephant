package com.pinktwins.elephant.editor;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

/**
 * Created by Kamil Nadonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class CustomDocument extends DefaultStyledDocument {
    private static final long serialVersionUID = 2807153134148093523L;

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        str = str.replaceAll("\t", "    ");
        super.insertString(offs, str, a);
    }
}