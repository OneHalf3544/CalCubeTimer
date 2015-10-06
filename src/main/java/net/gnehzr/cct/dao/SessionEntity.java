package net.gnehzr.cct.dao;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.scrambles.ScramblePluginManager;
import net.gnehzr.cct.statistics.Profile;
import net.gnehzr.cct.statistics.Session;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SolutionEntity> solutions;

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

    public ProfileEntity getProfileId() {
        return profile;
    }

    public void setProfileId(ProfileEntity profileId) {
        this.profile = profileId;
    }

    public Session toSession(Configuration configuration, ScramblePluginManager pluginManager, Profile profile) {
        return new Session(sessionStart, configuration, pluginManager.getCustomizationFromString(profile, scrambleCustomization));
    }
}
