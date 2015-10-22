package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.data.Note;

import javax.swing.text.BadLocationException;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public interface Editable {

    /**
     * Load data from Note to Editor.
     * @param note
     */
     void load(Note note);

    /**
     * @return current text from editor
     */
     String getText() throws BadLocationException;

    /**
     * Return note with changes
     */
    Note getNote();
}
