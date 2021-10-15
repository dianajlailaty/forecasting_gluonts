package org.activeeon.morphemic.application.deployment;

import org.activeeon.morphemic.service.SchedulerConnectionHelper;
import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.exception.*;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive_grid_cloud_portal.smartproxy.RestSmartProxyImpl;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class PASchedulerGateway {

    private RestSmartProxyImpl restSmartProxy;

    private static final Logger LOGGER = Logger.getLogger(PASchedulerGateway.class);

    /**
     * Construct a gateway to the ProActive Scheduler
     * @param paUrl ProActive URL (exp: http://try.activeeon.com:8080/)
     */
    public PASchedulerGateway(String paUrl) {
        SchedulerConnectionHelper.init(paUrl);
    }

    /**
     * Submit a ProActive job to the scheduler
     * @param job A ProActive job
     * @return JobId
     */
    public JobId submit(Job job) {
        JobId jobId = null;
        LOGGER.debug("Submitting job: " + job.toString());
        try {
            jobId = restSmartProxy.submit(job);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to submit the job due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to submit the job due to a PermissionException: " + pe.toString());
        } catch (SubmissionClosedException sce) {
            LOGGER.error("ERROR: Not able to submit the job due to a SubmissionClosedException: " + sce.toString());
        } catch (JobCreationException jce) {
            LOGGER.error("ERROR: Not able to submit the job due to a JobCreationException: " + jce.toString());
        }
        return jobId;
    }

    /**
     * Submit a ProActive job to the scheduler
     * @param xmlFile A ProActive job xml file
     * @return JobId
     */
    public JobId submit(File xmlFile) {
        JobId jobId = null;
        LOGGER.debug("Submitting job: " + xmlFile.toString());
        try {
            jobId = restSmartProxy.submit(xmlFile);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to submit the job due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to submit the job due to a PermissionException: " + pe.toString());
        } catch (SubmissionClosedException sce) {
            LOGGER.error("ERROR: Not able to submit the job due to a SubmissionClosedException: " + sce.toString());
        } catch (JobCreationException jce) {
            LOGGER.error("ERROR: Not able to submit the job due to a JobCreationException: " + jce.toString());
        }
        return jobId;
    }

    /**
     * Submit a ProActive job to the scheduler
     * @param xmlFile A ProActive job xml file
     * @param variables A variables map
     * @return JobId
     */
    public JobId submit(File xmlFile, Map<String, String> variables) {
        JobId jobId = null;
        LOGGER.debug("Submitting job: " + xmlFile.toString());
        LOGGER.debug("  with variables: " + variables.toString());
        try {
            jobId = restSmartProxy.submit(xmlFile, variables);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to submit the job due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to submit the job due to a PermissionException: " + pe.toString());
        } catch (SubmissionClosedException sce) {
            LOGGER.error("ERROR: Not able to submit the job due to a SubmissionClosedException: " + sce.toString());
        } catch (JobCreationException jce) {
            LOGGER.error("ERROR: Not able to submit the job due to a JobCreationException: " + jce.toString());
        }
        return jobId;
    }

    /**
     * Get a ProActive job state
     * @param jobId A ProActive job ID
     * @return The job state
     */
    public JobState getJobState(String jobId) {
        JobState jobState = null;
        try {
            jobState = restSmartProxy.getJobState(jobId);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to get the job state due to a NotConnectedException: " + nce.toString());
        } catch (UnknownJobException uje) {
            LOGGER.error("ERROR: Not able to get the job state due to an unknown job ID: " + uje.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to submit the job due to a PermissionException: " + pe.toString());
        }
        return jobState;
    }

    /**
     * Wait for a job
     * @param jobId A ProActive job ID
     * @param timeout The waiting timeout
     * @return The job result
     */
    public JobResult waitForJob(String jobId, long timeout) {
        JobResult jobResult = null;
        try {
            jobResult = restSmartProxy.waitForJob(jobId, timeout);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to wait for the job due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to wait for the job due to a PermissionException: " + pe.toString());
        } catch (UnknownJobException uje) {
            LOGGER.error("ERROR: Unknown job ID: " + uje.toString());
        } catch (TimeoutException te) {
            LOGGER.warn("WARNING: Not able to wait for the job due to timeout exceed: " + te.toString());
        }
        return jobResult;
    }

    /**
     * Get job results map
     * @param jobsId A list of ProActive jobs ID
     * @return The jobs results map
     */
    public Map<Long, Map<String, Serializable>> getJobResultMaps(List<String> jobsId) {
        Map<Long, Map<String, Serializable>> jobResults = null;
        try {
            jobResults = restSmartProxy.getJobResultMaps(jobsId);
        } catch (SchedulerException se) {
            LOGGER.error("ERROR: Not able to get jobs results due to : " + se.toString());
        }
        return jobResults;
    }

    /**
     * Get job results map
     * @param jobId A list of ProActive jobs ID
     * @return The jobs results map
     */
    public boolean killJob(String jobId) {
        boolean result = false;
        try {
            result = restSmartProxy.killJob(jobId);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to kill the job due to a NotConnectedException: " + nce.toString());
        } catch (UnknownJobException uje) {
            LOGGER.error("ERROR: Unknown job ID: " + uje.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to kill the job due to a PermissionException: " + pe.toString());
        }
        return result;
    }

    /**
     * Wait for a task
     * @param jobId A ProActive job ID
     * @param taskName A task name
     * @param timeout The waiting timeout
     * @return The task result
     */
    public TaskResult waitForTask(String jobId, String taskName, long timeout) {
        TaskResult taskResult = null;
        try {
            taskResult = restSmartProxy.waitForTask(jobId, taskName, timeout);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to wait for the task due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to wait for the task due to a PermissionException: " + pe.toString());
        } catch (UnknownJobException uje) {
            LOGGER.error("ERROR: Unknown job ID: " + uje.toString());
        } catch (UnknownTaskException ute) {
            LOGGER.error("ERROR: Unknown task name: " + ute.toString());
        } catch (TimeoutException te) {
            LOGGER.warn("WARNING: Not able to wait for the task due to timeout exceed: " + te.toString());
        }
        return taskResult;
    }

    /**
     * Get a task result
     * @param jobId A ProActive job ID
     * @param taskName A task name
     * @return The task result
     */
    public TaskResult getTaskResult(String jobId, String taskName) {
        TaskResult taskResult = null;
        try {
            taskResult = restSmartProxy.getTaskResult(jobId, taskName);
        } catch (NotConnectedException nce) {
            LOGGER.error("ERROR: Not able to wait for the task due to a NotConnectedException: " + nce.toString());
        } catch (PermissionException pe) {
            LOGGER.error("ERROR: Not able to wait for the task due to a PermissionException: " + pe.toString());
        } catch (UnknownJobException uje) {
            LOGGER.error("ERROR: Unknown job ID: " + uje.toString());
        } catch (UnknownTaskException ute) {
            LOGGER.error("ERROR: Unknown task name: " + ute.toString());
        }
        return taskResult;
    }

    /**
     * Connect to the ProActive server
     * @param username The user's username
     * @param password The user's password
     */
    public void connect(String username, String password) {
        // Connect to the Scheduler API
        restSmartProxy = SchedulerConnectionHelper.connect(username,password);
    }

    /**
     * Disconnect from the ProActive server
     */
    public void disconnect() {
        restSmartProxy = SchedulerConnectionHelper.disconnect();
    }

    // For testing purpose
    public RestSmartProxyImpl getRestSmartProxy() {
        return restSmartProxy;
    }

    public void setRestSmartProxy(RestSmartProxyImpl restSmartProxy) {
        this.restSmartProxy = restSmartProxy;
    }
}
