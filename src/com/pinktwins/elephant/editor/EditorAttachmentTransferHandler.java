package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.AttachmentTransferHandler;
import com.pinktwins.elephant.EditorEventListener;

import javax.swing.*;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorAttachmentTransferHandler  extends AttachmentTransferHandler {
   //TODO now only support customEditor it should more generic type
    private CustomEditor editor;

    public EditorAttachmentTransferHandler(EditorEventListener listener, CustomEditor noteEditor) {
        super(listener);
        editor = noteEditor;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return !editor.isShowingMarkdown();

    }
}
