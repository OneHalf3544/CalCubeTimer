package net.gnehzr.cct.dao;

import javax.persistence.*;

/**
* <p>
* <p>
* Created: 29.01.2015 22:28
* <p>
*
* @author OneHalf
*/
@Entity
@Table(name = "CONFIGURATION",
        uniqueConstraints = @UniqueConstraint(columnNames = {"PROFILE", "KEY"}))
public class ConfigEntity {
    @Id
    @GeneratedValue
    private Long id;
    @Column(name = "KEY", nullable = false)
    private String key;
    @Column(name = "PROFILE", nullable = false)
    private String profileName;
    @Column(name = "VALUE", length = 1000)
    private String value;

    public ConfigEntity() {
    }

    public ConfigEntity(String profileName, String key, String value) {
        this.key = key;
        this.value = value;
        this.profileName = profileName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}
