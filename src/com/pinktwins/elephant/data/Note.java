package com.pinktwins.elephant.data;

import com.pinktwins.elephant.editor.NoteEditorType;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;
import com.pinktwins.elephant.util.RtfUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class Note implements Comparable<Note> {

	private static final Logger LOG = Logger.getLogger(Note.class.getName());

	private File file, meta;
	private String fileName = "";
	private NoteEditorType noteType;
	private boolean saveLocked = false;

	private static DateTimeFormatter df = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.getDefault());

	private static File[] emptyFileList = new File[0];

	private static final NoteBoundDirectory[] boundDirs = {
			Note::attachmentFolderPath,
			Note::resourceFolderPath,
			Note::filesFolderPath
	};

	public Note(File f) {
		file = f;
		meta = metaFromFile(f);

		String s = f.getName().toLowerCase();
		boolean editable = s.endsWith(".txt") || s.endsWith(".rtf") || s.endsWith(".md");

		saveLocked = !editable;

		readInfo();
		recognizeTypeNote();
	}

	public Note(File f, NoteEditorType type) {
		this(f);
		noteType = type;
	}

	private void recognizeTypeNote()  {
	  if(isMarkdown()) {
		  noteType = NoteEditorType.MARKDOWN;
	  } else if(isHtml()){
		  noteType = NoteEditorType.HTML;
	  } else {
		  noteType = NoteEditorType.OLD;
	  }
	}
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (o instanceof File) {
			return file.equals(o);
		}

		if (o instanceof Note) {
			return file.equals(((Note) o).file());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return file.getAbsolutePath().hashCode();
	}

	@Override
	public int compareTo(Note n) {
		long m1 = file.lastModified(), m2 = n.file().lastModified();

		if (m1 > m2) {
			return -1;
		}

		if (m1 < m2) {
			return 1;
		}

		return 0;
	}

	private File metaFromFile(File f) {
		String flatPath = f.getAbsolutePath().replace(Vault.getInstance().getHome().getAbsolutePath() + File.separator, "");
		flatPath = flatPath.replaceAll(Matcher.quoteReplacement(File.separator), "_");
		File m = new File(Vault.getInstance().getHome().getAbsolutePath() + File.separator + ".meta" + File.separator + flatPath);
		m.getParentFile().mkdirs();
		return m;
	}

	public static Notebook findContainingNotebook(File f) {
		return Vault.getInstance().findNotebook(f.getParentFile());
	}

	public Notebook findContainingNotebook() {
		return findContainingNotebook(file);
	}

	public boolean isMarkdown() {
		String s = file.getName().toLowerCase();
		return s.endsWith(".md.txt") || s.endsWith(".md");
	}

	public boolean isHtml() {
		String s = file.getName().toLowerCase();
		return s.endsWith(".htm") || s.endsWith(".html");
	}

	public String createdStr() {
		long t = getMeta().created();
		long m = lastModified();
		return df.print(t < m ? t : m);
	}

	public String updatedStr() {
		return df.print(lastModified());
	}

	private void readInfo() {
		fileName = file.getName();
	}

	public File file() {
		return file;
	}

	public String name() {
		return fileName;
	}

	public long lastModified() {
		return file.lastModified();
	}

	private String readFileAsString() {
		byte[] contents = IOUtil.readFile(file);
		return new String(contents, Charset.defaultCharset());
	}

	public String contents() {
		if (saveLocked) {
			return isHtml() ? "(html content)" : "(binary)";
		}

		return readFileAsString();
	}

	public String contentsIncludingRawHtml() {
		if (isHtml()) {
			return readFileAsString();
		} else {
			return contents();
		}
	}

	public static String plainTextContents(String contents) {
		DefaultStyledDocument doc = new DefaultStyledDocument();
		try {
			RtfUtil.putRtf(doc, contents, 0);
			return doc.getText(0, doc.getLength());
		} catch (IOException | BadLocationException e) {
			LOG.severe("Fail: " + e);
		}

		return "";
	}

	public void save(String newText) {
		if (saveLocked) {
			return;
		}

		try {
			IOUtil.writeFile(file, newText);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public Map<String, String> getMetaMap() {
		try {
			String json = new String(IOUtil.readFile(meta), Charset.defaultCharset());
			if (json.isEmpty()) {
				return Collections.emptyMap();
			}

			JSONObject o = new JSONObject(json);
			Map<String, String> map = Factory.newHashMap();

			@SuppressWarnings("unchecked")
			Iterator<String> i = o.keys();
			while (i.hasNext()) {
				String key = i.next();
				String value = o.optString(key);
				map.put(key, value);
			}

			return map;
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		}

		return Collections.emptyMap();
	}

	private void setMeta(String key, String value) {
		try {
			String json = new String(IOUtil.readFile(meta), Charset.defaultCharset());
			if (json.isEmpty()) {
				json = "{}";
			}

			JSONObject o = new JSONObject(json);
			o.put(key, value);
			IOUtil.writeFile(meta, o.toString(4));
		} catch (JSONException | IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public Meta getMeta() {
		return new Metadata(getMetaMap());
	}

	private class Metadata implements Meta {

		private Map<String, String> map;

		private Metadata(Map<String, String> map) {
			this.map = map;
		}

		@Override
		public String title() {
			String s = map.get("title");
			if (s == null) {
				if (file.exists()) {
					s = file.getName();
					s = s.replace("." + FilenameUtils.getExtension(s), "");
				} else {
					s = "Untitled";
				}
			}
			return s;
		}

		@Override
		public long created() {
			try {
				return Long.valueOf(map.get("created"));
			} catch (NumberFormatException e) {
				setCreatedTime();
				return new Date().getTime();
			}
		}

		@Override
		public void title(String newTitle) {
			setMeta("title", newTitle);
			reload();
		}

		@Override
		public void setCreatedTime() {
			setMeta("created", String.valueOf(new Date().getTime()));
			reload();
		}

		private void reload() {
			map = getMetaMap();
		}

		@Override
		public int getAttachmentPosition(File attachment) {
			String key = "attachment:" + attachment.getName() + ":position";
			String value = map.get(key);
			if (value == null) {
				return 0;
			}
			return Integer.parseInt(value);
		}

		@Override
		public void setAttachmentPosition(File attachment, int position) {
			setMeta("attachment:" + attachment.getName() + ":position", String.valueOf(position));
			reload();
		}

		/* Return list of tagIds. */
		@Override
		public List<String> tags() {
			List<String> list = Factory.newArrayList();

			String ids = map.get("tagIds");
			if (ids != null) {
				String[] a = ids.split(",");
				for (String s : a) {
					list.add(s);
				}
			}

			return list;
		}

		@Override
		public void setTags(List<String> tagIds, List<String> tagNames) {
			// tagIds are what matters. names are stored just to make exporting
			// out of Elephant easier.
			setMeta("tagIds", StringUtils.join(tagIds, ","));
			setMeta("tagNames", StringUtils.join(tagNames, ","));
			reload();
		}

	}

	private String ts() {
		return Long.toString(System.currentTimeMillis(), 36);
	}

	public void moveTo(File dest) {

		File destFile = new File(dest + File.separator + file.getName());
		File destMeta = metaFromFile(destFile);
		boolean destExists = destFile.exists() || destMeta.exists();

		if (!destExists) {
			for (NoteBoundDirectory d : boundDirs) {
				File f = new File(d.getPath(destFile));
				if (f.exists()) {
					destExists = true;
					break;
				}
			}
		}

		if (destExists) {
			try {
				attemptSafeRename(file.getName());
				moveTo(dest);
				return;
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
				return;
			}
		}

		try {
			FileUtils.moveFileToDirectory(file, dest, false);
			if (meta.exists()) {
				FileUtils.moveFile(meta, destMeta);
			}

			for (NoteBoundDirectory d : boundDirs) {
				File bound = new File(d.getPath(file));
				if (bound.exists() && bound.isDirectory()) {
					FileUtils.moveDirectoryToDirectory(bound, dest, true);
				}
			}

			Notebook source = findContainingNotebook();
			if (source != null) {
				source.refresh();
			}

			Notebook nb = Vault.getInstance().findNotebook(dest);
			if (nb != null) {
				nb.refresh();
			}

			new NotebookEvent(NotebookEvent.Kind.noteMoved, file, destFile).post();
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public void attemptSafeRename(String newName) throws IOException {

		newName = newName.replaceAll("[^a-zA-Z0-9 \\.\\-]", "_");

		File newFile = new File(file.getParentFile().getAbsolutePath() + File.separator + newName);
		File newMeta = metaFromFile(newFile);

		boolean exists = newFile.exists() || newMeta.exists();
		if (!exists) {
			for (NoteBoundDirectory d : boundDirs) {
				File bound = new File(d.getPath(file));
				if (bound.exists() && bound.isDirectory()) {
					File newBound = new File(d.getPath(newFile));
					if (newBound.exists() && newBound.isDirectory()) {
						exists = true;
						break;
					}
				}
			}
		}

		if (exists) {
			// fallback
			String base = FilenameUtils.getBaseName(newName);
			String ext = FilenameUtils.getExtension(newName);
			newName = base + "_" + ts() + "." + ext;
			attemptSafeRename(newName);
			return;
		}

		if (meta.exists()) {
			FileUtils.moveFile(meta, newMeta);
		}
		if (file.exists()) {
			FileUtils.moveFile(file, newFile);
		}

		for (NoteBoundDirectory d : boundDirs) {
			File bound = new File(d.getPath(file));
			if (bound.exists() && bound.isDirectory()) {
				File newBound = new File(d.getPath(newFile));
				FileUtils.moveDirectory(bound, newBound);
			}
		}

		File oldFile = file;
		file = newFile;
		meta = newMeta;

		new NotebookEvent(NotebookEvent.Kind.noteRenamed, oldFile, newFile).post();
	}

	public File importAttachment(File f) throws IOException {
		File dest = new File(attachmentFolder().getAbsolutePath() + File.separator + f.getName());

		String orgDest = dest.getAbsolutePath();

		int n = 1;
		while (dest.exists()) {
			String ext = "." + FilenameUtils.getExtension(orgDest);
			dest = new File(orgDest.replace(ext, " " + n + ext));
			n++;
		}

		FileUtils.copyFile(f, dest);
		return dest;
	}

	public String attachmentFolderPath() {
		return attachmentFolderPath(file);
	}

	// Elephant's own 'attachments' folder for note attachments
	private static String attachmentFolderPath(File f) {
		return f.getAbsolutePath() + ".attachments";
	}

	// note's .resources folder created when exporting html from EN
	private static String resourceFolderPath(File f) {
		String s = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(s);
		s = s.substring(0, s.length() - ext.length()) + "resources";
		return s;
	}

	// note's _files folder created by Chrome when saving webpages in 'Complete format'
	private static String filesFolderPath(File f) {
		String s = f.getAbsolutePath();
		String ext = FilenameUtils.getExtension(s);
		s = s.substring(0, s.length() - ext.length() - 1) + "_files";
		return s;
	}

	private File attachmentFolder() throws IOException {
		String s = attachmentFolderPath(file);

		File f = new File(s);
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new IOException();
			}
		}

		return f;
	}

	private File[] getAttachmentFiles() {
		File f = new File(attachmentFolderPath(file));
		if (f.exists()) {
			return f.listFiles();
		} else {
			return emptyFileList;
		}
	}

	public List<AttachmentInfo2> getAttachmentList() {
		List<AttachmentInfo2> info = new ArrayList<AttachmentInfo2>();

		File[] files = getAttachmentFiles();
		if (files != null) {
			Meta m = getMeta();
			for (File f : files) {
				if (f.getName().charAt(0) != '.' && f.isFile()) {
					int position = m.getAttachmentPosition(f);
					info.add(new AttachmentInfo2(f, position));
				}
			}
			Collections.sort(info);
		}

		return info;
	}

	public void removeAttachment(File f) {
		try {
			File deletedFolder = new File(attachmentFolder() + File.separator + "deleted");

			File newDeletedFile = new File(deletedFolder + File.separator + f.getName());
			while (newDeletedFile.exists()) {
				newDeletedFile = new File(deletedFolder + File.separator + f.getName() + "_" + ts() + "_" + ((int) (Math.random() * 1000000)));
			}

			FileUtils.moveFile(f, newDeletedFile);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public static String getResourceNote(String name) {
		try {
			return IOUtils.toString(Note.class.getClass().getResourceAsStream("/notes/" + name));
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}
		return "";
	}

	public NoteEditorType getNoteType() {
		return noteType;
	}

	public void setNoteType(NoteEditorType noteType) {
		this.noteType = noteType;
	}
}
