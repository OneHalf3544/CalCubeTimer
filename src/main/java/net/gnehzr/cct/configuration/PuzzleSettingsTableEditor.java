package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.PuzzleTypeComboBox;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.scrambles.ScrambleSettings;
import net.gnehzr.cct.statistics.RollingAverageOf;
import org.jetbrains.annotations.NotNull;
import org.pushingpixels.lafwidget.LafWidget;
import org.pushingpixels.substance.api.SubstanceConstants;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class PuzzleSettingsTableEditor implements TableCellEditor, TableCellRenderer {

	private final Configuration configuration;
	private final ScramblePluginManager scramblePluginManager;
	private CellEditorListener listener;
	private int editingColumn;

	private JTextField generator;
	JSpinner raSize;
	JCheckBox trimmed;

	PuzzleType puzzleType;
	PuzzleTypeComboBox puzzleTypeComboBox;
	JSpinner scramLength;
	private JTextField customField;

	private List<Component> disabledComponents;

	public PuzzleSettingsTableEditor(Configuration configuration, ScramblePluginManager scramblePluginManager) {
		this.configuration = configuration;
		this.scramblePluginManager = scramblePluginManager;
	}

	//******* Start of renderer/editor stuff ****************//
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		String val = value == null ? "" : value.toString();
		if(value instanceof PuzzleType) {
			PuzzleType puzzleType = (PuzzleType) value;
			ScrambleSettings v = puzzleType.scramblePluginManager.getScrambleVariation(puzzleType);
			if(column == 0) { //puzzle type
				val = "<html><b>" + puzzleType.getVariationName() + "</b>" + "<html>";
			} else if(column == 1) { //scramble length
				val = "" + v.getLength();
			} else if(column == 2) { //generator group
				val = scramblePluginManager.getScrambleVariation(puzzleType).getGeneratorGroup();
			} else if(column == 3) { //ra 0
				val = getTrimmedState(puzzleType, RollingAverageOf.OF_5);
			} else if(column == 4) { //ra 1
				val = getTrimmedState(puzzleType, RollingAverageOf.OF_12);
			}
		}
		return new JLabel(val, SwingConstants.CENTER);
	}

	@NotNull
	private String getTrimmedState(PuzzleType puzzleType, RollingAverageOf raOf) {
		return puzzleType.getRASize(raOf) + " " + (puzzleType.isTrimmed(raOf) ? "Trimmed" : "Untrimmed");
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value instanceof PuzzleType) {
			puzzleType = ((PuzzleType) value);
		} else {
			puzzleType = scramblePluginManager.getPuzzleTypeByString(value.toString());
		}
		editingColumn = column;
		if(column == 0) //puzzleType
			return getPuzzleTypePanel(puzzleType);
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
	
	private JPanel getGeneratorPanel(PuzzleType sc) {
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.LINE_AXIS));
		temp.add(generator = new JTextField(sc.scramblePluginManager.getScrambleVariation(sc).getGeneratorGroup(), 6));
		generator.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		disabledComponents = new ArrayList<>();
		listenToContainer(temp);
		return temp;
	}

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

	private JPanel getPuzzleTypePanel(PuzzleType puzzleType) {
		JPanel customPanel = new JPanel();
		customPanel.setLayout(new BoxLayout(customPanel, BoxLayout.LINE_AXIS));
		puzzleTypeComboBox = new PuzzleTypeComboBox(scramblePluginManager, configuration);
		puzzleTypeComboBox.addItem(puzzleType);
		puzzleTypeComboBox.setMaximumRowCount(configuration.getInt(VariableKey.SCRAMBLE_COMBOBOX_ROWS));
		puzzleTypeComboBox.setSelectedItem(puzzleType.scramblePluginManager.getScrambleVariation(puzzleType));
		puzzleTypeComboBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
				scramblePluginManager.setScrambleSettings(this.puzzleType, (ScrambleSettings) puzzleTypeComboBox.getSelectedItem());
		});
		puzzleTypeComboBox.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.selectvariation"));
		customPanel.add(puzzleTypeComboBox);

		String originalFieldText = puzzleType.getVariationName();
		customField = new JTextField(originalFieldText, 15);
		customField.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.specifycustomization"));
		customPanel.add(customField);

		disabledComponents = new ArrayList<>();
		listenToContainer(customPanel);

		return customPanel;
	}

	private JPanel getLengthPanel(PuzzleType custom) {
		JPanel lengthPanel = new JPanel();
		lengthPanel.setLayout(new BoxLayout(lengthPanel, BoxLayout.LINE_AXIS));
		puzzleType = custom;
		scramLength = new JSpinner(new SpinnerNumberModel(Math.max(custom.scramblePluginManager.getScrambleVariation(custom).getLength(), 0), 0, null, 1));
		scramLength.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.specifylength"));
		((JSpinner.DefaultEditor) scramLength.getEditor()).getTextField().setColumns(3);
		lengthPanel.add(scramLength);

		JButton resetButton = new JButton("X");
		resetButton.setEnabled(false);
		resetButton.setToolTipText(StringAccessor.getString("ScrambleCustomizationListModel.resetlength"));
		resetButton.setFocusable(false);
		resetButton.setFocusPainted(false);
		resetButton.setMargin(new Insets(0, 0, 0, 0));
		resetButton.addActionListener(
				e -> scramLength.setValue(ScrambleSettings.getScrambleLength(
						puzzleType.getScramblePlugin(),
						puzzleType.getVariationName(),
						configuration, true)));
		resetButton.putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
		resetButton.putClientProperty(SubstanceLookAndFeel.BUTTON_SIDE_PROPERTY, new SubstanceConstants.Side[] { SubstanceConstants.Side.LEFT });
		lengthPanel.add(resetButton);
		disabledComponents = new ArrayList<>();
		listenToContainer(lengthPanel);
		return lengthPanel;
	}

	private void listenToContainer(Component c) {
		c.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				for(Component c : disabledComponents) {
					c.setEnabled(true);
				}
			}
		});
		c.setEnabled(false);
		disabledComponents.add(c);
		if(c instanceof Container) {
			Container container = (Container) c;
			for(Component c2 : container.getComponents())
				listenToContainer(c2);
		}
	}

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
		switch (editingColumn) {
			case 1:  //length
				scramblePluginManager.getScrambleVariation(puzzleType).setLength((Integer) scramLength.getValue());
				break;
			case 2:  //generator
				scramblePluginManager.setScrambleSettings(puzzleType, puzzleType.scramblePluginManager.getScrambleVariation(puzzleType).withGeneratorGroup(generator.getText()));
				break;
			case 3:  //ra 0
				puzzleType.setRA(RollingAverageOf.OF_5, (Integer) raSize.getValue(), trimmed.isSelected());
				break;
			case 4:  //ra 1
				puzzleType.setRA(RollingAverageOf.OF_12, (Integer) raSize.getValue(), trimmed.isSelected());
				break;
		}
		listener.editingStopped(null);
		return true;
	}
}
