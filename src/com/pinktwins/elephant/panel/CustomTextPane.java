package com.pinktwins.elephant.panel;

import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.editor.CustomDocument;
import com.pinktwins.elephant.editor.CustomEditor;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.RtfUtil;
import org.apache.commons.io.IOUtils;

import javax.activation.DataHandler;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Kamil Nadonek on 20.10.15.
 * email:kamilnadlonek@gmail.com
 */
public class CustomTextPane extends JTextPane implements ClipboardOwner {

    // http://www.javapractices.com/topic/TopicAction.do?Id=82
    private static final Logger LOG = Logger.getLogger(CustomTextPane.class.getName());
    private String prevRtfCopy = "";
    private CustomEditor customEditor;

    //TODO temporary isRichText need access ???
    public CustomTextPane(CustomEditor editor) {
        customEditor = editor;
    }


    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, this);
        prevRtfCopy = "";
    }

    public void setClipboardContentsRtf(String rtf) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            DataHandler hand = new DataHandler(new ByteArrayInputStream(rtf.getBytes("UTF-8")), "text/rtf");
            clipboard.setContents(hand, this);
        } catch (UnsupportedEncodingException e) {
            LOG.severe("Fail: " + e);
        }
        prevRtfCopy = rtf;
    }

    public String getClipboardContents() {
        String result = "";

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);

        DataFlavor[] fl = contents.getTransferDataFlavors();
        java.util.List<DataFlavor> textFlavors = Factory.newArrayList();
        for (DataFlavor df : fl) {
            String mime = df.getMimeType();
            if (mime.indexOf("text/rtf") >= 0 || mime.indexOf("text/plain") >= 0) {
                textFlavors.add(df);
            }
        }

        DataFlavor[] te = new DataFlavor[textFlavors.size()];
        te = textFlavors.toArray(te);

        DataFlavor best = DataFlavor.selectBestTextFlavor(te);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(best);
        if (hasTransferableText) {
            try {
                Reader r = best.getReaderForText(contents);
                BufferedReader br = new BufferedReader(r);
                result = IOUtils.toString(br);
            } catch (UnsupportedFlavorException e) {
                LOG.severe("Fail: " + e);
            } catch (IOException e) {
                LOG.severe("Fail: " + e);
            }
        }

        return result;
    }

    private class CPPInfo {
        Document doc;
        int pos, len, start, end, selLen, adjust;
        boolean hasSelection;

        public CPPInfo() {
            doc = getDocument();
            pos = getCaretPosition();
            len = doc.getLength();
            start = getSelectionStart();
            end = getSelectionEnd();
            selLen = end - start;
            adjust = 0;
            if (pos >= end) {
                adjust = end - start;
            }
            hasSelection = (start >= 0 && start < len && end > start && end <= len);
        }
    }

    @Override
    public void cut() {
        copy();
        CPPInfo i = new CPPInfo();
        if (i.hasSelection) {
            try {
                i.doc.remove(i.start, i.selLen);
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        }
    }

    @Override
    public void copy() {
        String s = getSelectedText();
        if (s != null && !s.isEmpty()) {
            setClipboardContents(s);
        }

        if (customEditor.isRichText) {
            int start = getSelectionStart();
            int end = getSelectionEnd();

            if (end > start) {
                // Put rtf to clipboard: clone document, remove
                // everything but selection.
                Document d = new CustomDocument();
                try {
                    RtfUtil.putRtf(d, RtfUtil.getRtf(getDocument()), 0);
                    d.remove(end, d.getLength() - end);
                    d.remove(0, start);
                    String rtf = RtfUtil.getRtf(d);

                    setClipboardContentsRtf(rtf);
                } catch (IOException e) {
                    LOG.severe("Fail: " + e);
                } catch (BadLocationException e) {
                    LOG.severe("Fail: " + e);
                }
            }
        }
    }

    @Override
    public void paste() {
        String s = getClipboardContents();

        // For some reason I don't get any rtf text from clipboard copied
        // there by me. Workround.
        if (s.isEmpty() && !prevRtfCopy.isEmpty()) {
            s = prevRtfCopy;
        }

        if (!s.isEmpty()) {
            try {
                CPPInfo i = new CPPInfo();

                if (i.hasSelection) {
                    i.doc.remove(i.start, i.selLen);
                }

                i.pos -= i.adjust;

                if (s.length() < 5 || !"{\\rtf".equals(s.substring(0, 5))) {
                    i.doc.insertString(i.pos, s, null);
                } else {
                    if (customEditor.isMarkdown) {
                        String plain = Note.plainTextContents(s);
                        i.doc.insertString(i.pos, plain, null);
                        return;
                    }

                    // RTFEditorKit doesn't support 'position' argument on
                    // read() method, so create a new document and copy
                    // text + styles over.
                    CustomDocument d = new CustomDocument();

                    try {
                        RtfUtil.putRtf(d, s, 0);

                        Element[] elems = d.getRootElements();
                        for (Element e : elems) {
                            for (int idx = 0, count = e.getElementCount(); idx < count - 1; idx++) {
                                Element sub = e.getElement(idx);
                                if ("paragraph".equals(sub.getName())) {
                                    int start = sub.getStartOffset();
                                    int end = sub.getEndOffset();
                                    AttributeSet as = d.getCharacterElement(start).getAttributes();

                                    if (end > start) {
                                        String text = d.getText(start, end - start);
                                        i.doc.insertString(i.pos, text, as);
                                        i.pos += end - start;
                                        customEditor.isRichText = true;
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOG.severe("Fail: " + e);
                    }
                }
            } catch (BadLocationException e) {
                LOG.severe("Fail: " + e);
            }
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}