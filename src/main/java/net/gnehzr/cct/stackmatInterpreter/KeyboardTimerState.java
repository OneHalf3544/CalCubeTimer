package net.gnehzr.cct.stackmatInterpreter;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;

/**
 * <p>
 * <p>
 * Created: 04.10.2015 13:42
 * <p>
 *
 * @author OneHalf
 */
public class KeyboardTimerState extends TimerState {

    private final Optional<InspectionState> inspectionState;

    public KeyboardTimerState(@NotNull Duration time, Optional<InspectionState> inspectionState) {
        super(time);
        this.inspectionState = inspectionState;
    }

    @Override
    public boolean isInspecting() {
        return inspectionState.isPresent();
    }

    public Optional<InspectionState> getInspectionState() {
        return inspectionState;
    }

}
