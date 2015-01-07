package net.gnehzr.cct.misc;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ComboRenderer<T> extends JLabel implements ListCellRenderer<T> {
	public ComboRenderer() {
		setOpaque(true);
		setBorder(new EmptyBorder(1, 1, 1, 1));
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus){
		if(isSelected){
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		if(!((ComboItem)value).isEnabled()) {
			setBackground(list.getBackground());
//			setForeground(UIManager.getColor("Label.disabledForeground"));
			setForeground(Color.GRAY); //the above isn't having any noticeable effect on the foreground
		}

		if(((ComboItem)value).isInUse())
			setForeground(Color.RED);
		setFont(list.getFont());
		setText(value.toString());
		return this;
	}
}
