package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;

/**
 * Node candidate environment
 */
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Environment implements Serializable {
    @Column(name = "RUNTIME")
    @Enumerated(EnumType.STRING)
    @JsonProperty("runtime")
    private Runtime runtime = null;

    public Environment runtime(Runtime runtime) {
        this.runtime = runtime;
        return this;
    }

    /**
     * Get runtime
     * @return runtime
     **/
    public Runtime getRuntime() {
        return runtime;
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Environment environment = (Environment) o;
        return Objects.equals(this.runtime, environment.runtime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtime);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Environment {\n");

        sb.append("    runtime: ").append(toIndentedString(runtime)).append("\n");
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

