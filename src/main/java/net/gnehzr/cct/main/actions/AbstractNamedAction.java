package net.gnehzr.cct.main.actions;

import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;

/**
 * <p>
 * Date: 25.06.2016
 *
 * @author OneHalf
 */
public abstract class AbstractNamedAction extends AbstractAction {

    private final String actionCode;

    protected AbstractNamedAction(String actionCode) {
        this.actionCode = actionCode;
    }

    protected AbstractNamedAction(String actionCode, String name) {
        super(name);
        this.actionCode = actionCode;
    }

    public String getActionCode() {
        return actionCode;
    }
}
