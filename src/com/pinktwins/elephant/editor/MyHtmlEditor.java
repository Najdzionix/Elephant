package com.pinktwins.elephant.editor;

import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.HTMLEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by Kamil Nadonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class MyHtmlEditor  extends JPanel{
    private JFXPanel jfxPanel;          // The JavaFX component(s)
    private JButton swingButton;
    private HTMLEditor htmlEditor;

    public MyHtmlEditor() {

        initComponents();
    }

    private void initComponents(){
        // The JavaFX 2.x JFXPanel makes the Swing integration seamless
        jfxPanel = new JFXPanel();

        // Create the JavaFX Scene
        createScene();

        setLayout(new BorderLayout());
        add(jfxPanel, BorderLayout.CENTER);

        swingButton = new JButton();
        swingButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent ae) {
                        System.out.print(htmlEditor.getHtmlText());
//                        System.out.print(((HTMLEditor)jfxPanel.getScene().getFocusOwner()).getHtmlText());
                        System.exit(0);
                    }
                });

        swingButton.setText("Close");

        add(swingButton, BorderLayout.SOUTH);
    }

    private void createScene() {
        // The Scene needs to be created on "FX user thread", NOT on the
        // AWT Event Thread
        PlatformImpl.startup(
                new Runnable() {
                    public void run() {


                        htmlEditor = new HTMLEditor();
                        htmlEditor.setHtmlText("Welcome in MyHtmlEditor:)");
                        htmlEditor.setPrefHeight(245);
                        htmlEditor.setStyle(
                                "-fx-font: 12 cambria;"
                                        + "-fx-border-color: brown; "
                                        + "-fx-border-style: dotted;"
                                        + "-fx-border-width: 2;"
                        );
                        Scene scene = new Scene(htmlEditor, javafx.scene.paint.Color.ALICEBLUE);
                        jfxPanel.setScene(scene);
//                        jfxPanel.setScene(scene);
                    }
                });
    }

    public HTMLEditor getHtmlEditor() { return htmlEditor;}


}
