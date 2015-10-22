package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.ElephantWindow;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Created by Kamil Nadłonek on 22.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class UIComponentFactory {

    public static JButton createJButton(String buttonLabel, Image icon) {
        JButton button = new JButton(buttonLabel);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        if(icon != null) {
            button.setIcon(new ImageIcon(icon));
        }

        return button;
    }

    public static JLabel createJLabel(String text, Font font, Color textColor, Border border) {
        JLabel label = new JLabel(text);
        label.setBorder(border);
        label.setForeground(textColor);
        label.setFont(font);
        return label;
    }


    public static JPanel createBorderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        return panel;
    }

}
