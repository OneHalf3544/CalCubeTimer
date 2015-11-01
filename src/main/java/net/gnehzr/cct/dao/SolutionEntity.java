package net.gnehzr.cct.dao;

import com.google.common.base.Throwables;
import net.gnehzr.cct.scrambles.InvalidScrambleException;
import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.statistics.Solution;
import net.gnehzr.cct.statistics.SolveTime;

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
    @Column(name = "SOLUTION_ID")
    private Long id;

    @Column
    private LocalDateTime solveStart;

    @Column
    private Duration solveTime;

    @Column(length = 2000)
    private String scramble;

    @Column
    private String comment;

    @OneToMany(cascade = CascadeType.ALL)
    private List<SplitTimesEntity> splits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID")
    private SessionEntity session;

    public SolutionEntity() {
    }

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

    public SolutionEntity withSession(SessionEntity session) {
        this.session = session;
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

    public Solution toSolution(PuzzleType puzzleType) {
        try {
            Solution result = new Solution(new SolveTime(getSolveTime()), puzzleType.importScramble(getScramble()));
            result.setSolutionId(getId());
            result.setComment(getComment());
            return result;
        } catch (InvalidScrambleException e) {
            throw Throwables.propagate(e);
        }
    }
}
