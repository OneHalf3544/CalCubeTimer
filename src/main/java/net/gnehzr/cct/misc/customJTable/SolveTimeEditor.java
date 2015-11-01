package net.gnehzr.cct.misc.customJTable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.statistics.SolveTime;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

@Singleton
public class SolveTimeEditor extends DefaultCellEditor {
	private SolveTime value;

	@Inject
	public SolveTimeEditor() {
		super(new JTextField());
	}
	
	//TODO - http://www.pushing-pixels.org/?p=69 ?
	@Override
	public boolean stopCellEditing() {
		String s = (String) super.getCellEditorValue();
		try {
			value = new SolveTime(s);
		} catch (Exception e) {
			JComponent component = (JComponent) getComponent();
			component.setBorder(new LineBorder(Color.RED));
			component.setToolTipText(e.getMessage());
			Action toolTipAction = component.getActionMap().get("postTip");
			if (toolTipAction != null) {
				ActionEvent postTip = new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "");
				toolTipAction.actionPerformed(postTip);
			}
			return false;
		}
		return super.stopCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.value = null;
		((JComponent) getComponent()).setBorder(new LineBorder(Color.BLACK));
		((JComponent) getComponent()).setToolTipText(StringAccessor.getString("CALCubeTimer.typenewtime"));
		return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		return value;
	}
}
