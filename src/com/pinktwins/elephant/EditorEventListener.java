package com.pinktwins.elephant;

import com.pinktwins.elephant.model.AttachmentInfo;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

// Notify 'NoteEditor' that editing has gained/lost focus,
// or other editing events happened.

public interface EditorEventListener {
	 void editingFocusGained();

	 void editingFocusLost();

	 void caretChanged(JTextPane text);

	 void filesDropped(List<File> files);

	 void attachmentClicked(MouseEvent event, Object attachmentObject);

	 void attachmentMoved(AttachmentInfo info);
}
