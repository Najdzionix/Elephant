package com.pinktwins.elephant.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Timer;
import java.util.TimerTask;

public class CustomScrollPane extends JScrollPane {
	private boolean isLocked;
	private int lockedValue;
	private int inactivity = -1;

	Timer timer = new Timer();

	class Unlock extends TimerTask {
		@Override
		public void run() {
			setLocked(false);
		}
	}

	public CustomScrollPane(Component view) {
		super(view);

		final JScrollBar bar = getVerticalScrollBar();

		bar.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (isLocked) {
					bar.setValue(lockedValue);

					if (inactivity > 0) {
						timer.schedule(new Unlock(), inactivity);
						inactivity = -1;
					}
				}
			}
		});
	}

	public void setLocked(boolean b) {
		isLocked = b;
		lockedValue = getVerticalScrollBar().getValue();
		//this.setVerticalScrollBarPolicy(b ? JScrollPane.VERTICAL_SCROLLBAR_NEVER : JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void unlockAfter(int inactivity) {
		this.inactivity = inactivity;
	}
}
