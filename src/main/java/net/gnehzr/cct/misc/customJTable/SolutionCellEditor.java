package net.gnehzr.cct.misc.customJTable;

import net.gnehzr.cct.i18n.StringAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * <p>
 * <p>
 * Created: 04.11.2015 13:29
 * <p>
 *
 * @author OneHalf
 */
public abstract class SolutionCellEditor<T> extends DefaultCellEditor {

    private static final Logger log = LogManager.getLogger(SolutionCellEditor.class);

    public final String messageTooltip;

    protected T value;

    public SolutionCellEditor(JTextField textField, String messageTooltip) {
        super(textField);
        this.messageTooltip = messageTooltip;
    }

    //TODO - http://www.pushing-pixels.org/?p=69 ?
    @Override
    public boolean stopCellEditing() {
        try {
            value = parseFromString((String) super.getCellEditorValue());
            return super.stopCellEditing();
        }
        catch (Exception e) {
            log.info("input string error: {} {}", e.getClass(), e.getMessage());
            JComponent component = (JComponent) getComponent();
            component.setBorder(new LineBorder(Color.RED));
            component.setToolTipText(e.getMessage());
            return false;
        }
    }

    protected abstract T parseFromString(String strValue) throws Exception;

    @Override
    public T getCellEditorValue() {
        return value;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.value = null;
        ((JComponent) getComponent()).setBorder(new LineBorder(Color.BLACK));
        ((JComponent) getComponent()).setToolTipText(StringAccessor.getString(messageTooltip));
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
}
