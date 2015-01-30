package net.gnehzr.cct.dao;

import javax.persistence.Entity;
import javax.persistence.Id;

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
    private String name;

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
}
