package org.activeeon.morphemic.model;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "TASK")
public class Task implements Serializable {
    @Id
    @Column(name = "TASK_ID")
    private String taskId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "TYPE")
    private String type;

    @Embedded
    private CommandsInstallation installation;

    @Embedded
    private DockerEnvironment environment;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REFRESH)
    private List<Deployment> deployments;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REFRESH)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Port> portsToOpen;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REFRESH)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<Monitor> monitors;

    @Column(name = "PARENT_TASKS")
    @ElementCollection(targetClass=String.class)
    private List<String> parentTasks;

    @Column(name = "SUBMITTED_TASK_NAMES")
    @ElementCollection(targetClass=String.class)
    private List<String> submittedTaskNames;

    @Column(name = "DEPLOYMENT_FIRST_SUBMITTED_TASK_NAME")
    private String deploymentFirstSubmittedTaskName;

    @Column(name = "DEPLOYMENT_LAST_SUBMITTED_TASK_NAME")
    private String deploymentLastSubmittedTaskName;

    public void addDeployment(Deployment deployment) {
        if (deployments==null){
            deployments = new LinkedList<>();
        }
        deployments.add(deployment);
    }

    public void addSubmittedTaskName(String submittedTaskName) {
        if (submittedTaskNames==null){
            submittedTaskNames = new LinkedList<>();
        }
        submittedTaskNames.add(submittedTaskName);
    }
}
