package com.pinktwins.elephant.data;

import com.google.common.eventbus.Subscribe;
import com.pinktwins.elephant.Elephant;
import com.pinktwins.elephant.SideBarList.SideBarItemModifier;
import com.pinktwins.elephant.eventbus.NotebookEvent;
import com.pinktwins.elephant.eventbus.ShortcutsChangedEvent;
import com.pinktwins.elephant.util.Factory;
import com.pinktwins.elephant.util.IOUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class Shortcuts implements SideBarItemModifier {

	private static final Logger LOG = Logger.getLogger(Shortcuts.class.getName());

	private List<String> list = Factory.newArrayList();

	public Shortcuts() {
		Elephant.eventBus.register(this);
	}

	private File shortcutsFile() {
		return new File(Vault.getInstance().getHome() + File.separator + ".shortcuts");
	}

	public List<String> load() {
		list.clear();

		JSONObject o = IOUtil.loadJson(shortcutsFile());
		if (o.has("list")) {
			try {
				JSONArray arr = o.getJSONArray("list");

				for (int n = 0, len = arr.length(); n < len; n++) {
					String s = arr.getString(n);
					list.add(s);
				}
			} catch (JSONException e) {
				LOG.severe("Fail: " + e);
			}
		}

		return list;
	}

	public List<String> list() {
		return list;
	}

	@Subscribe
	public void handleNotebookChanged(NotebookEvent event) {
		switch (event.kind) {
		case noteCreated:
			break;
		case noteMoved:
		case noteRenamed:
			if (event.source == null || event.dest == null) {
				return;
			}

			String prefix = Vault.getInstance().getHome() + File.separator;
			String oldPath = event.source.getAbsolutePath();
			boolean modified = false;

			for (int n = 0; n < list.size(); n++) {
				String s = list.get(n);

				String fullShortcut = prefix + s;

				if (oldPath.equals(fullShortcut)) {
					String newPath = event.dest.getAbsolutePath();
					newPath = newPath.replace(prefix, "");
					list.remove(n);   //error we can not delete elements form list
					list.add(n, newPath);
					modified = true;
				}
			}

			if (modified) {
				save();
				new ShortcutsChangedEvent().post();
			}

			break;
		default:
			break;
		}
	}

	private void save() {
		JSONArray arr = new JSONArray();

		for (String s : list) {
			arr.put(s);
		}

		JSONObject o = new JSONObject();
		try {
			o.put("list", arr);
			IOUtil.writeFile(shortcutsFile(), o.toString(4));
		} catch (JSONException | IOException e) {
			LOG.severe("Fail: " + e);
		}
	}

	public void add(String s) {
		list.add(s);
		save();
	}

	public void addNotebook(Notebook nb) {
		add(nb.folder().getName());
	}

	public void addNote(Note note) {
		add(note.file().getParentFile().getName() + File.separator + note.file().getName());
	}

	@Override
	public void swap(String a, String b) {
		int indexA = list.indexOf(a), indexB = list.indexOf(b);
		if (indexA >= 0 && indexB >= 0) {
			list.remove(indexA);
			list.add(indexA, b);
			list.remove(indexB);
			list.add(indexB, a);
			save();
		}
	}

	@Override
	public void remove(String s) {
		int index = list.indexOf(s);
		if (index >= 0) {
			list.remove(index);
			save();
		}
	}
}
