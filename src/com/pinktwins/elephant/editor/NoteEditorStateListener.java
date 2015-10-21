package com.pinktwins.elephant.editor;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public interface NoteEditorStateListener {
    void stateChange(boolean hasFocus, boolean hasSelection);
}
