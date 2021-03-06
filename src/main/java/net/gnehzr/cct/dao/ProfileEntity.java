package net.gnehzr.cct.dao;

import javax.persistence.*;
import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private LocalDateTime lastLoad;

    @OneToMany(orphanRemoval = true)
    private List<ConfigEntity> configEntity;

    @OneToMany(orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "profile")
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

    public LocalDateTime getLastLoad() {
        return lastLoad;
    }

    public void setLastLoad(LocalDateTime lastLoad) {
        this.lastLoad = lastLoad;
    }
}
