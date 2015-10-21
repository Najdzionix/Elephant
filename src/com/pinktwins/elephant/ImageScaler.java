package com.pinktwins.elephant;

import java.awt.*;
import java.io.File;

public interface ImageScaler {
    Image scale(Image i, File source);

    Image getCachedScale(File source);
}
