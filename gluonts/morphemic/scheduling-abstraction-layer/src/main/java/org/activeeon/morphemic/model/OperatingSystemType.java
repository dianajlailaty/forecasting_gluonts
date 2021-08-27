package org.activeeon.morphemic.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Embeddable
public class OperatingSystemType {
    @Column(name = "OPERATING_SYSTEM_FAMILY")
    private String operatingSystemFamily;

    @Column(name = "OPERATING_SYSTEM_VERSION")
    private float operatingSystemVersion;
}
