package net.gnehzr.cct.dao;

import javax.persistence.*;
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

    @OneToMany
    private List<SolutionEntity> solutions;

    @Column
    private String profileId;

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

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
