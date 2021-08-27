package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the credentials used to authenticate with a cloud
 */
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class CloudCredential implements Serializable {
    @Column(name = "USER")
    @JsonProperty("user")
    private String user = null;

    @Column(name = "SECRET")
    @JsonProperty("secret")
    private String secret = null;

    public CloudCredential user(String user) {
        this.user = user;
        return this;
    }

    /**
     * Username for authentication at the cloud provider's API
     * @return user
     **/
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public CloudCredential secret(String secret) {
        this.secret = secret;
        return this;
    }

    /**
     * Secret (e.g. Password) for authentication at the cloud provider's API
     * @return secret
     **/
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudCredential cloudCredential = (CloudCredential) o;
        return Objects.equals(this.user, cloudCredential.user) &&
                Objects.equals(this.secret, cloudCredential.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, secret);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CloudCredential {\n");

        sb.append("    user: ").append(toIndentedString(user)).append("\n");
        sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
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

