package net.gnehzr.cct.main;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* <p>
* <p>
* Created: 01.02.2015 15:21
* <p>
*
* @author OneHalf
*/
class IncrementalComboBoxModel implements ComboBoxModel<String> {

    private ArrayList<String> values;
    private String prefix;
    private ArrayList<String> filtered;
    private JTextField editor;

	private Object o;

	public IncrementalComboBoxModel(List<String> values, JTextField editor) {
        this.editor = editor;
        this.values = new ArrayList<>(values);
        filtered = new ArrayList<>();
        setPrefix("");
    }

	public List<String> getItems() {
        return values;
    }

	public void setPrefix(String prefix) {
        this.prefix = prefix;
        filtered.clear();
        filtered.addAll(values.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList()));
        fireDataChanged();
    }

	public void addElement(String elem) {
        if(!values.contains(elem)) {
            values.add(elem);
            setPrefix(prefix); //force a refresh
        }
    }

    @Override
    public String getElementAt(int index) {
        return filtered.get(index);
    }

	@Override
    public int getSize() {
        return filtered.size();
    }

	@Override
    public Object getSelectedItem() {
        if(o == null)
            return "";
        return o;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        editor.setText(anItem.toString());
        o = anItem;
    }
    private void fireDataChanged() {
        for(ListDataListener ldl : l)
            ldl.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, filtered.size()));
    }
    private ArrayList<ListDataListener> l = new ArrayList<>();

    @Override
    public void addListDataListener(ListDataListener ldl) {
        l.add(ldl);
    }

    @Override
    public void removeListDataListener(ListDataListener ldl) {
        l.remove(ldl);
    }
}
