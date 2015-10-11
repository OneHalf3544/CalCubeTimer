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

    public SolutionEntity withId(Long id) {
        this.id = id;
        return this;
    }

    public SolutionEntity withSolveStart(LocalDateTime solveStart) {
        this.solveStart = solveStart;
        return this;
    }

    public SolutionEntity withSolveTime(Duration solveTime) {
        this.solveTime = solveTime;
        return this;
    }

    public SolutionEntity withScramble(String scramble) {
        this.scramble = scramble;
        return this;
    }

    public SolutionEntity withSplits(List<SplitTimesEntity> splits) {
        this.splits = splits;
        return this;
    }

    public SolutionEntity withComment(String comment) {
        this.comment = comment;
        return this;
    }
}
