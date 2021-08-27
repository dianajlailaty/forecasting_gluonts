package org.activeeon.morphemic.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Embeddable
public class CommandsInstallation {
    @Column(name = "PREINSTALL", columnDefinition = "TEXT")
    private String preInstall;

    @Column(name = "INSTALL", columnDefinition = "TEXT")
    private String install;

    @Column(name = "POSTINSTALL", columnDefinition = "TEXT")
    private String postInstall;

    @Column(name = "PRESTART", columnDefinition = "TEXT")
    private String preStart;

    @Column(name = "START", columnDefinition = "TEXT")
    private String start;

    @Column(name = "POSTSTART", columnDefinition = "TEXT")
    private String postStart;

    @Column(name = "PRESTOP", columnDefinition = "TEXT")
    private String preStop;

    @Column(name = "STOP", columnDefinition = "TEXT")
    private String stop;

    @Column(name = "POSTSTOP", columnDefinition = "TEXT")
    private String postStop;

    @Column(name = "UPDATE_CMD", columnDefinition = "TEXT")
    private String updateCmd;

    @Embedded
    private OperatingSystemType operatingSystemType;
}
