package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an API used by a cloud
 */
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Api implements Serializable {
    /*
    Possible values of providerName for particular CSPs:
        AWS EC2 = "aws-ec2"
        Openstack = "openstack4j"
        Google = "google-compute-engine"
        Azure = "azure"
        Oktawave = "oktawave"
    */
    @Column(name = "PROVIDER_NAME")
    @JsonProperty("providerName")
    private String providerName = null;

    public Api providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    /**
     * Name of the API provider, maps to a driver
     * @return providerName
     **/
    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Api api = (Api) o;
        return Objects.equals(this.providerName, api.providerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Api {\n");

        sb.append("    providerName: ").append(toIndentedString(providerName)).append("\n");
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

