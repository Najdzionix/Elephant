package com.pinktwins.elephant.editor.scaler;

import com.pinktwins.elephant.ImageScaler;
import com.pinktwins.elephant.editor.NoteEditor;
import com.pinktwins.elephant.util.SimpleImageInfo;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorWidthImageScaler implements ImageScaler {
    private NoteEditor noteEditor;

    int adjust = (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) ? -20 : -12;

     public EditorWidthImageScaler(NoteEditor noteEditor) {
         this.noteEditor = noteEditor;
     }
    @Override
    public Image scale(Image i, File source) {
        return ScalerService.getScaledImage(noteEditor, i, source, adjust, true);
    }

    @Override
    public Image getCachedScale(File source) {
        return ScalerService.getScaledImageCacheOnly(noteEditor, source, adjust, true);
    }

}