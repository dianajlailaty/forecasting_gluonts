package org.activeeon.morphemic.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Embeddable
public class DockerEnvironment {

    @Column(name = "DOCKER_IMAGE")
    private String dockerImage;

    @Column(name = "PORT")
    private String port;

    @Column(name = "ENV_VARS")
    @ElementCollection(targetClass=String.class)
    private Map<String, String> environmentVars;

    public String getEnvVarsAsCommandString() {
        StringBuilder commandString = new StringBuilder();
        for (Map.Entry<String, String> entry : environmentVars.entrySet()) {
            commandString.append("-e ").append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
        }
        return commandString.toString();
    }
}
