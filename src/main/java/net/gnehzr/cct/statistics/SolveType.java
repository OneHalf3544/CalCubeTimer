package net.gnehzr.cct.statistics;

import net.gnehzr.cct.i18n.StringAccessor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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

    private static final Logger LOG = Logger.getLogger(SolveType.class);

    private static final Map<String, SolveType> SOLVE_TYPES = new HashMap<>();

    public static void remove(SolveType type) {
        SOLVE_TYPES.remove(type.desc.toLowerCase());
    }

    public static final SolveType DNF = new SolveType("DNF");

    public static final SolveType PLUS_TWO = new SolveType("+2");

    private String desc;

    public static SolveType createSolveType(String desc) {
        if(!isValidTagName(desc)) {
            throw new IllegalArgumentException(StringAccessor.getString("SolveTime.invalidtype"));
        }
        if(SOLVE_TYPES.containsKey(desc.toLowerCase())) {
            return SOLVE_TYPES.get(desc);
        }
        return new SolveType(desc);
    }

    public static boolean isValidTagName(String desc) {
        return !desc.isEmpty() && !desc.contains(",");
    }

    public static SolveType getSolveType(String name) {
        return SOLVE_TYPES.get(name.toLowerCase());
    }

    public static Collection<SolveType> getSolveTypes(@NotNull List<String> solveTags) {
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
                if (isValidTagName(tag)) {
                    types.add(createSolveType(tag));
                } else {
                    LOG.info("invalid tag name: " + tag);
                }
            }
        }
        return types;
    }

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
