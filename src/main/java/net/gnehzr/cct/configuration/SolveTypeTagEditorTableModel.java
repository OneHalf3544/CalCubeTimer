package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.statistics.SolveType;
import net.gnehzr.cct.statistics.StatisticsTableModel;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SolveTypeTagEditorTableModel extends DraggableJTableModel {

	private static final Logger LOG = Logger.getLogger(SolveTypeTagEditorTableModel.class);

	private JTable parent;
	private final Configuration configuration;
	private final StatisticsTableModel statsModel;

	private List<TypeAndName> tags;
	private List<TypeAndName> deletedTags;

	private String origValue;

	public SolveTypeTagEditorTableModel(JTable parent, Configuration configuration, StatisticsTableModel statsModel) {
		this.parent = parent;
		this.configuration = configuration;
		this.statsModel = statsModel;
	}
	public class TypeAndName {
		public String name;
		public SolveType type;
		public TypeAndName(String name, SolveType type) {
			this.name = name;
			this.type = type;
		}
		@Override
		public String toString() {
			if(type != null && type.isIndependent())
				return "<html><b>" + name + "</b></html>";
			return name;
		}

		@Override
		public boolean equals(Object o) {
            return o instanceof TypeAndName && name.equalsIgnoreCase(((TypeAndName) o).name);
        }

		@Override
		public int hashCode() {
			return name.toLowerCase().hashCode();
		}

	}

	public void setTags(Collection<SolveType> tagTypes) {
		tags = new ArrayList<>();
		deletedTags = new ArrayList<>();
		for(SolveType t : tagTypes)
			tags.add(new TypeAndName(t.toString(), t));
		fireTableDataChanged();
	}

	public void apply() {
		List<String> tagNames = new ArrayList<>(tags.size());
		for(int c = 0; c < tags.size(); c++) {
			TypeAndName tan = tags.get(c);
			if(tan.type == null) { //this indicates that the type was created
				try {
					SolveType.createSolveType(tan.name);
				} catch (Exception e) {
					LOG.info("unexpected exception", e);
					continue;
				}
			} else {
				tan.type.rename(tan.name);
			}
			tagNames.set(c, tan.name);
		}
		configuration.setStringArray(VariableKey.SOLVE_TAGS, tagNames);

		//no need to attempt to remove something that never got truly created
		deletedTags.stream()
				.filter(tan -> tan.type != null)
				.forEach(tan -> SolveType.remove(tan.type));
	}

	@Override
	public void deleteRows(int[] indices) {
		for(int c = indices.length - 1; c >= 0; c--) {
			TypeAndName tan = tags.get(indices[c]);
			int count = statsModel.getCurrentSession().getPuzzleStatistics().getPuzzleDatabase().getDatabaseTypeCount(tan.type);
			if(count == 0)
				deletedTags.add(tags.remove(indices[c])); //mark this type for deletion
			else
				Utils.showConfirmDialog(parent, StringAccessor.getString("SolveTypeTagEditorTableModel.deletefail") + "\n"
						+ tan.name + "/" + count +
						" (" + StringAccessor.getString("SolveTypeTagEditorTableModel.tag") + "/" +
						StringAccessor.getString("SolveTypeTagEditorTableModel.instances") + ")");
		}
		fireTableDataChanged();
	}

	@Override
	public void removeRows(int[] indices) {
		for(int c = indices.length - 1; c >= 0; c--)
			tags.remove(indices[c]);
		fireTableDataChanged();
	}

	@Override
	public String getColumnName(int column) {
		return "Tags";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return TypeAndName.class;
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public int getRowCount() {
		return tags == null ? 0 : tags.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return tags.get(rowIndex);
	}

	@Override
	public void insertValueAt(Object value, int rowIndex) {
		tags.add(rowIndex, (TypeAndName) value);
		fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		SolveType t = tags.get(rowIndex).type;
		return t == null || !t.isIndependent();
	}

	@Override
	public boolean isRowDeletable(int rowIndex) {
		SolveType t = tags.get(rowIndex).type;
		return t == null || !t.isIndependent();
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if(rowIndex == tags.size())
			tags.add(new TypeAndName((String)value, null));
		else
			tags.get(rowIndex).name = (String) value;
		fireTableDataChanged();
	}
	public TagEditor editor = new TagEditor();
	public class TagEditor extends DefaultCellEditor {

		public TagEditor() {
			super(new JTextField());
		}
		private String s;

		@Override
		public boolean stopCellEditing() {
			s = (String) super.getCellEditorValue();
			String error = null;
			if(s.indexOf(',') != -1)
				error = StringAccessor.getString("SolveTypeTagEditorTableModel.toomanycommas");
			else if(s.isEmpty())
				error = StringAccessor.getString("SolveTypeTagEditorTableModel.noemptytags");
			else if(tags.contains(new TypeAndName(s, null)) && !s.equalsIgnoreCase(origValue))
				error = StringAccessor.getString("SolveTypeTagEditorTableModel.noduplicates");
			if(error != null) {
				JComponent component = (JComponent) getComponent();
				component.setBorder(new LineBorder(Color.RED));
				component.setToolTipText(error);
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
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			s = null;
			((JComponent) getComponent()).setBorder(new LineBorder(Color.BLACK));
			((JComponent) getComponent()).setToolTipText(StringAccessor.getString("SolveTypeTagEditorTableModel.addtooltip"));
			origValue = value.toString();
			return super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}

		@Override
		public Object getCellEditorValue() {
			return s;
		}
	}
}
