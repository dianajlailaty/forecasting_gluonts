package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Repesents the configuration of a cloud.
 */
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class CloudConfiguration implements Serializable {
    @Column(name = "NODE_GROUP")
    @JsonProperty("nodeGroup")
    private String nodeGroup = null;

    @Column(name = "PROPERTIES")
    @ElementCollection(targetClass=String.class, fetch = FetchType.EAGER)
    @JsonProperty("properties")
    private Map<String, String> properties = null;

    public CloudConfiguration nodeGroup(String nodeGroup) {
        this.nodeGroup = nodeGroup;
        return this;
    }

    /**
     * A prefix all Cloudiator managed entities will belong to.
     * @return nodeGroup
     **/
    public String getNodeGroup() {
        return nodeGroup;
    }

    public void setNodeGroup(String nodeGroup) {
        this.nodeGroup = nodeGroup;
    }

    public CloudConfiguration properties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Configuration as key-value map.
     * @return properties
     **/
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudConfiguration cloudConfiguration = (CloudConfiguration) o;
        return Objects.equals(this.nodeGroup, cloudConfiguration.nodeGroup) &&
                Objects.equals(this.properties, cloudConfiguration.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeGroup, properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CloudConfiguration {\n");

        sb.append("    nodeGroup: ").append(toIndentedString(nodeGroup)).append("\n");
        sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

