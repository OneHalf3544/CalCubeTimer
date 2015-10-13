package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CalCubeTimerModel;
import net.gnehzr.cct.main.ScrambleChooserComboBox;
import net.gnehzr.cct.main.ScrambleVariationChooserComboBox;
import net.gnehzr.cct.misc.customJTable.DraggableJTable;
import net.gnehzr.cct.misc.customJTable.DraggableJTableModel;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.jetbrains.annotations.NotNull;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.api.SubstanceConstants;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class ScrambleCustomizationListModel extends DraggableJTableModel implements TableCellRenderer, TableCellEditor, MouseListener {

	private List<PuzzleType> customizations;
	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private final CalCubeTimerModel profileDao;

	public ScrambleCustomizationListModel(Configuration configuration, ScramblePluginManager scramblePluginManager, CalCubeTimerModel profileDao) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
		this.profileDao = profileDao;
	}

	public void setContents(List<PuzzleType> contents) {
		this.customizations = contents;
		fireTableDataChanged();
	}
	public List<PuzzleType> getContents() {
		return customizations;
	}

	@Override
	public void deleteRows(int[] indices) {
		removeRows(indices);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return PuzzleType.class;
	}

	private String[] columnNames = new String[]{
			StringAccessor.getString("ScrambleCustomizationListModel.scramblecustomization"),
			StringAccessor.getString("ScrambleCustomizationListModel.length"),
			StringAccessor.getString("ScrambleCustomizationListModel.generatorgroup"),
			"RA 0",
			"RA 1"};

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public int getRowCount() {
		return customizations == null ? 0 : customizations.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return customizations.get(rowIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if(columnIndex >= 1)
			return true;
		
		return customizations.get(rowIndex).getCustomization() != null;
			
	}
	@Override
	public boolean isRowDeletable(int rowIndex) {
		PuzzleType sc = customizations.get(rowIndex);
		List<PuzzleType> dbCustoms = profileDao.getSelectedProfile().getSessionsListTableModel().getSessionsList().getUsedPuzzleTypes();
		//we allow the user to delete a customization if it was autogenerated without a backing plugin (sc.getScramblePlugin().getPluginClass() == null)
		return !dbCustoms.contains(sc) && (sc.getCustomization() != null || sc.getScramblePlugin() == null);
	}

	@Override
	public void removeRows(int[] indices) {
		for(int ch = indices.length - 1; ch >=0; ch--) {
			int i = indices[ch];
			if(i >= 0 && i < customizations.size()) {
				customizations.remove(i);
			}
		}
		fireTableRowsDeleted(indices[0], indices[indices.length - 1]);
	}
	@Override
	public void insertValueAt(Object value, int rowIndex) {
		customizations.add(rowIndex, (PuzzleType)value);
		fireTableRowsInserted(rowIndex, rowIndex);
	}
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		PuzzleType newVal = (PuzzleType)value;
		if(rowIndex == customizations.size()) {
			customizations.add(rowIndex, newVal);
			fireTableRowsInserted(rowIndex, rowIndex);
		} else {
			customizations.set(rowIndex, newVal);
			fireTableRowsUpdated(rowIndex, rowIndex);
		}
	}
	@Override
	public void showPopup(MouseEvent e, DraggableJTable source, Component prevFocusOwner) {}

	//******* Start of renderer/editor stuff ****************//
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		String val = value == null ? "" : value.toString();
		if(value instanceof PuzzleType) {
			PuzzleType customization = (PuzzleType) value;
			ScrambleSettings v = customization.getScrambleVariation();
			if(column == 0) { //scramble customization
				String bolded = customization.getVariationName();
				if(bolded.isEmpty())
					bolded = customization.getScramblePlugin().getPuzzleName();
				val = "<html><b>" + bolded + "</b>";
				if(customization.getCustomization() != null)
					val += ":" + customization.getCustomization();
				val += "<html>";
			} else if(column == 1) { //scramble length
				val = "" + v.getLength();
			} else if(column == 2) { //generator group
				val = customization.getScrambleVariation().getGeneratorGroup();
			} else if(column == 3) { //ra 0
				val = getTrimmedState(customization, RollingAverageOf.OF_5);
			} else if(column == 4) { //ra 1
				val = getTrimmedState(customization, RollingAverageOf.OF_12);
			}
		}
		return new JLabel(val, SwingConstants.CENTER);
	}

	@NotNull
	private String getTrimmedState(PuzzleType customization, RollingAverageOf of5) {
		return customization.getRASize(of5) + " " + (customization.isTrimmed(of5) ? "Trimmed" : "Untrimmed");
	}

	private int editingColumn;

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		puzzleType = (PuzzleType) value;
		editingColumn = column;
		if(column == 0) //customization
			return getCustomizationPanel(puzzleType);
		else if(column == 1) //length
			return getLengthPanel(puzzleType);
		else if(column == 2) //generator
			return getGeneratorPanel(puzzleType);
		else if(column == 3) //ra0
			return getRAPanel(RollingAverageOf.OF_5, puzzleType);
		else if(column == 4) //ra1
			return getRAPanel(RollingAverageOf.OF_12, puzzleType);
		
		return null;
	}
	
	private JTextField generator;
	private JPanel getGeneratorPanel(PuzzleType sc) {
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.LINE_AXIS));
		temp.add(generator = new JTextField(sc.getScrambleVariation().getGeneratorGroup(), 6));
		generator.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		disabledComponents = new ArrayList<>();
		listenToContainer(temp);
		return temp;
	}

	JSpinner raSize;
	JCheckBox trimmed;

	private JPanel getRAPanel(final RollingAverageOf index, final PuzzleType sc) {
		raSize = new JSpinner(new SpinnerNumberModel(sc.getRASize(index), 0, null, 1));
		raSize.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.specifylength"));
		((JSpinner.DefaultEditor) raSize.getEditor()).getTextField().setColumns(3);
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.LINE_AXIS));
		temp.add(raSize);
		temp.add(trimmed = new JCheckBox(StringAccessor.getString("ScrambleCustomizationListModel.trimmed"), sc.isTrimmed(index)));
		JButton resetRA = new JButton("X");
		resetRA.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
		resetRA.addActionListener(e -> {
            raSize.setValue(configuration.getInt(VariableKey.RA_SIZE(index, null)));
            trimmed.setSelected(configuration.getBoolean(VariableKey.RA_TRIMMED(index, null)));
        });
		temp.add(resetRA);
		disabledComponents = new ArrayList<>();
		listenToContainer(temp);
		return temp;
	}

	PuzzleType puzzleType;
	ScrambleChooserComboBox<PuzzleType> scrambleVariations;
	JSpinner scramLength;
	private JTextField customField;

	private JPanel getCustomizationPanel(PuzzleType custom) {
		JPanel customPanel = new JPanel();
		customPanel.setLayout(new BoxLayout(customPanel, BoxLayout.LINE_AXIS));
		if(custom.getCustomization() != null) {
			scrambleVariations = new ScrambleVariationChooserComboBox(false, scramblePluginManager, configuration);
			scrambleVariations.addItem(scramblePluginManager.NULL_PUZZLE_TYPE);
			scrambleVariations.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
			scrambleVariations.setSelectedItem(custom.getScrambleVariation());
			scrambleVariations.addItemListener(e -> {
                if(e.getStateChange() == ItemEvent.SELECTED)
                    scramblePluginManager.setScrambleSettings(puzzleType, (ScrambleSettings) scrambleVariations.getSelectedItem());
            });
			scrambleVariations.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.selectvariation"));
			customPanel.add(scrambleVariations);

			String originalFieldText = custom.getCustomization();
			customField = new JTextField(originalFieldText, 15);
			customField.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.specifycustomization"));
			customPanel.add(customField);
		} else {
			customPanel.add(new JLabel("<html><b>" + custom.getVariationName() + "</b></html>"));
		}

		disabledComponents = new ArrayList<>();
		listenToContainer(customPanel);

		return customPanel;
	}

	private JPanel getLengthPanel(PuzzleType custom) {
		JPanel lengthPanel = new JPanel();
		lengthPanel.setLayout(new BoxLayout(lengthPanel, BoxLayout.LINE_AXIS));
		puzzleType = custom;
		scramLength = new JSpinner(new SpinnerNumberModel(Math.max(custom.getScrambleVariation().getLength(), 0), 0, null, 1));
		scramLength.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.specifylength"));
		((JSpinner.DefaultEditor) scramLength.getEditor()).getTextField().setColumns(3);
		lengthPanel.add(scramLength);

		JButton resetButton = new JButton("X");
		resetButton.setEnabled(false);
		resetButton.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.resetlength"));
		resetButton.setFocusable(false);
		resetButton.setFocusPainted(false);
		resetButton.setMargin(new Insets(0, 0, 0, 0));
		resetButton.addActionListener(e -> {
			scramLength.setValue(ScrambleSettings.getScrambleLength(puzzleType.getScramblePlugin(), puzzleType.getVariationName(), configuration, true));
        });
		resetButton.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
		resetButton.putClientProperty(SubstanceLookAndFeel.BUTTON_SIDE_PROPERTY, new SubstanceConstants.Side[] { SubstanceConstants.Side.LEFT });
		lengthPanel.add(resetButton);
		disabledComponents = new ArrayList<>();
		listenToContainer(lengthPanel);
		return lengthPanel;
	}

	private ArrayList<Component> disabledComponents;
	private void listenToContainer(Component c) {
		c.addMouseListener(this);
		c.setEnabled(false);
		disabledComponents.add(c);
		if(c instanceof Container) {
			Container container = (Container) c;
			for(Component c2 : container.getComponents())
				listenToContainer(c2);
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {}
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {
		for(Component c : disabledComponents) {
			c.setEnabled(true);
		}
	}

	private CellEditorListener listener;
	@Override
	public void addCellEditorListener(CellEditorListener l) {
		listener = l;
	}

	@Override
	public void cancelCellEditing() {
		scramLength = null;
		listener.editingCanceled(null);
	}

	@Override
	public Object getCellEditorValue() {
		return puzzleType;
	}

	@Override
	public boolean isCellEditable(EventObject e) {
		if(e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent) e;
			if(me.getClickCount() >= 2)
				return true;
		}
		return false;
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		if(listener == l)
			listener = null;
	}

	@Override
	public boolean shouldSelectCell(EventObject arg0) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		if(editingColumn == 0) { //customization
//		if(customization.getCustomization() != null) {
			String customName = customField.getText();
			String error = null;
			if(customName.isEmpty()) {
				error = StringAccessor.getString("ScrambleCustomizationListModel.noemptycustomization");
			} else {
				String fullCustomName = puzzleType.getVariationName() + ":" + customName;
				for(PuzzleType c : customizations) {
					if(c.toString().equals(fullCustomName) && c != puzzleType) {
						error = StringAccessor.getString("ScrambleCustomizationListModel.noduplicatecustomizations");
						break;
					}
				}
			}
			if(error != null) {
				customField.setBorder(new LineBorder(Color.RED));
				customField.setToolTipText(error);
				Action toolTipAction = customField.getActionMap().get("postTip");
				if(toolTipAction != null) {
					ActionEvent postTip = new ActionEvent(customField, ActionEvent.ACTION_PERFORMED, "");
					toolTipAction.actionPerformed(postTip);
				}
				return false;
			}
			puzzleType.setCustomization(customField.getText());
		} else if(editingColumn == 1) { //length
//		if(scramLength != null) {
			puzzleType.getScrambleVariation().setLength((Integer) scramLength.getValue());
		} else if(editingColumn == 2) { //generator
			scramblePluginManager.setScrambleSettings(puzzleType, puzzleType.getScrambleVariation().withGeneratorGroup(generator.getText()));
		} else if(editingColumn == 3) { //ra 0
			puzzleType.setRA(RollingAverageOf.OF_5, (Integer) raSize.getValue(), trimmed.isSelected());
		} else if(editingColumn == 4) { //ra 1
			puzzleType.setRA(RollingAverageOf.OF_12, (Integer) raSize.getValue(), trimmed.isSelected());
		}
		listener.editingStopped(null);
		return true;
	}
}
