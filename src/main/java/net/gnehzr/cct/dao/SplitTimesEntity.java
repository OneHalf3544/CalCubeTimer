package net.gnehzr.cct.dao;

import javax.persistence.*;
import java.time.Duration;

/**
 * <p>
 * <p>
 * Created: 07.02.2015 12:27
 * <p>
 *
 * @author OneHalf
 */
@Entity
@Table
public class SplitTimesEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Duration duration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
