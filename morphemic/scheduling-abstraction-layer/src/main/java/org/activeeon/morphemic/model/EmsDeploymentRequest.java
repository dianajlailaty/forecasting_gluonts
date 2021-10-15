package org.activeeon.morphemic.model;

import org.ow2.proactive.scheduler.common.task.TaskVariable;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Entity
@Table(name = "EMSDEPLOYMENTREQUEST")
public class EmsDeploymentRequest implements Serializable {

    public enum TargetType {
        vm("IAAS"),
        container("PAAS"),
        edge("EDGE"),
        baremetal("BYON"),
        faas("FAAS");

        TargetType(String adapterVal) {
            this.adapterVal = adapterVal;
        }

        String adapterVal;

        public static TargetType fromValue(String text) {
            for (TargetType b : TargetType.values()) {
                if (String.valueOf(b.adapterVal).equals(text.toUpperCase(Locale.ROOT))) {
                    return b;
                }
            }
            return null;
        }

    }

    public enum TargetProvider {
        // Amazon Web Service Elastic Compute Cloud
        AWSEC2("aws-ec2"),
        // Azure VM
        AZUREVM("azure"),
        // Google CLoud Engine
        GCE("gce"),
        // OpenStack NOVA
        OPENSTACKNOVA("openstack"),
        // BYON, to be used for on-premise baremetal & Edge
        BYON("byon");

        TargetProvider(String upperwareVal) {
            this.upperwareValue = upperwareVal;
        }

        String upperwareValue;

        public static TargetProvider fromValue(String text) {
            for (TargetProvider b : TargetProvider.values()) {
                if (String.valueOf(b.upperwareValue).equals(text.toUpperCase(Locale.ROOT))) {
                    return b;
                }
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    @Column(name = "AUTHORIZATIONBEARER")
    private String authorizationBearer;

    @Column(name = "BAGUETTEIP")
    private String baguetteIp;

    @Column(name = "BAGUETTEPORT")
    private int baguette_port;

    @Column(name = "TARGETOS")
    @Enumerated(EnumType.STRING)
    private OperatingSystemFamily targetOs;

    @Column(name = "TARGETTYPE")
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(name = "TARGETNAME")
    private String targetName;

    @Column(name = "TARGETPROVIDER")
    @Enumerated(EnumType.STRING)
    private TargetProvider targetProvider;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "ISUSINGHTTP")
    private boolean isUsingHttps;

    @Column(name = "NODEID")
    private String nodeId;


    private EmsDeploymentRequest(String authorizationBearer, String baguetteIp, int baguette_port, OperatingSystemFamily targetOs,
                                TargetType targetType, String targetName,
                                TargetProvider targetProvider, String location, boolean isUsingHttps, String id) {
        this.authorizationBearer = authorizationBearer;
        this.baguetteIp = baguetteIp;
        this.baguette_port = baguette_port;
        this.targetOs = targetOs;
        this.targetType = targetType;
        this.targetName = targetName;
        this.targetProvider = targetProvider;
        this.location = location;
        this.isUsingHttps = isUsingHttps;
        this.nodeId = id;
    }

    public EmsDeploymentRequest(String authorizationBearer, String baguetteIp, int baguette_port, OperatingSystemFamily targetOs,
                                String targetType, String targetName,
                                TargetProvider targetProvider, String location, boolean isUsingHttps, String id) {
        this.authorizationBearer = authorizationBearer;
        this.baguetteIp = baguetteIp;
        this.baguette_port = baguette_port;
        this.targetOs = targetOs;
        this.targetType = TargetType.fromValue(targetType);
        this.targetName = targetName;
        this.targetProvider = targetProvider;
        this.location = location;
        this.isUsingHttps = isUsingHttps;
        this.nodeId = id;
    }

    /**
     * Provide the variable Array to be used in the EMS deployment workflow, structured to be ysed with the submit PA API
     * @return The structured map.
     */
    public Map<String, TaskVariable> getWorkflowMap() {
        Map<String,TaskVariable> result =  new HashMap<>();
        result.put("authorization_bearer", new TaskVariable("authorization_bearer",this.authorizationBearer,"",false));
        result.put("baguette_ip", new TaskVariable("baguette_ip",baguetteIp.toString(),"",false));
        result.put("baguette_port",new TaskVariable("baguette_port",String.valueOf(baguette_port),"",false));
        result.put("target_operating_system",new TaskVariable("target_operating_system",targetOs.name(),"",false));
        result.put("target_type", new TaskVariable("target_type",targetType.name(),"",false));
        result.put("target_name",new TaskVariable("target_name",targetName,"",false));
        result.put("target_provider",new TaskVariable("target_provider",targetProvider.name(),"",false));
        result.put("location", new TaskVariable("location",location,"",false));
        result.put("using_https", new TaskVariable("using_https",isUsingHttps + "","PA:Boolean",false));
        result.put("id",new TaskVariable("id", nodeId,"",false));
        return result;
    }

    public EmsDeploymentRequest clone(String nodeId) {
        EmsDeploymentRequest req = new EmsDeploymentRequest(authorizationBearer,baguetteIp,baguette_port,targetOs,targetType,targetName,targetProvider,location,isUsingHttps,nodeId);
        return req;
    }
}
