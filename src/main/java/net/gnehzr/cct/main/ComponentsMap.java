package net.gnehzr.cct.main;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
* <p>
* <p>
* Created: 13.01.2015 1:31
* <p>
*
* @author OneHalf
*/
class ComponentsMap implements Iterable<JComponentAndBorder> {

    private HashMap<String, JComponentAndBorder> componentMap = new HashMap<>();

    public ComponentsMap() {}

    public JComponent getComponent(String name) {
        if(!componentMap.containsKey(name.toLowerCase())) {
            return null;
        }
        return componentMap.get(name.toLowerCase()).c;
    }

    public void put(@NotNull String name, @NotNull JComponent component) {
        componentMap.put(name.toLowerCase(), new JComponentAndBorder(component));
    }

    @Override
    public Iterator<JComponentAndBorder> iterator() {
        return new ArrayList<>(componentMap.values()).iterator();
    }
}
