package net.gnehzr.cct.statistics;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
* <p>
* <p>
* Created: 08.11.2014 14:12
* <p>
*
* @author OneHalf
*/
public class SolveType {
    private static final HashMap<String, SolveType> SOLVE_TYPES = new HashMap<String, SolveType>();
    public static SolveType createSolveType(String desc) throws Exception {
        if(desc.isEmpty() || desc.indexOf(',') != -1)
            throw new Exception(StringAccessor.getString("SolveTime.invalidtype"));
        if(SOLVE_TYPES.containsKey(desc.toLowerCase()))
            return SOLVE_TYPES.get(desc);
        return new SolveType(desc);
    }
    public static SolveType getSolveType(String name) {
        return SOLVE_TYPES.get(name.toLowerCase());
    }
    public static Collection<SolveType> getSolveTypes(boolean defaults) {
        ArrayList<SolveType> types = new ArrayList<SolveType>(SOLVE_TYPES.values());
        String[] tags = Configuration.getStringArray(VariableKey.SOLVE_TAGS, defaults);
        for(int c = tags.length - 1; c >= 0; c--) {
            String tag = tags[c];
            int ch;
            for(ch = 0; ch < types.size(); ch++) {
                if(types.get(ch).desc.equalsIgnoreCase(tag)) {
                    types.add(0, types.remove(ch));
                    break;
                }
            }
            if(ch == types.size()) { //we didn't find the tag, so we'll have to create it
                try {
                    types.add(createSolveType(tag));
                } catch(Exception e) {}
            }
        }
        return types;
    }
    public static void remove(SolveType type) {
        SOLVE_TYPES.remove(type.desc.toLowerCase());
    }
    public static final SolveType DNF = new SolveType("DNF");
    public static final SolveType PLUS_TWO = new SolveType("+2");
    private String desc;
    private SolveType(String desc) {
        this.desc = desc;
        SOLVE_TYPES.put(desc.toLowerCase(), this);
    }
    public void rename(String newDesc) {
        SOLVE_TYPES.remove(desc.toLowerCase());
        desc = newDesc;
        SOLVE_TYPES.put(desc.toLowerCase(), this);
    }
    public String toString() {
        return desc;
    }
    public boolean isIndependent() {
        return this == DNF || this == PLUS_TWO;
    }
    public boolean isSolved() {
        return this != DNF;
    }
}
