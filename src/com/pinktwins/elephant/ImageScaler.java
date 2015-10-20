package com.pinktwins.elephant;

import java.awt.*;
import java.io.File;

public interface ImageScaler {
	public Image scale(Image i, File source);

	public Image getCachedScale(File source);
}
