package net.gnehzr.cct.misc.dynamicGUI;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.i18n.MessageAccessor;
import net.gnehzr.cct.statistics.RollingAverageOf;
import net.gnehzr.cct.statistics.SessionsList;

import javax.annotation.Nonnull;
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

    DStringPart(@Nonnull String string, @Nonnull Type type) {
        this.string = Objects.requireNonNull(string);
        this.type = type;
    }

    private final String string;
    private final Type type;

    public String getString() {
        return string;
    }

    public String toString(DynamicString dynamicString, MessageAccessor accessor, RollingAverageOf num,
                           SessionsList sessions, Configuration configuration) {
        switch (getType()) {
            case I18N_TEXT:
                return Objects.requireNonNull(accessor).getString(string);
            case STATISTICS_TEXT:
                return dynamicString.getReplacement(this, num, sessions);
            case CONFIGURATION_TEXT:
                return configuration.getString(string);
            case RAW_TEXT:
                return string;
            default:
                throw new IllegalArgumentException(getType().toString());
        }
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
