package com.pinktwins.elephant.editor.panel;

import com.pinktwins.elephant.panel.CustomScrollPane;
import com.pinktwins.elephant.util.Images;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class TopShadowPanel extends JPanel {
    private static final long serialVersionUID = 6626079564069649611L;

    private static Image noteTopShadow = Images.loadImage(Images.NOTE_TOP_SHADOW);
    private final int kNoteOffset = 64;
    private final int kBorder = 14;
    private final Color lineColor = Color.decode("#b4b4b4");

    CustomScrollPane scroll;

    public TopShadowPanel(CustomScrollPane scroll) {
        this.scroll = scroll;
        setLayout(null);
        setBorder(BorderFactory.createEmptyBorder(kBorder, kBorder, kBorder, kBorder));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        JScrollBar v = scroll.getVerticalScrollBar();
        if (!v.isVisible()) {
            g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth(), 4, null);
        } else {
            if (v.getValue() < 4) {
                int adjust = scroll.isLocked() ? 0 : kBorder + 1;
                g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth() - adjust, 4, null);
            } else {
                g.drawImage(noteTopShadow, 0, kNoteOffset + 1, getWidth(), 2, null);
            }
        }

        g.setColor(lineColor);
        g.drawLine(0, 0, 0, getHeight());
    }
}
