package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.panel.CustomScrollPane;

/**
 * Created by Kamil Nadłonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public  class EditorController {
    private NoteEditor noteEditor;
    private CustomScrollPane scroll;

    public EditorController(NoteEditor noteEditor) {
        this.noteEditor = noteEditor;
    }

    public void scrollTo(int value) {
       scroll.getVerticalScrollBar().setValue(value);
    }

    public void lockScrolling(boolean value) {
        scroll.setLocked(value);
    }

    public int noteHash() {
        if (noteEditor.getCurrentNote() == null) {
            return 0;
        } else {
            return noteEditor.getCurrentNote().hashCode();
        }
    }

    public void setScroll(CustomScrollPane scroll) {
        this.scroll = scroll;
    }
}
