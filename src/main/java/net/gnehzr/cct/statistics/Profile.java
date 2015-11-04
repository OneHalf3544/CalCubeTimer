package net.gnehzr.cct.statistics;

import net.gnehzr.cct.dao.ProfileEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class Profile {

    private Long id;
    private String name;

    private String newName;
    private Long lastSessionId;

    //constructors are private because we want only 1 instance of a profile
    //pointing to a given database
    public Profile(Long id, String name, Long lastSessionId) {
        this.id = id;
        this.name = name;
        this.lastSessionId = lastSessionId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void discardRename() {
        newName = null;
    }

    public ProfileEntity toEntity() {
        ProfileEntity profileEntity = new ProfileEntity(name);
        profileEntity.setProfileId(id);
        profileEntity.setLastSessionId(getLastSessionId());
        profileEntity.setLastLoad(LocalDateTime.now());
        return profileEntity;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Profile)) {
            return false;
        }
        return Objects.equals(this.getName(), ((Profile) o).getName());
    }

    //this is the only indication to the user of whether we successfully loaded the database file
    @Override
    public String toString() {
        return (newName != null ? newName : name);// + (isLoginDisabled() ? StringAccessor.getString("Profile.loggingdisabled") : "");
    }

    public void renameTo(String newName) {
        this.newName = newName;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getNewName() {
        return newName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastSessionId() {
        return lastSessionId;
    }


    public void setLastSessionId(Long sessionId) {
        this.lastSessionId = sessionId;
    }
}
