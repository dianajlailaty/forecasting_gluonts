package org.activeeon.morphemic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "PA_CLOUD")
public class PACloud implements Serializable {
    @Id
    @Column(name = "CLOUD_ID")
    private String cloudID;

    @Column(name = "NODE_SOURCE_NAME_PREFIX")
    private String nodeSourceNamePrefix;

    @Column(name = "CLOUD_PROVIDER_NAME")
    private String cloudProviderName;

    @Column(name = "CLOUD_TYPE")
    @Enumerated(EnumType.STRING)
    private CloudType cloudType;

    @Column(name = "SECURITY_GROUP")
    private String securityGroup;

    @Column(name = "ENDPOINT")
    private String endpoint;

    @Column(name = "SCOPE_PREFIX")
    private String scopePrefix;

    @Column(name = "SCOPE_VALUE")
    private String scopeValue;

    @Column(name = "IDENTITY_VERSION")
    private String identityVersion;

    @Column(name = "DUMMY_INFRASTRUCTURE_NAME")
    private String dummyInfrastructureName;

    @Column(name = "DEFAULT_NETWORK")
    private String defaultNetwork;

    @Column(name = "BLACKLIST")
    private String blacklist;

    @Column(name = "DEPLOYED_REGIONS")
    @ElementCollection(targetClass=String.class)
    private Map<String, String> deployedRegions;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REFRESH)
    private List<Deployment> deployments;

    @OneToOne
    private Credentials credentials;

    public void addDeployment(Deployment deployment) {
        if (deployments==null){
            deployments = new LinkedList<>();
        }
        deployments.add(deployment);
    }

    public void addDeployedRegion(String region, String imageProviderId) {
        if (deployedRegions==null){
            deployedRegions = new HashMap<>();
        }
        deployedRegions.put(region, imageProviderId);
    }

    public Boolean isRegionDeployed(String region) {
        return deployedRegions.containsKey(region);
    }

    @Override
    public String toString() {
        return "PACloud{" +
                "cloudID='" + cloudID + '\'' +
                ", nodeSourceNamePrefix='" + nodeSourceNamePrefix + '\'' +
                ", cloudProviderName='" + cloudProviderName + '\'' +
                ", securityGroup='" + securityGroup + '\'' +
                ", dummyInfrastructureName='" + dummyInfrastructureName + '\'' +
                ", deployedRegions=" + deployedRegions +
                '}';
    }
}
