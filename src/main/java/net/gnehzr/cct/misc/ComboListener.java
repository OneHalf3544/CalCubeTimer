package net.gnehzr.cct.misc;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ComboListener implements ActionListener{
	private JComboBox combo;
	private Object currentItem;

	public ComboListener(JComboBox combo){
		this.combo = combo;
		combo.setSelectedIndex(combo.getItemCount() - 1);
		currentItem = combo.getSelectedItem();
	}

	@Override
	public void actionPerformed(ActionEvent e){
		ComboItem tempItem = (ComboItem)combo.getSelectedItem();
		if (tempItem.isEnabled())
			currentItem = tempItem;
		else
			combo.setSelectedItem(currentItem);
	}
}
