package com.pinktwins.elephant.editor.panel;

import com.pinktwins.elephant.panel.BackgroundPanel;
import com.pinktwins.elephant.util.Images;

import java.awt.*;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class DividedPanel extends BackgroundPanel {
    private static final long serialVersionUID = -7285142017724975923L;
    private static Image noteToolsDivider = Images.loadImage(Images.NOTE_TOOLS_DIVIDER);

    public DividedPanel(Image i) {
        super(i);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(noteToolsDivider, 14, 32, getWidth() - 28, 2, null);
    }
}
