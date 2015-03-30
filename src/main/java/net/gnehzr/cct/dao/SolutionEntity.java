package net.gnehzr.cct.dao;

import javax.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * <p>
 * Created: 07.02.2015 10:48
 * <p>
 *
 * @author OneHalf
 */
@Entity
@Table
public class SolutionEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private LocalDateTime solveStart;

    @Column
    private Duration solveTime;

    @Column
    private String scramble;

    @Column
    private String pluginName;

    @Column
    private String variationName;

    @Column
    private String comment;

    @OneToMany(cascade = CascadeType.ALL)
    private List<SplitTimesEntity> splits;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getSolveStart() {
        return solveStart;
    }

    public void setSolveStart(LocalDateTime solveStart) {
        this.solveStart = solveStart;
    }

    public Duration getSolveTime() {
        return solveTime;
    }

    public void setSolveTime(Duration solveTime) {
        this.solveTime = solveTime;
    }

    public String getScramble() {
        return scramble;
    }

    public void setScramble(String scramble) {
        this.scramble = scramble;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getVariationName() {
        return variationName;
    }

    public void setVariationName(String variationName) {
        this.variationName = variationName;
    }

    public List<SplitTimesEntity> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitTimesEntity> splits) {
        this.splits = splits;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
