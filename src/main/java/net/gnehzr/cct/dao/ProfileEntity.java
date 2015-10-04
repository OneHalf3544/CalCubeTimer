package net.gnehzr.cct.dao;

import javax.persistence.*;
import java.util.List;

/**
* <p>
* <p>
* Created: 30.01.2015 8:16
* <p>
*
* @author OneHalf
*/
@Entity(name = "PROFILE")
public class ProfileEntity {

    @Id
    @GeneratedValue
    private Long profileId;

    @Column
    private Long lastSessionId;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(orphanRemoval = true)
    private List<ConfigEntity> configEntity;

    @OneToMany(orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn
    private List<SessionEntity> sessionEntities;


    public ProfileEntity() {
    }

    public ProfileEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLastSessionId() {
        return lastSessionId;
    }

    public void setLastSessionId(Long lastSessionId) {
        this.lastSessionId = lastSessionId;
    }

    public List<ConfigEntity> getConfigEntity() {
        return configEntity;
    }

    public void setConfigEntity(List<ConfigEntity> configEntity) {
        this.configEntity = configEntity;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long id) {
        this.profileId = id;
    }

    public List<SessionEntity> getSessionEntities() {
        return sessionEntities;
    }

    public void setSessionEntities(List<SessionEntity> sessionEntities) {
        this.sessionEntities = sessionEntities;
    }
}
