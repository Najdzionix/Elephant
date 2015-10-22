package com.pinktwins.elephant.editor;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.*;
import com.pinktwins.elephant.data.Note;
import com.pinktwins.elephant.data.Note.Meta;
import com.pinktwins.elephant.data.Notebook;
import com.pinktwins.elephant.data.Vault;
import com.pinktwins.elephant.editor.panel.EditorToolsPanel;
import com.pinktwins.elephant.editor.panel.NoteEditorsPanel;
import com.pinktwins.elephant.eventbus.TagsChangedEvent;
import com.pinktwins.elephant.eventbus.UIEvent;
import com.pinktwins.elephant.model.AttachmentInfo;
import com.pinktwins.elephant.panel.BackgroundPanel;
import com.pinktwins.elephant.util.*;
import org.apache.commons.lang3.SystemUtils;
import org.pegdown.PegDownProcessor;

import javax.swing.*;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class NoteEditor extends BackgroundPanel implements EditorEventListener {

	private static final Logger LOG = Logger.getLogger(NoteEditor.class.getName());
	public static final int kMinNoteSize = 288;    // TODO move to constatns class
	private static Image tile = Images.loadImage(Images.NOTE_EDITOR);

	private ElephantWindow window;
	private boolean isDirty;
	private final int kNoteOffset = 64;
	private final int kBorder = 14;

	private Note loadAfterLayout = null;
	public EditorWidthImageScaler editorWidthScaler = new EditorWidthImageScaler();
	public ImageAttachmentImageScaler imageAttachmentImageScaler = new ImageAttachmentImageScaler();
	public EditorController editorController = new EditorController(this);
	NoteEditorStateListener stateListener;
	private Note currentNote, previousNote;
	private NoteAttachments attachments = new NoteAttachments();
	private NoteEditorsPanel main;

	public static final ImageScalingCache scalingCache = new ImageScalingCache();
	public static final PegDownProcessor pegDown = new PegDownProcessor(org.pegdown.Parser.AUTOLINKS);
	private EditorToolsPanel tools;

	class EditorWidthImageScaler implements ImageScaler {
		int adjust = (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) ? -20 : -12;

		@Override
		public Image scale(Image i, File source) {
			return getScaledImage(i, source, adjust, true);
		}

		@Override
		public Image getCachedScale(File source) {
			return getScaledImageCacheOnly(source, adjust, true);
		}
	}

	public class ImageAttachmentImageScaler implements ImageScaler {
		public Image scale(Image i, File source) {
			return getScaledImage(i, source, 0, false);
		}

		@Override
		public Image getCachedScale(File source) {
			return getScaledImageCacheOnly(source, 0, false);
		}
	}

	public void addStateListener(NoteEditorStateListener l) {
		stateListener = l;
	}

	public NoteEditor(ElephantWindow w) {
		super(tile);
		window = w;

		Elephant.eventBus.register(this);

		createComponents();
	}

	private void createComponents() {
        tools = new EditorToolsPanel(this);
        final int topBorderOffset = 2;
		main = new NoteEditorsPanel(this);
		editorController.setScroll(main.getScroll());
		main.add(tools);
		add(main, BorderLayout.CENTER);

		caretChanged(main.getEditor().getTextPane());

		addComponentListener(new ResizeListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				Rectangle mb = main.getBounds();
				Rectangle ab = main.getEditorPanel().getBounds();

				ab.width = mb.width - kBorder * 2;
				main.getEditorPanel().setBounds(ab);

				main.getScrollHolder().setBounds(0, kNoteOffset + topBorderOffset, getWidth(), getHeight() - kNoteOffset - topBorderOffset);
				main.getAreaHolder().setBounds(0, 0, ab.width, ab.height);

				Rectangle r = tools.getBounds();
				r.width = getWidth();
				tools.setBounds(r);

				tools.getTagPane().updateWidth(r.width);

				if (loadAfterLayout != null) {
					EventQueue.invokeLater(() -> {
                        _load(loadAfterLayout);
                        loadAfterLayout = null;
                    });
				}
			}
		});

		main.addMouseListener(new CustomMouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				unfocus();
			}
		});

		main.setTransferHandler(new EditorAttachmentTransferHandler(this, main.getEditor()));
	}

	private long getUsableEditorWidth() {
		return getWidth() - kBorder * 4 - 12;
	}

	private Image getScaledImageCacheOnly(File sourceFile, int widthOffset, boolean useFullWidth) {
		SimpleImageInfo info;
		try {
			info = new SimpleImageInfo(sourceFile);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
			return null;
		}

		long w = getUsableEditorWidth() + widthOffset;
		long iw = info.getWidth();

		if (useFullWidth || iw > w) {
			float f = w / (float) iw;
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) info.getHeight());

			return scalingCache.get(sourceFile, scaledWidth, scaledHeight);
		}

		return null;
	}

	private Image getScaledImage(Image i, File sourceFile, int widthOffset, boolean useFullWidth) {
		long w = getUsableEditorWidth() + widthOffset;
		long iw = i.getWidth(null);

		if (useFullWidth || iw > w) {
			float f = w / (float) iw;
			int scaledWidth = (int) (f * (float) iw);
			int scaledHeight = (int) (f * (float) i.getHeight(null));

			Image cached = scalingCache.get(sourceFile, scaledWidth, scaledHeight);
			if (cached != null) {
				return cached;
			}

			Image img = i.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING);
			scalingCache.put(sourceFile, scaledWidth, scaledHeight, img);

			return img;
		} else {
			return i;
		}
	}

	public void openNotebookChooserForMoving() {
		if (currentNote != null) {
			NotebookChooser nbc = new NotebookChooser(window, String.format("Move \"%s\"", main.getEditor().getTitle()));

			// Center on window
			Point p = main.getLocationOnScreen();
			Rectangle r = window.getBounds();
			int x = r.x + r.width / 2 - NotebookChooser.fixedWidth / 2;
			nbc.setBounds(x, p.y, NotebookChooser.fixedWidth, NotebookChooser.fixedHeight);

			nbc.setVisible(true);

			nbc.setNotebookActionListener(new NotebookActionListener() {
				@Override
				public void didCancelSelection() {
				}

				@Override
				public void didSelect(Notebook nb) {
					moveNoteAction(currentNote, nb);
				}
			});
		}
	}

	protected void moveNoteAction(Note n, Notebook destination) {
		if (n == null || destination == null) {
			throw new AssertionError();
		}

		File source = n.file().getParentFile();
		if (destination.folder().equals(source)) {
			return;
		}

		LOG.info("move " + n.getMeta().title() + " -> " + destination.name() + " (" + destination.folder() + ")");

		n.moveTo(destination.folder());

		int index = window.getIndexOfFirstSelectedNoteInNoteList();

		window.sortAndUpdate();

		if (window.isShowingSearchResults()) {
			window.redoSearch();
		}

		clear();

		if (index >= 0) {
			window.selectNoteByIndex(index);
		}
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void clear() {
		currentNote = null;
		attachments = new NoteAttachments();
		main.getEditor().saveSelection();
		main.getEditor().clear();
		isDirty = false;
		visible(false);
	}

	public void load(final Note note) {
		if (getWidth() == 0) {
			loadAfterLayout = note;
		} else {
			_load(note);
		}
	}

//	todo
	public CustomEditor getEditor() {
		return main.getEditor();
	}

	public void _load(Note note) {

		if (!note.file().exists()) {
			// XXX Tell user what is going on.
			clear();
			return;
		}

		currentNote = note;
		attachments = new NoteAttachments();

		Meta m = note.getMeta();

		main.getEditor().setTitle(m.title());
		main.getEditor().setText(note.contents());
		main.getEditor().setMarkdown(note.isMarkdown());

		tools.getTagPane().load(Vault.getInstance().resolveTagIds(m.tags()));

		List<Note.AttachmentInfo> info = currentNote.getAttachmentList();
		if (!info.isEmpty()) {

			// We need to insert attachments from end to start - thus, sort.
			Collections.reverse(info);

			for (Note.AttachmentInfo ap : info) {

				// If position to insert attachment into would have
				// component content already, it would be overwritten.
				// Make sure there is none.
				AttributeSet as = main.getEditor().getAttributes(ap.position);
				if (as instanceof LeafElement) {
					LeafElement l = (LeafElement) as;
					if (!"content".equals(l.getName())) {
						main.getEditor().insertNewline(ap.position);
					}
				}

				attachments.insertFileIntoNote(this, ap.f, ap.position);
			}
		}

		attachments.loaded();

		main.getEditor().discardUndoBuffer();

		if (note.isMarkdown()) {
			String contents = note.contents();
			String html = pegDown.markdownToHtml(main.getEditor().isRichText ? Note.plainTextContents(contents) : contents);
			main.getEditor().displayHtml(currentNote.file(), html);
		}

		if (note.isHtml()) {
			main.getEditor().displayBrowser(currentNote.file());
		}

		visible(true);

		Notebook nb = Vault.getInstance().findNotebook(note.file().getParentFile());
		tools.getCurrNotebook().setText(nb.name());

		tools.getTrash().setVisible(!nb.folder().equals(Vault.getInstance().getTrash()));

		tools.updateNote(note);

		caretChanged(main.getEditor().getTextPane());

		if (previousNote != null && previousNote.equals(note)) {
			main.getEditor().restoreSelection();
		}

		previousNote = currentNote;
	}

	private void reloadTags() {
		Meta m = currentNote.getMeta();
		tools.getTagPane().load(Vault.getInstance().resolveTagIds(m.tags()));
	}

	private void reloadDates() {
		tools.updateNote(currentNote);
	}

	public void focusQuickLook() {
		for (Object o : attachments.keySet()) {
			if (o instanceof FileAttachment) {
				FileAttachment fa = (FileAttachment) o;
				fa.focusQuickLook();
				break;
			}
		}
	}

	private void visible(boolean b) {
		main.setVisible(b);
	}

	public boolean hasFocus() {
		return main.getEditor().hasFocus() || tools.getTagPane().hasFocus();
	}

	public void focusTags() {
		tools.getTagPane().requestFocus();
	}

	public void unfocus() {
		window.unfocusEditor();
	}

	@Subscribe
	public void handleTagsChangedEvent(TagsChangedEvent event) {
		// some tags were added/changed. Not neccessarily this note's tags,
		// but might have, so reload tags.
		if (currentNote != null) {
			reloadTags();
		}
	}

	@Subscribe
	public void handleUIEvent(UIEvent event) {
		if (event.kind == UIEvent.Kind.editorWillChangeNote) {
			saveChanges();
		}
	}

	public void saveChanges() {
		if (!isDirty && !tools.getTagPane().isDirty()) {
			return;
		}

		SaveChanges.saveChanges(currentNote, attachments, this, tools.getTagPane());

		attachments.loaded();
		isDirty = false;

		main.getScroll().setLocked(true);
		main.getScroll().unlockAfter(100);

		reloadDates();
	}

	public void focusTitle() {
		main.getEditor().focusTitle();
	}

	public void focusEditor() {
		main.getEditor().getTextPane().requestFocusInWindow();
	}

	@Override
	public void editingFocusGained() {
		isDirty = true;
	}

	@Override
	public void editingFocusLost() {
		saveChanges();
	}

	@Override
	public void caretChanged(final JTextPane text) {
		EventQueue.invokeLater(() -> {
            int pos = text.getCaretPosition();
            int len = text.getDocument().getLength();

            // When editor is unfocused, caret should only change when
            // inserting images/attachments to document during loading.
            // We want to see the top of note after all loading is done,
            // so keep vertical scroll bar at 0.
            //
            // Only exception to this should be dropping a file,
            // which can change caret position while editor
            // is unfocused. editor.maybeImporting() tracks
            // drag'n'drop state - true indicates a drop might
            // be in progress, and we need to keep scroll value.
            if (!main.getEditor().isFocusOwner() && !main.getEditor().maybeImporting()) {
				main.getScroll().getVerticalScrollBar().setValue(0);
            }

            // Writing new lines, keep scroll to bottom
            if (pos == len) {
				main.getScroll().getVerticalScrollBar().setValue(Integer.MAX_VALUE);
            }
        });

		int len = text.getDocument().getLength();
		int start = text.getSelectionStart(), end = text.getSelectionEnd();
		boolean hasSelection = (start >= 0 && start < len && end > start && end <= len);
		boolean hasFocus = text.hasFocus();

		if (stateListener != null) {
			stateListener.stateChange(hasFocus, hasSelection);
		}
	}

	private void insertMarkdownLink(File f) {
		boolean isImage = Images.isImage(f);
		String name = f.getName();
		String encName = name;
		try {
			encName = URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.severe("Fail: " + e);
		}
		encName = encName.replace("+", "%20");

		JTextPane tp = main.getEditor().getTextPane();
		try {
			tp.getDocument().insertString(tp.getCaretPosition(), String.format("%s[%s](%s \"\")\n", isImage ? "!" : "", name, encName), null);
		} catch (BadLocationException e) {
			LOG.severe("Fail: " + e);
		}
	}

	@Override
	public void filesDropped(List<File> files) {
		JTextPane noteArea = main.getEditor().getTextPane();
		for (File f : files) {
			if (f.isDirectory()) {
				// XXX directory dropped. what to do? compress and import zip?
			}
			if (f.isFile()) {
				System.out.println("file: " + f.getAbsolutePath());
				try {
					isDirty = true;

					File attached = currentNote.importAttachment(f);
					currentNote.getMeta().setAttachmentPosition(attached, noteArea.getCaretPosition());

					attachments.insertFileIntoNote(this, attached, noteArea.getCaretPosition());

					if (currentNote.isMarkdown()) {
						insertMarkdownLink(f);
					}
				} catch (IOException e) {
					LOG.severe("Fail: " + e);
				}
			}
		}
	}

	public void cutAction() {
		main.getEditor().getTextPane().cut();
	}

	public void copyAction() {
		main.getEditor().getTextPane().copy();
	}

	public void pasteAction() {
		main.getEditor().getTextPane().paste();
		main.getEditor().getTextPane().requestFocusInWindow();
	}

	public void undo() {
		main.getEditor().undo();
	}

	public void redo() {
		main.getEditor().redo();
	}

	private void turnToPlainText_format() {
		List<AttachmentInfo> info = main.getEditor().getAttachmentInfo();
		List<AttachmentInfo> info_reverse = main.getEditor().removeAttachmentElements(info);
		main.getEditor().turnToPlainText();
		importAttachments(info_reverse);
	}

	public void turnToPlainText() {
		turnToPlainText_format();
		/*
		 * try { currentNote.attemptSafeRename(editor.getTitle() + ".txt"); editor.setMarkdown(false); } catch
		 * (IOException e) { e.printStackTrace(); }
		 */
	}

	/*
	 * somehow I just dont like this. public void turnToMarkdown() { if (!currentNote.isMarkdown()) {
	 * turnToPlainText_format(); try { currentNote.attemptSafeRename(editor.getTitle() + ".md");
	 * editor.setMarkdown(true); } catch (IOException e) { e.printStackTrace(); } } }
	 */

	public void importAttachments(List<AttachmentInfo> info) {
		for (AttachmentInfo i : info) {
			File f = attachments.get(i.getObject());
			if (f != null) {
				// Use note.att-path in case note was renamed
				File ff = new File(currentNote.attachmentFolderPath() + File.separator + f.getName());

				// remove previous object mapping,
				attachments.remove(i.getObject());
				// insert.. will map a new Object -> File
				attachments.insertFileIntoNote(this, ff, i.getStartPosition());
			}
		}
	}

	@Override
	public void attachmentClicked(MouseEvent event, Object attachmentObject) {
		if (attachmentObject != null && event.getClickCount() == 2) {
			LaunchUtil.launch(attachments.get(attachmentObject));
		}
	}

	@Override
	public void attachmentMoved(AttachmentInfo info) {
		attachments.makeDirty();
	}

	public static Logger getLOG() {
		return LOG;
	}

	public Note getCurrentNote() {
		return currentNote;
	}

	public void setCurrentNote(Note currentNote) {
		this.currentNote = currentNote;
	}

	public Note getPreviousNote() {
		return previousNote;
	}

	public void setPreviousNote(Note previousNote) {
		this.previousNote = previousNote;
	}

	public ElephantWindow getWindow() {
		return window;
	}
}
