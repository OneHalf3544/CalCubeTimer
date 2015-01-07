package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.ProfileListModel;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.ProfileDao;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ProfileEditor extends DefaultCellEditor {
	private Profile value;
	private ProfileListModel model;
	private String editText;
	private final ProfileDao profileDao;

	public ProfileEditor(String editText, ProfileListModel model, ProfileDao profileDao) {
		super(new JTextField());
		this.model = model;
		this.editText = editText;
		this.profileDao = profileDao;
	}

	private static final String INVALID_CHARACTERS = "\\/:*?<>|\"";

	@Override
	public boolean stopCellEditing() {
		String s = (String) super.getCellEditorValue();
		value = profileDao.getProfileByName(s);
		if(!value.toString().equals(originalValue)) {
			String error = null;
			if(stringContainsCharacters(s, INVALID_CHARACTERS))
				error = StringAccessor.getString("ProfileEditor.invalidname") + INVALID_CHARACTERS;
			if(model.getContents().contains(value)) {
				error = StringAccessor.getString("ProfileEditor.alreadyexists");
			}
			if(error != null) {
				JComponent component = (JComponent) getComponent();
				component.setBorder(new LineBorder(Color.RED));
				component.setToolTipText(error);
				Action toolTipAction = component.getActionMap().get("postTip");
				if (toolTipAction != null) {
					ActionEvent postTip = new ActionEvent(component,
							ActionEvent.ACTION_PERFORMED, "");
					toolTipAction.actionPerformed(postTip);
				}
				return false;
			}
		} else
			value = null;
		return super.stopCellEditing();
	}
	private boolean stringContainsCharacters(String s, String characters) {
		for(char ch : characters.toCharArray()) {
			if(s.indexOf(ch) != -1)
				return true;
		}
		return false;
	}

	private String originalValue;
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		this.value = null;
		if(value instanceof Profile)
			originalValue = ((Profile)value).getName();
		else
			originalValue = value.toString();
		((JComponent) getComponent()).setBorder(new LineBorder(Color.black));
		((JComponent) getComponent()).setToolTipText(editText);
		return super.getTableCellEditorComponent(table, originalValue, isSelected, row,
				column);
	}

	public Object getCellEditorValue() {
		return value;
	}
}
