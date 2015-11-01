package net.gnehzr.cct.stackmatInterpreter;

import java.time.Duration;
import java.time.Instant;

/**
 * <p>
 * <p>
 * Created: 04.10.2015 14:29
 * <p>
 *
 * @author OneHalf
 */
public class InspectionState {

    public static final Duration INSPECTION_TIME = Duration.ofSeconds(15);
    public static final Duration FIRST_WARNING = Duration.ofSeconds(8);
    public static final Duration FINAL_WARNING = Duration.ofSeconds(12);

    private final Instant inspectionStart;
    private final Instant currentTime;


    public InspectionState(Instant inspectionStart, Instant currentTime) {
        this.inspectionStart = inspectionStart;
        this.currentTime = currentTime;
    }

    public Duration getElapsedTime() {
        return Duration.between(inspectionStart, currentTime);
    }

    public Duration getRemainingTime() {
        return INSPECTION_TIME.minus(getElapsedTime());
    }

    public boolean isDisqualification() {
        return getRemainingTime().getSeconds() <= -2;
    }

    public boolean isPenalty() {
        long seconds = getRemainingTime().getSeconds();
        return seconds <= 0 && seconds > -2;
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == InspectionState.class
                && getElapsedTime().getSeconds() == ((InspectionState) obj).getElapsedTime().getSeconds();
    }

    @Override
    public int hashCode() {
        return (int) getElapsedTime().getSeconds();
    }

    @Override
    public String toString() {
        return String.format("inspection: elapsedTime=%s, ", getElapsedTime());
    }
}
