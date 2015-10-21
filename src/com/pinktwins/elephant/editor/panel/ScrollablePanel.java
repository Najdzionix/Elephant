package com.pinktwins.elephant.editor.panel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Kamil Nadłonek on 21.10.15.
 * email:kamilnadlonek@gmail.com
 */
// Custom panel to fix note editor width to window width.
public class ScrollablePanel extends JPanel implements Scrollable {
    public static final int kMinNoteSize = 288;

    public Dimension getPreferredScrollableViewportSize() {
        Dimension d = getPreferredSize();
        if (d.height < kMinNoteSize) {
            d.height = kMinNoteSize;
        }
        return d;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}