package com.pinktwins.elephant;

import com.pinktwins.elephant.editor.CustomEditor;
import com.pinktwins.elephant.model.AttachmentInfo;
import com.pinktwins.elephant.panel.CustomTextPane;
import com.pinktwins.elephant.util.CustomMouseListener;
import com.pinktwins.elephant.util.Images;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;

public class AttachmentDragMouseListener extends CustomMouseListener {

	private static final Logger LOG = Logger.getLogger(AttachmentDragMouseListener.class.getName());
	private static final Image dragHand = Images.loadImage(Images.DRAG_HAND);
    private static final Image dragFile = Images.loadImage(Images.DRAG_FILE);

    private final CustomTextPane noteTextPane;
    private Object attachmentDragObject = null;
    private Point mouseDownPoint;
    private final Highlighter defaultHighlighter;
    private final Cursor defaultCursor;

	protected Object attachmentObject = null;

	public AttachmentDragMouseListener(CustomTextPane note) {
		this.noteTextPane = note;

		defaultHighlighter = note.getHighlighter();
		defaultCursor = note.getCursor();
	}

	private void checkIconAtPosition(int n) {
		if (n >= 0 && n < noteTextPane.getDocument().getLength()) {
			AttributeSet as = noteTextPane.getCustomDocument().getAttributeSetByPosition(n);

			boolean hasIcon = as.containsAttribute(CustomEditor.ELEM, CustomEditor.ICON);
			boolean hasComp = as.containsAttribute(CustomEditor.ELEM, CustomEditor.COMP);
			if (hasIcon || hasComp) {
				// Build cursor approriate for dragging this attachment
				attachmentDragObject = hasIcon ? StyleConstants.getIcon(as) : StyleConstants.getComponent(as);
				noteTextPane.setHighlighter(null);

				if (SystemUtils.IS_OS_WINDOWS) {
					// Cursor is more limited on Windows, cannot use the image.
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Cursor c = toolkit.createCustomCursor(dragHand, new Point(0, 8), "img");
					noteTextPane.setCursor(c);
				} else {
					// On Mac, use a scaled version of image next to dragHand.
					Image ref = null;

					if (attachmentDragObject instanceof ImageIcon) {
						ImageIcon img = (ImageIcon) attachmentDragObject;
						ref = img.getImage();
					} else {
						ref = dragFile;
					}

					float f = 96.0f / ref.getHeight(null);
					Image scaled = ref.getScaledInstance((int) (f * ref.getWidth(null)), (int) (f * ref.getHeight(null)), Image.SCALE_FAST);

					int dhWidth = dragHand.getWidth(null), dhHeight = dragHand.getHeight(null);

					BufferedImage composite = new BufferedImage(scaled.getWidth(null) + dhWidth, Math.max(scaled.getHeight(null), dhHeight),
							BufferedImage.TYPE_INT_ARGB);

					int yOffset = (composite.getHeight() - dhHeight) / 2;

					Graphics2D g = composite.createGraphics();
					g.drawImage(dragHand, 0, yOffset, null);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.4f));
					g.drawImage(scaled, dhWidth, 0, null);
					g.dispose();

					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Cursor c = toolkit.createCustomCursor(composite, new Point(0, yOffset + 8), "img");
					noteTextPane.setCursor(c);
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent event) {
		int i = noteTextPane.viewToModel(event.getPoint());
		checkIconAtPosition(i);
		checkIconAtPosition(i - 1);

		mouseDownPoint = event.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		if (attachmentDragObject != null) {
			// Started dragging from image. Move image to current caret position.
			List<AttachmentInfo> info = noteTextPane.getCustomDocument().getAttachmentsInfo();
			for (AttachmentInfo i : info) {
				if (i.getObject() == attachmentDragObject) {
					AttributeSet as = noteTextPane.getCustomDocument().getAttributeSetByPosition(i.getStartPosition());
					int len = i.getEndPosition() - i.getStartPosition();
					Document doc = noteTextPane.getDocument();

					if (!event.getPoint().equals(mouseDownPoint)) {
						try {
							String s = doc.getText(i.getStartPosition(), len);
							doc.remove(i.getStartPosition(), len);
							doc.insertString(noteTextPane.getCaretPosition(), s, as);
							attachmentMoved(i);
						} catch (BadLocationException e) {
							LOG.severe("Fail: " + e);
						}
					}

					if (i.getObject() instanceof ImageIcon) {
						attachmentObject = i.getObject();
					} else {
						attachmentObject = null;
					}

					break;
				}
			}

			attachmentDragObject = null;
			noteTextPane.setHighlighter(defaultHighlighter);
			noteTextPane.setCursor(defaultCursor);
		}
	}

	protected void attachmentMoved(AttachmentInfo info) {
	}
}
