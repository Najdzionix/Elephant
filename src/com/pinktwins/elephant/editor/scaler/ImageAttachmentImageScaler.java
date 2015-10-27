package com.pinktwins.elephant.editor.scaler;

import com.pinktwins.elephant.ImageScaler;
import com.pinktwins.elephant.editor.NoteEditor;

import java.awt.*;
import java.io.File;

/**
 * Created by Kamil Nadłonek on 27.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class ImageAttachmentImageScaler implements ImageScaler {
    private NoteEditor noteEditor;

    public ImageAttachmentImageScaler(NoteEditor noteEditor) {
        this.noteEditor = noteEditor;
    }

    public Image scale(Image i, File source) {
        return ScalerService.getScaledImage(noteEditor, i, source, 0, false);
    }

    @Override
    public Image getCachedScale(File source) {
        return ScalerService.getScaledImageCacheOnly(noteEditor, source, 0, false);
    }
}
