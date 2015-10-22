package com.pinktwins.elephant.editor.scaler;

import com.pinktwins.elephant.ImageScaler;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.io.File;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class EditorWidthImageScaler implements ImageScaler {
    int adjust = (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) ? -20 : -12;

    @Override
    public Image scale(Image i, File source) {
        return null;
//        return getScaledImage(i, source, adjust, true);
    }

    @Override
    public Image getCachedScale(File source) {
        return null;
//        return getScaledImageCacheOnly(source, adjust, true);
    }
}