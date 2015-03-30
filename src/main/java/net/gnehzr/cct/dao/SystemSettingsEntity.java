package net.gnehzr.cct.dao;

import javax.persistence.*;

/**
 * <p>
 * <p>
 * Created: 08.02.2015 16:20
 * <p>
 *
 * @author OneHalf
 */
@Entity
@Table
public class SystemSettingsEntity {

    @Id
    @GeneratedValue
    private Long settingsId;

    @Column
    private String profileOrdering;

    @OneToOne
    private ProfileEntity startupProfile;

    public Long getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(Long settings_id) {
        this.settingsId = settings_id;
    }

    public ProfileEntity getStartupProfile() {
        return startupProfile;
    }

    public void setStartupProfile(ProfileEntity startupProfile) {
        this.startupProfile = startupProfile;
    }

    public void setProfileOrdering(String profileOrdering) {
        this.profileOrdering = profileOrdering;
    }

    public String getProfileOrdering() {
        return profileOrdering;
    }
}
