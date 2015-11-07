package net.gnehzr.cct.scrambles.crosssolver;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

/**
 * Created: 07.11.2015 13:57
 * <p>
 *
 * @author OneHalf
 */
public class CrossSolution {
    private final List<Turn> turnList;

    public CrossSolution(List<Turn> turnList) {
        checkArgument(turnList.stream().allMatch(Objects::nonNull));
        this.turnList = turnList;
    }

    public List<Turn> getTurnList() {
        return turnList;
    }

    public CrossSolution withTurnBefore(@NotNull Turn turn) {
        return new CrossSolution(ImmutableList.<Turn>builder()
                .add(turn)
                .addAll(turnList)
                .build());
    }


    public String toString(Rotate setup_rotations, Rotate r) {
        Rotate unsetup = setup_rotations.invert();
        Rotate rotation = r.invert();

        return rotation.getDesc() + getTurnList().stream()
                .map(turn -> unsetup.getOGTurn(rotation.getOGTurn(turn)).toString())
                .collect(joining(" "));
    }
}
