package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.model.AttachmentInfo;
import com.pinktwins.elephant.util.Factory;

import javax.swing.text.*;
import java.util.List;

/**
 * Created by Kamil Nadłonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class CustomDocument extends DefaultStyledDocument {
    private static final long serialVersionUID = 2807153134148093523L;

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        str = str.replaceAll("\t", "    ");
        super.insertString(offs, str, a);
    }

    public AttributeSet getAttributeSetByPosition(int position) {
        return getCharacterElement(position).getAttributes();
    }

    public List<AttachmentInfo> getAttachmentsInfo() {
        List<AttachmentInfo> list = Factory.newArrayList();

        ElementIterator iterator = new ElementIterator(this);
        Element element;
        while ((element = iterator.next()) != null) {
            AttributeSet as = element.getAttributes();
            if (as.containsAttribute(CustomEditor.ELEM, CustomEditor.ICON)) {
                AttachmentInfo info = new AttachmentInfo();
                info.setObject(StyleConstants.getIcon(as));
                info.setStartPosition(element.getStartOffset());
                info.setEndPosition(element.getEndOffset());
                list.add(info);
            }

            if (as.containsAttribute(CustomEditor.ELEM, CustomEditor.COMP)) {
                AttachmentInfo info = new AttachmentInfo();
                info.setObject(StyleConstants.getComponent(as));
                info.setStartPosition(element.getStartOffset());
                info.setEndPosition(element.getEndOffset());
                list.add(info);
            }
        }

        return list;
    }
}