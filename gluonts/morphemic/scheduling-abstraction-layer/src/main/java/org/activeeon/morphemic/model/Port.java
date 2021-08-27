package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "PORT")
public class Port implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    @Column(name = "PORT_ID")
    private int portId;

    @Column(name = "VALUE")
    private Integer value;

    @Column(name = "REQUESTED_NAME")
    private String requestedName;

    public Port(Integer value) {
        if ((value == -1) || (value >= 0 && value <= 65535)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(String.format("Invalid port value provided: %d", value));
        }
    }
}
