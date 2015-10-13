package net.gnehzr.cct.statistics;

/**
 * <p>
 * <p>
 * Created: 13.10.2015 9:06
 * <p>
 *
 * @author OneHalf
 */
public enum RollingAverageOf {
    OF_5("0"),
    OF_12("1");

    private final String code;

    RollingAverageOf(String code) {
        this.code = code;
    }

    public static RollingAverageOf byCode(String arg) {
        switch (arg) {
            case "0":
                return OF_5;
            case "1":
                return OF_12;
            default:
                throw new IllegalArgumentException(arg);
        }
    }

    public String getCode() {
        return code;
    }
}
