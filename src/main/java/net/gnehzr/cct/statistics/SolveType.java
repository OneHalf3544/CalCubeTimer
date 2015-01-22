package net.gnehzr.cct.statistics;

import net.gnehzr.cct.i18n.StringAccessor;

import java.util.*;

/**
* <p>
* <p>
* Created: 08.11.2014 14:12
* <p>
*
* @author OneHalf
*/
public class SolveType {

    private static final Map<String, SolveType> SOLVE_TYPES = new HashMap<>();

    public static SolveType createSolveType(String desc) throws Exception {
        if(desc.isEmpty() || desc.contains(",")) {
            throw new Exception(StringAccessor.getString("SolveTime.invalidtype"));
        }
        if(SOLVE_TYPES.containsKey(desc.toLowerCase())) {
            return SOLVE_TYPES.get(desc);
        }
        return new SolveType(desc);
    }

    public static SolveType getSolveType(String name) {
        return SOLVE_TYPES.get(name.toLowerCase());
    }

    public static Collection<SolveType> getSolveTypes(List<String> solveTags) {
        ArrayList<SolveType> types = new ArrayList<>(SOLVE_TYPES.values());
        for(int c = solveTags.size() - 1; c >= 0; c--) {
            String tag = solveTags.get(c);
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
