package com.pinktwins.elephant;

import com.pinktwins.elephant.util.Factory;

import java.util.List;

class Trigger {
	private final List<Trigger> list = Factory.newArrayList();

	public boolean isDown = false;

	public Trigger get() {
		Trigger t = new Trigger();
		list.add(t);
		return t;
	}

	public void triggerAll() {
		for (Trigger t : list) {
			t.isDown = true;
		}
		list.clear();
	}
}
