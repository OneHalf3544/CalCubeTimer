package net.gnehzr.cct.misc.dynamicGUI;

import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 19.10.2015 22:46
 * <p>
 *
 * @author OneHalf
 */
class DStringPart {

    enum Type {
        RAW_TEXT,
        I18N_TEXT,
        STATISTICS_TEXT,
        CONFIGURATION_TEXT,
    }

    DStringPart(String string, Type type) {
        this.string = string;
        this.type = type;
    }

    private final String string;
    private final Type type;

    public String getString() {
        return string;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DStringPart that = (DStringPart) o;
        return Objects.equals(string, that.string) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string, type);
    }

    @Override
    public String toString() {
        return "DStringPart{" +
                "string='" + string + '\'' +
                ", type=" + type +
                '}';
    }
}
