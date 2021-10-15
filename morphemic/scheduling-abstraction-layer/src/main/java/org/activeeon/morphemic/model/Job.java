package org.activeeon.morphemic.model;

import lombok.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "JOB")
public class Job implements Serializable {
    @Id
    @Column(name = "JOB_ID")
    private String jobId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "VARIABLES")
    @ElementCollection(targetClass=String.class)
    private Map<String, String> variables;

    @Column(name = "SUBMITTED_JOB_ID")
    private long submittedJobId;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.REFRESH)
    private List<Task> tasks;

    public Task findTask(String taskName) {
        return tasks.stream()
                .filter(task -> task.getName().equals(taskName)).findAny().orElse(null);
    }

    /**
     *
     * Transform a job into JSON format
     *
     * @return the JSON representation of the job
     */
    public String getJobInJson() throws IOException{
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }
}
