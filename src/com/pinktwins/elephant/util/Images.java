package com.pinktwins.elephant.util;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

public class Images {

	private static final Logger LOG = Logger.getLogger(Images.class.getName());

	public static final String NOTE_TOP_SHADOW = "noteTopShadow";
	public static final String NOTE_TOOLS_DIVIDER = "noteToolsDivider";
	public static final String NOTEBOOKS = "notebooks";
	public static final String ELEPHANT_ICON = "elephantIcon";
	public static final String NOTELIST = "notelist";
	public static final String ALL_NOTES = "allNotes";
	public static final String SIDEBAR = "sidebar";
	public static final String SIDEBAR_DIVIDER = "sidebarDivider";
	public static final String MULTI_SELECTION = "multiSelection";
	public static final String MULTI_SELECTION_TAG_FOCUS = "multiSelectionTagFocus";
	public static final String MOVE_TO_NOTEBOOK = "moveToNotebook";
	public static final String NEW_TAG = "newTag";
	public static final String NOTE_EDITOR = "noteeditor";
	public static final String NOTE_TOOLS_NOTEBOOK = "noteToolsNotebook";
	public static final String NOTE_TOOLS_TRASH = "noteToolsTrash";



	private Images() {
	}

	//TODO remove this
	public static Iterator<Image> iterator(String[] names) {
		ArrayList<Image> list = new ArrayList<Image>();
		for (int n = 0; n < names.length; n++) {
			Image img = null;
			try {
				img = ImageIO.read(Images.class.getClass().getResourceAsStream("/images/" + names[n] + ".png"));
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
			list.add(img);
		}
		return list.iterator();
	}

	public static boolean isImage(File f) {
		if (!f.exists()) {
			return false;
		}

		String s = FilenameUtils.getExtension(f.getName()).toLowerCase();
		return "png tif jpg jpeg bmp gif".indexOf(s) >= 0;
	}

	public static Image loadImage(String name) {
		Image img = null;
		try {
			img = ImageIO.read(Images.class.getClass().getResourceAsStream("/images/" + name + ".png"));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
		return img;
	}
}
