package com.pinktwins.elephant.editor;

/**
 * Created by Kamil Nadłonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public  class EditorController {
    private NoteEditor noteEditor;

    public EditorController(NoteEditor noteEditor) {
        this.noteEditor = noteEditor;
    }

    public void scrollTo(int value) {
        noteEditor.scroll.getVerticalScrollBar().setValue(value);
    }

    public void lockScrolling(boolean value) {
        noteEditor.scroll.setLocked(value);
    }

    public int noteHash() {
        if (noteEditor.getCurrentNote() == null) {
            return 0;
        } else {
            return noteEditor.getCurrentNote().hashCode();
        }
    }
}
