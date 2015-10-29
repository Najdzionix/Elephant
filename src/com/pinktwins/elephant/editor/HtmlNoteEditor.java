package com.pinktwins.elephant.editor;

import com.pinktwins.elephant.data.Note;
import com.sun.javafx.application.PlatformImpl;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.web.HTMLEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by Kamil Nadłonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class HtmlNoteEditor extends JPanel implements Editable{
    private JFXPanel jfxPanel;          // The JavaFX component(s)
    private JButton swingButton;
    private HTMLEditor htmlEditor;
    private Note note;
    public HtmlNoteEditor(Note note) {
        this.note = note;
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
                        htmlEditor.setHtmlText(note.contents());
                        htmlEditor.setPrefHeight(245);
                        htmlEditor.setStyle(
                                "-fx-font: 12 cambria;"
                                        + "-fx-border-color: brown; "
                                        + "-fx-border-style: dotted;"
                                        + "-fx-border-width: 2;"
                        );
                        javafx.scene.control.Button b = new Button("TTTTTT");
                        Scene scene = new Scene(htmlEditor, javafx.scene.paint.Color.ALICEBLUE);
                        jfxPanel.setScene(scene);
//                        jfxPanel.setScene(scene);
                    }
                });
    }

    public HTMLEditor getHtmlEditor() { return htmlEditor;}


    @Override
    public void load(Note note) {
       this.note = note;

//        jfxPanel.revalidate();
//        jfxPanel.getScene().getRoot().setVisible(true);
//        jfxPanel.getScene().getRoot().requestLayout();;
//        if(htmlEditor != null) {
//            System.out.println("tttttt  t ");
//            htmlEditor.setVisible(true);
//            htmlEditor.requestLayout();
//        }
//        htmlEditor.setHtmlText(note.contents());
    }

    @Override
    public String getText() {
        return htmlEditor.getHtmlText();
    }

    @Override
    public Note getNote() {
        //TODO update content Note ..
        return note;
    }
}
