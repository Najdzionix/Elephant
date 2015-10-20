package com.pinktwins.elephant;

import com.pinktwins.elephant.data.Notebook;

public interface NotebookActionListener {
	public void didCancelSelection();

	public void didSelect(Notebook nb);
}
