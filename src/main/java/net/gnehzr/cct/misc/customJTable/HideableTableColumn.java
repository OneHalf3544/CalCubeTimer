package net.gnehzr.cct.misc.customJTable;

import javax.swing.table.TableColumn;

/**
* <p>
* <p>
* Created: 23.01.2015 1:29
* <p>
*
* @author OneHalf
*/
class HideableTableColumn {
    TableColumn col;
    boolean isVisible;
    int viewIndex;
    int modelIndex;

    public HideableTableColumn(TableColumn col, boolean isVisible, int modelIndex, int viewIndex) {
        this.col = col;
        this.isVisible = isVisible;
        this.modelIndex = modelIndex;
        this.viewIndex = viewIndex;
    }
    public TableColumn getColumn() {
        return col;
    }
    public boolean isVisible() {
        return isVisible;
    }
    public int getModelIndex() {
        return modelIndex;
    }
    public int getViewIndex() {
        return viewIndex;
    }
    public String toString() {
        return viewIndex+"="+isVisible;
    }
}
