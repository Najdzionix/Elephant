package com.pinktwins.elephant;

import com.pinktwins.elephant.model.AttachmentInfo;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

// Notify 'NoteEditor' that editing has gained/lost focus,
// or other editing events happened.

public interface EditorEventListener {
	public void editingFocusGained();

	public void editingFocusLost();

	public void caretChanged(JTextPane text);

	public void filesDropped(List<File> files);

	public void attachmentClicked(MouseEvent event, Object attachmentObject);

	public void attachmentMoved(AttachmentInfo info);
}
