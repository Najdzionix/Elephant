package com.pinktwins.elephant.editor.scaler;

import com.pinktwins.elephant.ImageScalingCache;
import com.pinktwins.elephant.editor.NoteEditor;
import com.pinktwins.elephant.util.SimpleImageInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by Kamil Nadłonek on 27.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class ScalerService {
    private static final Logger LOG = Logger.getLogger(ScalerService.class.getName());
    public static final ImageScalingCache scalingCache = new ImageScalingCache();

    public static Image getScaledImage(NoteEditor noteEditor, Image i, File sourceFile, int widthOffset, boolean useFullWidth) {
        long w = noteEditor.getUsableEditorWidth() + widthOffset;
        long iw = i.getWidth(null);

        if (useFullWidth || iw > w) {
            float f = w / (float) iw;
            int scaledWidth = (int) (f * (float) iw);
            int scaledHeight = (int) (f * (float) i.getHeight(null));

            Image cached = scalingCache.get(sourceFile, scaledWidth, scaledHeight);
            if (cached != null) {
                return cached;
            }

            Image img = i.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING);
            scalingCache.put(sourceFile, scaledWidth, scaledHeight, img);

            return img;
        } else {
            return i;
        }
    }

    public static Image getScaledImageCacheOnly(NoteEditor noteEditor, File sourceFile, int widthOffset, boolean useFullWidth) {
        SimpleImageInfo info;
        try {
            info = new SimpleImageInfo(sourceFile);
        } catch (IOException e) {
            LOG.severe("Fail: " + e);
            return null;
        }

        long w = noteEditor.getUsableEditorWidth() + widthOffset;
        long iw = info.getWidth();

        if (useFullWidth || iw > w) {
            float f = w / (float) iw;
            int scaledWidth = (int) (f * (float) iw);
            int scaledHeight = (int) (f * (float) info.getHeight());

            return scalingCache.get(sourceFile, scaledWidth, scaledHeight);
        }

        return null;
    }

    public static ImageScalingCache getScalingCache() {
        return scalingCache;
    }
}
