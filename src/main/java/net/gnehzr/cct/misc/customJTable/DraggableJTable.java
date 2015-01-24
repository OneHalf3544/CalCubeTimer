package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.Utils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class DraggableJTable extends JTable implements MouseMotionListener, ActionListener {

	private static final Logger LOG = Logger.getLogger(DraggableJTable.class);

	private final Configuration configuration;
	String addText;
	private boolean ignoreMoving;

	private JTableHeader headers;
	private List<? extends SortKey> defaultSort;

	private SelectionListener selectionListener;
	private DraggableJTableModel model;
	Vector<HideableTableColumn> cols;
	private int fromRow;

	//You must set any editors or renderers before setting this table's model
	//because the preferred size is computed inside setModel()
	public DraggableJTable(Configuration configuration, boolean draggable, final boolean columnChooser) {
		this.configuration = configuration;
		MouseListener mouselistener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					int row = DraggableJTable.this.rowAtPoint(e.getPoint());
					if (selectionListener != null) {
						selectionListener.rowSelected(row);
						DraggableJTable.this.repaint();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getSource() == this)
					fromRow = rowAtPoint(e.getPoint());
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

		};
		this.addMouseListener(mouselistener);
		if(draggable) {
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.addMouseMotionListener(this);
		}
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if(keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
					deleteSelectedRows(true);
				}
			}
		});
		this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		this.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
		headers = new JTableHeader() {
			@Override
			public String getToolTipText(MouseEvent event) {
				int col = convertColumnIndexToModel(columnAtPoint(event.getPoint()));
				String tip = getColumnName(col);
				if(columnChooser)
					tip += "<br>" + StringAccessor.getString("DraggableJTable.columntooltip");
				return "<html>" + tip + "</html>";
			}
		};
		setTableHeader(headers);
		if(columnChooser) {
			headers.addMouseListener(mouselistener);
		}
		//need to override the DefaultTableColumnModel's moveColumn()
		//in order to catch mouse dragging, the JTable's moveColumn()
		//won't catch that stuff
		//this needs to be outside the above if statement so the header is visible
		setColumnModel(new DefaultTableColumnModel() {
			@Override
			public void moveColumn(int fromIndex, int toIndex) {
				HideableTableColumn col = getHideableTableColumn(getColumnModel().getColumn(fromIndex));
				if(col == null)
					return;
				int from = col.viewIndex;
				col = getHideableTableColumn(getColumnModel().getColumn(toIndex));
				if(col == null)
					return;
				int to = col.viewIndex;
				moveHideableColumn(from, to);
				super.moveColumn(fromIndex, toIndex);
			}
		});
		refreshStrings(null);
	}
	
	@Override
	public String getToolTipText(@NotNull MouseEvent event) {
		return model.getToolTip(convertRowIndexToModel(rowAtPoint(event.getPoint())), convertColumnIndexToModel(columnAtPoint(event.getPoint())));
	}

	//this will refresh any draggablejtable specific strings
	//and the addtext for the editable row at bottom
	public void refreshStrings(String addText) {
		this.addText = addText;
	}

	public void promptForNewRow() {
		editCellAt(model.getRowCount() - 1, 0);
	}

	@Override
	public boolean editCellAt(int row, int column) {
		boolean temp = super.editCellAt(row, column);
		getEditorComponent().requestFocusInWindow();
		return temp;
	}

	public Vector<HideableTableColumn> getAllColumns() {
		return cols;
	}

	private HideableTableColumn getHideableTableColumn(int viewIndex) {
		for(HideableTableColumn c : cols) {
			if(viewIndex == c.viewIndex) { 
				return c;
			}
		}
		return null;
	}
	HideableTableColumn getHideableTableColumn(TableColumn col) {
		for(HideableTableColumn c : cols) {
			if(col == c.col) { 
				return c;
			}
		}
		return null;
	}

	public void setColumnVisible(int column, boolean visible) {
		boolean isVisible = isColumnVisible(column);
		if(isVisible == visible)
			return;
		HideableTableColumn hideCol = cols.get(column);
		ignoreMoving = true;
		if(!visible) {
			removeColumn(hideCol.col);
		} else {
			addColumn(hideCol.col);  //this appends the column to the end of the view
			int trueView = hideableModelToView(hideCol.viewIndex);
			if(trueView < getColumnModel().getColumnCount() - 1) { //this moves the column to where it belongs
				moveColumn(getColumnModel().getColumnCount() - 1, trueView);
			}
		}
		ignoreMoving = false;
		hideCol.isVisible = visible;
	}
	//subtracts away the invisible columns
	private int hideableModelToView(int hideableModel) {
		int i = hideableModel;
		for(HideableTableColumn htc : cols) {
			if(!htc.isVisible && htc.viewIndex < hideableModel) {
				i--;
			}
		}
		return i;
	}
	public void setColumnOrdering(Integer[] viewIndices) {
		if(viewIndices == null)
			return;
		for(int ch = 0; ch < viewIndices.length; ch++) {
			int modelIndex = indexOfMax(viewIndices);
			int viewIndex = viewIndices[modelIndex];
			viewIndices[modelIndex] = -1; //indicate we're done with it
			HideableTableColumn col = cols.get(modelIndex);
			moveColumn(col.viewIndex, viewIndex);
		}
	}
	private int indexOfMax(Integer[] searchMe) {
		int max = -1;
		int index = -1;
		for(int ch = 0; ch < searchMe.length; ch++) {
			int val = searchMe[ch];
			if(val > max) {
				max = val;
				index = ch;
			}
		}
		return index;
	}
	public boolean isColumnVisible(int column) {
		return cols.get(column).isVisible;
	}
	public void refreshColumnNames() {
		for(int c = 0; c < model.getColumnCount(); c++) {
			cols.get(c).col.setHeaderValue(model.getColumnName(c));
		}
	}
	void moveHideableColumn(int from, int to) {
		if(from == to || ignoreMoving)
			return;
		if(from + 1 < to) {
			moveHideableColumn(from, from + 1);
			moveHideableColumn(from + 1, to);
		} else if(from - 1 > to) {
			moveHideableColumn(from, from - 1);
			moveHideableColumn(from - 1, to);
		} else { //from +-1 == to
			HideableTableColumn newCol = getHideableTableColumn(to); //getHideableTableColumn(getColumnModel().getColumn(to));
			HideableTableColumn oldCol = getHideableTableColumn(from); //getHideableTableColumn(getColumnModel().getColumn(from));
			//swapping newCol and oldCol viewIndices
			int temp = newCol.viewIndex;
			newCol.viewIndex = oldCol.viewIndex;
			oldCol.viewIndex = temp;
		}
	}

	@Override
	public void setModel(@NotNull TableModel tableModel) {
		if (tableModel instanceof DraggableJTableModel) {
			model = (DraggableJTableModel) tableModel;
			model = new JTableModelWrapper(this, model);
			super.setModel(model);
			computePreferredSizes(null);
			cols = new Vector<>();
			for(int ch = 0; ch < getColumnCount(); ch++) {
				cols.add(new HideableTableColumn(getColumnModel().getColumn(ch), true, ch, ch));
			}
		} else {
			super.setModel(tableModel);
		}
	}
	
	public void computePreferredSizes(String value) {
		TableColumnModel columns = this.getColumnModel();
		if(addText == null) {
			for(int ch = 0; ch < columns.getColumnCount(); ch++) {
				columns.getColumn(ch).setPreferredWidth(getRendererPreferredSize(value, ch).width + 2); //need to add a bit of space for insets
			}
			return;
		}
		Dimension rendDim = getCellRenderer(0, 0).getTableCellRendererComponent(
				this,
				addText,
				true,
				true,
				0,
				0).getPreferredSize();
		Dimension edDim = getEditorPreferredSize(addText, 0);

		this.setRowHeight(Math.max(rendDim.height, edDim.height));
		rendDim.height = 0;
		for(int ch = 1; ch < getColumnModel().getColumnCount(); ch++) {
			int edWidth = getEditorPreferredSize(null, ch).width;
			int rendWidth = getCellRenderer(0, ch).getTableCellRendererComponent(
					this,
					addText,
					true,
					true,
					0,
					ch).getPreferredSize().width;
			rendDim.width += Math.max(edWidth, rendWidth);
		}
		this.setPreferredScrollableViewportSize(rendDim);
		Container par = this.getParent();
		if(par != null) {
			par.setMinimumSize(rendDim);
		}
		
		Object render = addText;
		for(int ch = 0; ch < columns.getColumnCount(); ch++) {
			if(!model.isCellEditable(0, ch) && ch == 0) //it's probably allright to just always execute the else statement
				columns.getColumn(ch).sizeWidthToFit();
			else {
				int width = Math.max(getRendererPreferredSize(render, ch).width, getEditorPreferredSize(render, ch).width);
				columns.getColumn(ch).setPreferredWidth(width + 4); //adding 4 for the border, just a nasty fix to get things working
			}
			render = null;
		}
	}

	private Dimension getRendererPreferredSize(Object value, int col) {
		Component c = getCellRenderer(0, col).getTableCellRendererComponent(
				this,
				value,
				true,
				true,
				0,
				col);
		//c == null if the class returned by getColumnClass(0) doesn't have a constructor of 1 string
		return c == null ? new Dimension(0, 0) : c.getPreferredSize();
	}

	private Dimension getEditorPreferredSize(Object value, int col) {
		Component c = getCellEditor(0, col).getTableCellEditorComponent(
				this,
				value,
				true,
				0,
				col);
		//c == null if the class returned by getColumnClass(0) doesn't have a constructor of 1 string
		return c == null ? new Dimension(0, 0) : c.getPreferredSize();
	}
	
	public void sortByColumn(SortKey sortKey) {
		if(sortKey == null) {
			return;
		}
		defaultSort = Arrays.asList(sortKey);
		try {
			getRowSorter().setSortKeys(defaultSort);
		} catch(Exception e3) {
			LOG.trace("ignored exception", e3);
		}
	}
	//returns column + 1, negative if descending, positive if ascending
	//returns 0 if no column is sorted
	public int getSortedColumn() {
		if(getRowSorter() != null) {
			for(SortKey key : getRowSorter().getSortKeys()) {
				SortOrder so = key.getSortOrder();
				int col = key.getColumn() + 1;
				if(so == SortOrder.ASCENDING) {
					return col;
				} else if(so == SortOrder.DESCENDING) {
					return -col;
				}
			}
		}
		return 0;
	}
	@Override
	public void tableChanged(TableModelEvent event) {
		//TODO - refreshing the tooltip would be nice here
		
		List<? extends SortKey> sorts = null;
		if (getRowSorter() != null) {
			sorts = getRowSorter().getSortKeys();
		}
		if (sorts == null || sorts.isEmpty()) {
			sorts = defaultSort;
		}
		super.tableChanged(event);
		if (getRowSorter() == null) {
			return;
		}
		try{
            getRowSorter().setSortKeys(sorts);
        } catch(Exception e2) {
            LOG.trace("ignored exception", e2);
            try {
                getRowSorter().setSortKeys(defaultSort);
            } catch(Exception e3) {
                LOG.trace("ignored exception", e3);
            }
        }
	}
	
	
	public interface SelectionListener {
		public void rowSelected(int row);
	}

	public void setSelectionListener(SelectionListener sl) {
		selectionListener = sl;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(e.getSource() == this) {
			int toRow = this.getSelectedRow();
			if (toRow == -1 || fromRow == -1 || toRow == fromRow || fromRow == this.getRowCount() - 1 || toRow == this.getRowCount() - 1)
				return;
			Object element = model.getValueAt(fromRow, 0);
			model.removeRows(new int[]{fromRow});
			model.insertValueAt(element, toRow);
			fromRow = toRow;
		}
	}
	@Override
	public void mouseMoved(MouseEvent e) {}

	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		if(e.getSource() == this) {
            int row;
            if((row = rowAtPoint(e.getPoint())) != -1) {
                Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if(getSelectedRowCount() <= 1) {
                    // if right clicking on a single cell, this will select it first
                    setRowSelectionInterval(row, row);
                }
                model.showPopup(e, this, c);
            }
        } else {
            if (e.getSource() == headers) {
                JPopupMenu jPopupMenu = new JPopupMenu();
                JMenuItem jMenuItem = new JMenuItem(StringAccessor.getString("DraggableJTable.choosecolumns"));
                jMenuItem.setEnabled(false);
                jPopupMenu.add(jMenuItem);
                jPopupMenu.addSeparator();
                JCheckBoxMenuItem[] columnCheckBoxes = new JCheckBoxMenuItem[cols.size()];
                for (int ch = 1; ch < columnCheckBoxes.length; ch++) {
                    JCheckBoxMenuItem check = new JCheckBoxMenuItem(cols.get(ch).col.getHeaderValue().toString(), isColumnVisible(ch));
                    check.setActionCommand(ch + "");
                    check.addActionListener(this);
                    columnCheckBoxes[cols.get(ch).viewIndex] = check;
                }
                for (JCheckBoxMenuItem check : columnCheckBoxes) {
                    if (check != null) {
                        jPopupMenu.add(check);
                    }
                }
                jPopupMenu.show(headers, e.getX(), e.getY());
            }
        }
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JCheckBoxMenuItem check = (JCheckBoxMenuItem) e.getSource();
		int col = Integer.parseInt(e.getActionCommand());
		setColumnVisible(col, check.isSelected());
	}

	public void deleteSelectedRows(boolean prompt) {
		int[] selectedRows = this.getSelectedRows();
		String temp = "";
		for(int ch = 0; ch < selectedRows.length; ch++) {
			int row = convertRowIndexToModel(selectedRows[ch]);
			selectedRows[ch] = row;
			if (model.isRowDeletable(row)) {
				temp += ", " + model.getValueAt(row, 0);
			}
		}
		if(temp.isEmpty()) //nothing to delete
			return;
		temp = temp.substring(2);
		Arrays.sort(selectedRows);
		int choice = JOptionPane.YES_OPTION;
		if(prompt)
			choice = Utils.showYesNoDialog(getParent(),
					StringAccessor.getString("DraggableJTable.confirmdeletion") + "\n" + temp);
		if(choice == JOptionPane.YES_OPTION) {
			model.deleteRows(selectedRows);
			if(selectedRows.length > 1) {
				clearSelection();
			} else if(selectedRows[0] < model.getRowCount() - 1) {
				setRowSelectionInterval(selectedRows[0], selectedRows[0]);
			} else if(selectedRows[0] != 0) {
				int newRow = model.getRowCount() - 2;
				if(addText == null)
					newRow++;
				setRowSelectionInterval(newRow, newRow);
			}
		}
	}
	
	public void saveToConfiguration() {
		configuration.setLong(VariableKey.JCOMPONENT_VALUE(this.getName() + "_sortBy", false, configuration.getXMLGUILayout()), this.getSortedColumn());
		Integer[] ordering = new Integer[this.getAllColumns().size()];
		for(HideableTableColumn col : this.getAllColumns()) {
			int index = col.getModelIndex();
			configuration.setLong(VariableKey.JCOMPONENT_VALUE(this.getName() + index + "_width", false, configuration.getXMLGUILayout()), col.getColumn().getWidth());
			ordering[index] = col.getViewIndex();
			configuration.setBoolean(VariableKey.COLUMN_VISIBLE(this, index), col.isVisible());
		}
		configuration.setIntegerArray(VariableKey.JTABLE_COLUMN_ORDERING(this.getName()), ordering);
	}

	public void loadFromConfiguration() {
		for(HideableTableColumn htc : this.getAllColumns()) {
			int index = htc.getModelIndex();
			if(index != 0) {
				setColumnVisible(index, true);
			}
		}
		this.setColumnOrdering(configuration.getIntegerArray(VariableKey.JTABLE_COLUMN_ORDERING(this.getName()), false));
		
		for(TableColumn tc : Collections.list(this.getColumnModel().getColumns())) {
			int index = tc.getModelIndex();
			Integer width = configuration.getInt(VariableKey.JCOMPONENT_VALUE(this.getName() + index + "_width", false, configuration.getXMLGUILayout()), false);
			if(width != null) {
				tc.setPreferredWidth(width);
			}
			if(index != 0) {
				this.setColumnVisible(index, configuration.getBoolean(VariableKey.COLUMN_VISIBLE(this, index), false));
			}
		}
		this.sortByColumn(getSortKeyFromConfig());
	}

	private SortKey getSortKeyFromConfig() {
		Integer sortCol = configuration.getInt(VariableKey.JCOMPONENT_VALUE(this.getName() + "_sortBy", false, configuration.getXMLGUILayout()), false);
		if (sortCol == 0) {
			return null;
		}
		SortOrder sortOrder = sortCol < 0 ? SortOrder.DESCENDING : SortOrder.ASCENDING;
		return new SortKey(Math.abs(sortCol) - 1, sortOrder);
	}
}
