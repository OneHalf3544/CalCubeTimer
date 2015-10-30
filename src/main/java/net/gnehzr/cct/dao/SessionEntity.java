package net.gnehzr.cct.dao;

import net.gnehzr.cct.scrambles.PuzzleType;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Session;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * <p>
 * Created: 07.02.2015 12:03
 * <p>
 *
 * @author OneHalf
 */
@Entity
@Table(name = "SESSION")
public class SessionEntity {

    @Id
    @GeneratedValue
    @Column(name = "SESSION_ID")
    private Long sessionId;

    @Column
    private LocalDateTime sessionStart;

    @Column
    private String scrambleCustomization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SolutionEntity> solutions;

    @Column
    private String pluginName;

    @Column
    private String variationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFILEID")
    private ProfileEntity profile;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public List<SolutionEntity> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<SolutionEntity> solutions) {
        this.solutions = solutions;
    }

    public LocalDateTime getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(LocalDateTime sessionStart) {
        this.sessionStart = sessionStart;
    }

    public String getScrambleCustomization() {
        return scrambleCustomization;
    }

    public void setScrambleCustomization(String scrambleCustomization) {
        this.scrambleCustomization = scrambleCustomization;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
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

    public SessionEntity withPluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public SessionEntity withVariationName(String variationName) {
        this.variationName = variationName;
        return this;
    }

    public Session toSession(ScramblePluginManager pluginManager, SolutionDao solutionDao) {
        PuzzleType puzzleType = pluginManager.getPuzzleTypeByString(variationName);
        Session session = new Session(sessionStart, puzzleType, solutionDao);
        session.setSolutions(getSolutions().stream()
                .map(solutionEntity -> solutionEntity.toSolution(puzzleType))
                .collect(toList()));
        return session;
    }
}
