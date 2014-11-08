package net.gnehzr.cct.main;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class LoudComboBox extends JComboBox {
	//overriden to cause selected events to be fired even if the new item
	//is already selected (this helps simplify cct startup logic)
	public void setSelectedItem(Object selectMe) {
		super.setSelectedItem(selectMe);
		Object selected = getSelectedItem();
		if(selectMe == null || selected == null || selectMe.equals(selected) || !getSelectedItem().equals(selectMe)) {
			fireItemStateChanged(new ItemEvent(this, 0, selectMe, ItemEvent.SELECTED));
		}
	}
}
