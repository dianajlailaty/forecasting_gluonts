package org.activeeon.morphemic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "DEPLOYMENT")
public class Deployment implements Serializable {

    @Id
    @Column(name = "NODE_NAME")
    private String nodeName;

    @Column(name = "LOCATION_NAME")
    private String locationName;

    @Column(name = "IMAGE_PROVIDER_ID")
    private String imageProviderId;

    @Column(name = "HARDWARE_PROVIDER_ID")
    private String hardwareProviderId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    private EmsDeploymentRequest emsDeployment;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    private PACloud paCloud;

    @Column(name = "IS_DEPLOYED")
    private Boolean isDeployed = false;

    @Override
    public String toString() {
        return "Deployment{" +
                "nodeName='" + nodeName + '\'' +
                ", locationName='" + locationName + '\'' +
                ", imageProviderId='" + imageProviderId + '\'' +
                ", hardwareProviderId='" + hardwareProviderId + '\'' +
                '}';
    }
}
