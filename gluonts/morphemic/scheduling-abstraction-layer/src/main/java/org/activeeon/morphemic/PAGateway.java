package org.activeeon.morphemic;

import org.activeeon.morphemic.application.deployment.PAFactory;
import org.activeeon.morphemic.application.deployment.PASchedulerGateway;
import org.activeeon.morphemic.infrastructure.deployment.PAConnectorIaasGateway;
import org.activeeon.morphemic.infrastructure.deployment.PAResourceManagerGateway;
import org.activeeon.morphemic.model.*;
import org.activeeon.morphemic.service.*;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.UserException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.ScriptTask;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskVariable;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.PermissionRestException;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.RestException;

import javax.security.auth.login.LoginException;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PAGateway {

    private final String paURL;

    public PAResourceManagerGateway resourceManagerGateway;

    public PASchedulerGateway schedulerGateway;

    public PAConnectorIaasGateway connectorIaasGateway;

    private static final Logger LOGGER = Logger.getLogger(PAGateway.class);

    /**
     * Construct a gateway to the ProActive server
     * @param paURL ProActive server URL (exp: http://try.activeeon.com:8080/)
     */
    public PAGateway(String paURL) {
        this.paURL = paURL;
        resourceManagerGateway = new PAResourceManagerGateway(paURL);
        schedulerGateway = new PASchedulerGateway(paURL);
        connectorIaasGateway = new PAConnectorIaasGateway(paURL);
    }

    /**
     * Connect to the ProActive server
     * @param username The user's username
     * @param password The user's password
     * @throws LoginException In case the login is not valid
     * @throws KeyException In case the password is not valid
     * @throws RMException In case an error happens in the RM
     */
    public void connect(String username, String password) throws LoginException, KeyException, RMException {
        LOGGER.debug("Connecting to ProActive's Resource Manager");
        resourceManagerGateway.connect(username, password);
        LOGGER.debug("Connecting to ProActive's Scheduler");
        schedulerGateway.connect(username,password);
    }

    /**
     * Disconnect from the ProActive server
     */
    public void disconnect() throws NotConnectedException {
        LOGGER.debug("Disconnecting from RM...");
        resourceManagerGateway.disconnect();
        LOGGER.debug("Disconnecting from Scheduler...");
        schedulerGateway.disconnect();
    }

    private List<Port> extractListOfPortsToOpen(JSONArray ports, JSONObject job) {
        List<Port> portsToOpen = new LinkedList<>();
        if (ports != null) {
            ports.forEach(object -> {
                JSONObject portEntry = (JSONObject) object;
                if (Objects.equals("PortProvided", portEntry.optString("type"))) {
                    Port portToOpen = new Port(portEntry.optInt("port"));
                    portToOpen.setRequestedName(findRequiredPort(job, portEntry.optString("name")));
                    portsToOpen.add(portToOpen);
                }
            });
        }
        return portsToOpen;
    }

    private String findRequiredPort(JSONObject job, String providedPortName) {
        for (Object communicationObject : job.optJSONArray("communications")) {
            JSONObject communication = (JSONObject) communicationObject;
            if (Objects.equals(providedPortName, communication.optString("portProvided")))
                return communication.optString("portRequired");
        }
        return "NOTREQUESTED_providedPortName";
    }

    private String findProvidedPort(JSONObject job, String requiredPortName) {
        for (Object communicationObject : job.optJSONArray("communications")) {
            JSONObject communication = (JSONObject) communicationObject;
            if (Objects.equals(requiredPortName, communication.optString("portRequired")))
                return communication.optString("portProvided");
        }
        throw new NotFoundException("Required port " + requiredPortName + " not found in communications.");
    }

    private boolean taskProvidesPort(JSONObject task, String providedPortName) {
        for (Object portObject : task.optJSONArray("ports")) {
            JSONObject port = (JSONObject) portObject;
            if (Objects.equals("PortProvided", port.optString("type"))
                    && Objects.equals(providedPortName, port.optString("name")))
                return true;
        }
        return false;
    }

    private String findTaskByProvidedPort(JSONArray tasks, String providedPortName) {
        for (Object taskObject : tasks) {
            JSONObject task = (JSONObject) taskObject;
            if (taskProvidesPort(task, providedPortName))
                return task.optString("name");
        }
        throw new NotFoundException("Task that provides port " + providedPortName + " was not found in job.");
    }

    private List<String> extractParentTasks(JSONObject job, JSONObject task) {
        List<String> parentTasks = new LinkedList<>();
        JSONArray ports = task.optJSONArray("ports");
        if (ports != null) {
            ports.forEach(portObject -> {
                JSONObject portEntry = (JSONObject) portObject;
//                if (Objects.equals("PortRequired", portEntry.optString("type"))
//                        && portEntry.optBoolean("isMandatory")) {
                if (Objects.equals("PortRequired", portEntry.optString("type"))) {
                    LOGGER.debug("Mandatory required port detected");
                    String providedPortName = findProvidedPort(job, portEntry.optString("name"));
                    parentTasks.add(findTaskByProvidedPort(job.optJSONArray("tasks"), providedPortName));
                }
            });
        }

        return parentTasks;
    }


    /**
     *
     * Return the dot format of the application's graph
     *
     * @param jobId The ID of the job
     * @return The graph in dot format
     */
    public String getGraphInDotFormat(String jobId) {

        // StringBuilder used to write the syntax of the application graph in dot format
        StringBuilder dotGraphSyntax = new StringBuilder();

        // Get the job by jobId from the DB
        Job applicationJob = EntityManagerHelper.find(Job.class, jobId);

        LOGGER.debug("Dot graph creation for the job: "+applicationJob.toString());

        // Write the dot file header
        dotGraphSyntax.append("digraph g {\n");

        // Get the job tasks
        LinkedList<Task> jobTasks = new LinkedList<>(applicationJob.getTasks());

        // Add the mandatory connections between the tasks
        jobTasks.forEach(task -> {

            // Write the dot representation of the task
            dotGraphSyntax.append(task.getName() + ";\n");

            // Get the current task name
            String childTask = task.getName();

            // Get the list of the parent tasks
            List<String> parentTasks = task.getParentTasks();

            // Check for Mandatory connections
            // If the list is empty there are no mandatory connections
            parentTasks.forEach(parentTask -> {

                // Write the dot syntax of the connection between the two tasks
                dotGraphSyntax.append(parentTask
                        + "->"
                        + childTask
                        + " [fillcolor=red, fontcolor=red, color=red]"
                        + ";");

                dotGraphSyntax.append("\n");
            });

        });

        // Write the dot file end character
        dotGraphSyntax.append("}\n");

        LOGGER.debug("Dot graph created");

        return dotGraphSyntax.toString();
    }

    /**
     * Create a ProActive job skeleton
     * @param job A job skeleton definition in JSON format
     */
    public void createJob(JSONObject job) {
        Validate.notNull(job, "The job received is empty. Nothing to be created.");

        EntityManagerHelper.begin();

        Job newJob = new Job();
        newJob.setJobId(job.optJSONObject("jobInformation").optString("id"));
        newJob.setName(job.optJSONObject("jobInformation").optString("name"));
        List<Task> tasks = new LinkedList<>();
        JSONArray jsonTasks = job.optJSONArray("tasks");
        jsonTasks.forEach(object -> {
            JSONObject task = (JSONObject) object;
            Task newTask = new Task();
            newTask.setTaskId(newJob.getJobId() + task.optString("name"));
            newTask.setName(task.optString("name"));
            JSONObject installation = task.getJSONObject("installation");

            newTask.setType(installation.optString("type"));
            switch (newTask.getType()) {
                case "docker":
                    DockerEnvironment environment = new DockerEnvironment();
                    environment.setDockerImage(installation.optString("dockerImage"));
                    environment.setPort(installation.optJSONObject("environment").optString("port"));
                    Map<String, String> vars = new HashMap<>();
                    installation.optJSONObject("environment").keySet().stream().filter(key -> !key.equals("port")).forEach(key -> vars.put(key, installation.optJSONObject("environment").optString(key)));
                    environment.setEnvironmentVars(vars);
                    newTask.setEnvironment(environment);
                    LOGGER.info("vars calculated" + vars);
                case "commands":
                    CommandsInstallation commands = new CommandsInstallation();
                    commands.setPreInstall(installation.optString("preInstall"));
                    commands.setInstall(installation.optString("install"));
                    commands.setPostInstall(installation.optString("postInstall"));
                    commands.setPreStart(installation.optString("preStart"));
                    commands.setStart(installation.optString("start"));
                    commands.setPostStart(installation.optString("postStart"));
                    commands.setUpdateCmd(installation.optString("update"));
                    commands.setPreStop(installation.optString("preStop"));
                    commands.setStop(installation.optString("stop"));
                    commands.setPostStop(installation.optString("postStop"));
                    OperatingSystemType operatingSystemType = new OperatingSystemType();
                    operatingSystemType.setOperatingSystemFamily(installation.optJSONObject("operatingSystem")
                            .optString("operatingSystemFamily"));
                    operatingSystemType.setOperatingSystemVersion(installation.optJSONObject("operatingSystem")
                            .optFloat("operatingSystemVersion"));
                    commands.setOperatingSystemType(operatingSystemType);
                    newTask.setInstallation(commands);
                    break;
                case "spark":
                    throw new IllegalArgumentException("Spark tasks are not handled yet.");
            }

            List<Port> portsToOpen = extractListOfPortsToOpen(task.optJSONArray("ports"), job);
            portsToOpen.forEach(EntityManagerHelper::persist);
            newTask.setPortsToOpen(portsToOpen);
            newTask.setParentTasks(extractParentTasks(job, task));

            EntityManagerHelper.persist(newTask);
            tasks.add(newTask);
        });

        newJob.setTasks(tasks);

        EntityManagerHelper.persist(newJob);

        EntityManagerHelper.commit();

        LOGGER.info("Job created: " + newJob.toString());
    }

    /**
     * Find node candidates
     * @param requirements List of NodeType or Attribute requirements
     * @return A list of all node candidates that satisfy the requirements
     */
    public List<NodeCandidate> findNodeCandidates(List<Requirement> requirements) {
        List<NodeCandidate> filteredNodeCandidates = new LinkedList<>();
        List<NodeCandidate> allNodeCandidates = EntityManagerHelper.createQuery("SELECT nc FROM NodeCandidate nc",
                NodeCandidate.class).getResultList();
        allNodeCandidates.forEach(nodeCandidate -> {
            if (JCloudsInstancesUtils.isHandledHardwareInstanceType(nodeCandidate.getCloud().getApi().getProviderName(),
                    nodeCandidate.getHardware().getName())) {
                if (NodeCandidateUtils.verifyAllFilters(requirements, nodeCandidate)) {
                    filteredNodeCandidates.add(nodeCandidate);
                }
            }
        });
        return filteredNodeCandidates;
    }

    /**
     * Define a node source in PA server related to a deployment information
     * @param nodeSourceName A valid and unique node source name
     * @param cloud The cloud information object
     * @param deployment The deployment information object
     */
    private void defineNSWithDeploymentInfo(String nodeSourceName, PACloud cloud, Deployment deployment) {
        String filename;
        Map<String, String> variables = new HashMap<>();
        variables.put("NS_name", nodeSourceName);
        variables.put("NS_nVMs", "0");
        variables.put("security_group", cloud.getSecurityGroup());
        variables.put("image", deployment.getLocationName() + File.separator + deployment.getImageProviderId());
        try {
            URL endpointPa = (new URL(this.paURL));
            variables.put("rm_host_name", endpointPa.getHost());
            variables.put("pa_port", "" + endpointPa.getPort());
        } catch (MalformedURLException e) {
            LOGGER.error(e.getStackTrace());
        }
        switch (cloud.getCloudProviderName()) {
            case "aws-ec2":
                filename = File.separator + "Define_NS_AWS.xml";
                variables.put("aws_username", cloud.getCredentials().getUserName());
                variables.put("aws_secret", cloud.getCredentials().getPrivateKey());
                break;
            case "openstack":
                filename = File.separator + "Define_NS_OS.xml";
                variables.put("os_endpoint", cloud.getEndpoint());
                variables.put("os_scopePrefix", cloud.getScopePrefix());
                variables.put("os_scopeValue", cloud.getScopeValue());
                variables.put("os_identityVersion", cloud.getIdentityVersion());
                variables.put("os_username", cloud.getCredentials().getUserName());
                variables.put("os_password", cloud.getCredentials().getPrivateKey());
                variables.put("os_domain", cloud.getCredentials().getDomain());
                variables.put("os_region", deployment.getLocationName());
                variables.put("os_networkId",cloud.getDefaultNetwork());
                break;
            default:
                throw new IllegalArgumentException("Spark tasks are not handled yet.");
        }
        File fXmlFile = null;
        LOGGER.info("NodeSource deployment workflow filename: " + filename);
        try {
            fXmlFile = TemporaryFilesHelper.createTempFileFromResource(filename);
        } catch (IOException ioe) {
            LOGGER.error("Opening the NS deployment workflow file failed due to : " + Arrays.toString(ioe.getStackTrace()));
        }
        assert fXmlFile != null;
        LOGGER.info("Submitting the file: " + fXmlFile.toString());
        LOGGER.info("Trying to deploy the NS: " + nodeSourceName);
        JobId jobId = schedulerGateway.submit(fXmlFile, variables);
        LOGGER.info("Job submitted with ID: " + jobId);
        TemporaryFilesHelper.delete(fXmlFile);
    }

    /**
     * Add an EMS deployment to a defined job
     * @param authorizationBearer The authorization bearer used by upperware's components to authenticate with each other. Needed by the EMS.
     * @return return 0 if the deployment task is properly added.
     */
    public int addEmsDeployment(List<String> nodeNames, String authorizationBearer) {
        Validate.notNull(authorizationBearer,"The provided authorization bearer cannot be empty");
        EntityManagerHelper.begin();

        AtomicInteger failedDeploymentIdentification = new AtomicInteger();
        // TODO Info to fetch from a config file and from nodeCandidate, when the feature will be available
        String baguetteIp = "ems";
        int baguettePort = 22;
        String operatingSystem = "UBUNTU";
        String targetType = "IAAS";
        boolean isUsingHttps = true;

        // For supplied node ...
        nodeNames.forEach(node -> {
            Deployment deployment = EntityManagerHelper.find(Deployment.class,node);
            PACloud cloud = deployment.getPaCloud();
            EmsDeploymentRequest req = new EmsDeploymentRequest(authorizationBearer, baguetteIp, baguettePort, OperatingSystemFamily.fromValue(operatingSystem), targetType, deployment.getNodeName(), EmsDeploymentRequest.TargetProvider.fromValue(cloud.getCloudProviderName()), deployment.getLocationName(), isUsingHttps, deployment.getNodeName());
            deployment.setEmsDeployment(req);
            EntityManagerHelper.persist(deployment);
        });

        EntityManagerHelper.commit();

        LOGGER.info("EMS deployment definition finished.");
        return failedDeploymentIdentification.get();
    }

    /**
     * Add nodes to the tasks of a defined job
     * @param nodes An array of nodes information in JSONObject format
     * @param jobId A constructed job identifier
     * @return 0 if nodes has been added properly. A greater than 0 value otherwise.
     */
    public synchronized int addNodes(JSONArray nodes, String jobId) {
        Validate.notNull(nodes, "The received nodes structure is empty. Nothing to be created.");

        EntityManagerHelper.begin();

        nodes.forEach(object -> {
            JSONObject node = (JSONObject) object;
            Deployment newDeployment = new Deployment();
            JSONObject nodeCandidateInfo = node.optJSONObject("nodeCandidateInformation");
            newDeployment.setNodeName(node.optString("nodeName"));
            newDeployment.setLocationName(nodeCandidateInfo.optString("locationName"));
            newDeployment.setImageProviderId(nodeCandidateInfo.optString("imageProviderId"));
            newDeployment.setHardwareProviderId(nodeCandidateInfo.optString("hardwareProviderId"));

            PACloud cloud = EntityManagerHelper.find(PACloud.class, nodeCandidateInfo.optString("cloudID"));
            cloud.addDeployment(newDeployment);
            String nodeSourceName = cloud.getNodeSourceNamePrefix() + newDeployment.getLocationName();
            if (!cloud.isRegionDeployed(newDeployment.getLocationName())) {
                this.defineNSWithDeploymentInfo(nodeSourceName, cloud, newDeployment);
                cloud.addDeployedRegion(newDeployment.getLocationName(),
                        newDeployment.getLocationName() + "/" + newDeployment.getImageProviderId());
            }

            LOGGER.info("Node source defined.");

            newDeployment.setPaCloud(cloud);
            EntityManagerHelper.persist(newDeployment);
            LOGGER.debug("Deployment created: " + newDeployment.toString());

            EntityManagerHelper.persist(cloud);
            LOGGER.info("Deployment added to the related cloud: " + cloud.toString());

            LOGGER.info("Trying to retrieve task: " + node.optString("taskName"));
            Task task = EntityManagerHelper.find(Job.class, jobId).findTask(node.optString("taskName"));
            task.addDeployment(newDeployment);
            EntityManagerHelper.persist(task);
        });

        EntityManagerHelper.commit();

        LOGGER.info("Nodes added properly.");

        return 0;
    }

    /**
     * Undeploy clouds
     * @param cloudIDs List of cloud IDs to remove
     * @param preempt If true undeploy node source immediately without waiting for nodes to be freed
     */
    public void undeployClouds(List<String> cloudIDs, Boolean preempt) {
        cloudIDs.forEach(cloudID -> {
            PACloud cloud = EntityManagerHelper.find(PACloud.class, cloudID);
            for (Map.Entry<String, String> entry : cloud.getDeployedRegions().entrySet()) {
                try {
                    resourceManagerGateway.undeployNodeSource(cloud.getNodeSourceNamePrefix() + entry.getKey(), preempt);
                } catch (NotConnectedException | PermissionRestException e) {
                    LOGGER.error(e.getStackTrace());
                }
            }
        });
    }

    /**
     * Remove clouds
     * @param cloudIDs List of cloud IDs to remove
     * @param preempt If true undeploy node source immediately without waiting for nodes to be freed
     */
    public void removeClouds(List<String> cloudIDs, Boolean preempt) {
        EntityManagerHelper.begin();
        cloudIDs.forEach(cloudID -> {
            PACloud cloud = EntityManagerHelper.find(PACloud.class, cloudID);
            for (Map.Entry<String, String> entry : cloud.getDeployedRegions().entrySet()) {
                try {
                    resourceManagerGateway.removeNodeSource(cloud.getNodeSourceNamePrefix() + entry.getKey(), preempt);
                } catch (NotConnectedException | PermissionRestException e) {
                    LOGGER.error(e.getStackTrace());
                }
            }
            EntityManagerHelper.remove(cloud);
        });
        EntityManagerHelper.commit();
    }

    /**
     * Remove nodes
     * @param nodeNames List of node names to remove
     * @param preempt If true remove node immediately without waiting for it to be freed
     */
    public void removeNodes(List<String> nodeNames, Boolean preempt) {
        nodeNames.forEach(nodeName -> {
            try {
                String nodeUrl = resourceManagerGateway.searchNodes(nodeNames, true).get(0);
                resourceManagerGateway.removeNode(nodeUrl, preempt);
                LOGGER.info("Node " + nodeName + " with URL: " + nodeUrl + " has been removed successfully.");
            } catch (NotConnectedException | RestException e) {
                LOGGER.error(e.getStackTrace());
            }
        });
    }

    /**
     * Stop jobs
     * @param jobIDs List of job IDs to stop
     */
    public void stopJobs(List<String> jobIDs) {
        //TODO
    }

    private void updateNodeCandidatesAsync(List<String> newCloudIds) {
        UpdatingNodeCandidatesThread updatingThread = new UpdatingNodeCandidatesThread(this.paURL, newCloudIds);
        updatingThread.start();
    }

    /**
     * Add clouds to the ProActive Resource Manager
     * @param clouds An array of clouds information in JSONObject format
     * @return 0 if clouds has been added properly. A greater than 0 value otherwise.
     */
    public int addClouds(JSONArray clouds) {
        Validate.notNull(clouds, "The received clouds structure is empty. Nothing to be created.");

        EntityManagerHelper.begin();
        List<String> savedCloudIds = new LinkedList<>();
        clouds.forEach(object -> {
            JSONObject cloud = (JSONObject) object;
            PACloud newCloud = new PACloud();
            String nodeSourceNamePrefix = cloud.optString("cloudProviderName") + cloud.optString("cloudID");
            newCloud.setNodeSourceNamePrefix(nodeSourceNamePrefix);
            newCloud.setCloudID(cloud.optString("cloudID"));
            newCloud.setCloudProviderName(cloud.optString("cloudProviderName"));
            newCloud.setCloudType(CloudType.fromValue(cloud.optString("cloudType")));
            newCloud.setDeployedRegions(new HashMap<>());
            if ("null".equals(cloud.optString("securityGroup"))) {
                newCloud.setSecurityGroup("");
            } else {
                newCloud.setSecurityGroup(cloud.optString("securityGroup"));
            }
            newCloud.setEndpoint(cloud.optString("endpoint"));
            newCloud.setScopePrefix(cloud.optJSONObject("scope").optString("prefix"));
            newCloud.setScopeValue(cloud.optJSONObject("scope").optString("value"));
            newCloud.setIdentityVersion(cloud.optString("identityVersion"));
            newCloud.setDefaultNetwork(cloud.optString("defaultNetwork"));
            newCloud.setBlacklist(cloud.optString("blacklist"));

            Credentials credentials = new Credentials();
            credentials.setUserName(cloud.optJSONObject("credentials").optString("user"));
            credentials.setPrivateKey(cloud.optJSONObject("credentials").optString("secret"));
            credentials.setDomain(cloud.optJSONObject("credentials").optString("domain"));
            EntityManagerHelper.persist(credentials);
            newCloud.setCredentials(credentials);

            String dummyInfraName = "iamadummy" + newCloud.getCloudProviderName();
            connectorIaasGateway.defineInfrastructure(dummyInfraName, newCloud, "");
            newCloud.setDummyInfrastructureName(dummyInfraName);

            EntityManagerHelper.persist(newCloud);
            LOGGER.debug("Cloud created: " + newCloud.toString());
            savedCloudIds.add(newCloud.getCloudID());
        });

        EntityManagerHelper.commit();

        LOGGER.info("Clouds created properly.");

        updateNodeCandidatesAsync(savedCloudIds);

        return 0;
    }

    private List<ScriptTask> createAppTasks(Task task, String taskNameSuffix, String taskToken, Job job) {
        switch (task.getType()) {
            case "commands":
                return createCommandsTask(task, taskNameSuffix, taskToken, job);
            case "docker":
                return createDockerTask(task, taskNameSuffix, taskToken);
        }

        return new LinkedList<>();
    }

    private List<ScriptTask> createDockerTask(Task task, String taskNameSuffix, String taskToken) {
        List<ScriptTask> scriptTasks = new LinkedList<>();
        ScriptTask scriptTask = PAFactory.createBashScriptTaskFromFile(task.getName() + taskNameSuffix, "start_docker_app.sh");
        Map<String, TaskVariable> taskVariablesMap = new HashMap<>();
        taskVariablesMap.put("INSTANCE_NAME", new TaskVariable("INSTANCE_NAME", task.getTaskId() + "-$PA_JOB_ID"));
        taskVariablesMap.put("DOCKER_IMAGE", new TaskVariable("DOCKER_IMAGE", task.getEnvironment().getDockerImage()));
        taskVariablesMap.put("PORTS", new TaskVariable("PORTS", task.getEnvironment().getPort()));
        taskVariablesMap.put("ENV_VARS", new TaskVariable("ENV_VARS", task.getEnvironment().getEnvVarsAsCommandString()));
        scriptTask.setVariables(taskVariablesMap);
        scriptTask.addGenericInformation("NODE_ACCESS_TOKEN", taskToken);
        scriptTasks.add(scriptTask);
        return scriptTasks;
    }

    private List<ScriptTask> createCommandsTask(Task task, String taskNameSuffix, String taskToken, Job job) {
        final String newLine = System.getProperty("line.separator");
        final String scriptsSeparation = newLine + newLine + "# Main script" + newLine;
        List<ScriptTask> scriptTasks = new LinkedList<>();
        ScriptTask scriptTaskStart = null;
        ScriptTask scriptTaskInstall = null;

        Map<String, TaskVariable> taskVariablesMap = new HashMap<>();
        if (!task.getParentTasks().isEmpty()) {
            //TODO: Taking into consideration multiple parent tasks with multiple communications
            taskVariablesMap.put("requestedPortName", new TaskVariable("requestedPortName",
                    job.findTask(task.getParentTasks().get(0)).getPortsToOpen().get(0).getRequestedName()));
        }

        if (!(task.getInstallation().getInstall().isEmpty() &&
                task.getInstallation().getPreInstall().isEmpty() &&
                task.getInstallation().getPostInstall().isEmpty())) {
            if (!task.getInstallation().getInstall().isEmpty()) {
                scriptTaskInstall = PAFactory.createBashScriptTask(task.getName() + "_install" + taskNameSuffix,
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getInstall());
            } else {
                scriptTaskInstall = PAFactory.createBashScriptTask(task.getName() + "_install" + taskNameSuffix,
                        "echo \"Installation script is empty. Nothing to be executed.\"");
            }

            if (!task.getInstallation().getPreInstall().isEmpty()) {
                scriptTaskInstall.setPreScript(PAFactory.createSimpleScript(
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getPreInstall(),
                        "bash"));
            }
            if (!task.getInstallation().getPostInstall().isEmpty()) {
                scriptTaskInstall.setPostScript(PAFactory.createSimpleScript(
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getPostInstall(),
                        "bash"));
            }
            if (!task.getParentTasks().isEmpty()) {
                scriptTaskInstall.setVariables(taskVariablesMap);
            }
            scriptTaskInstall.addGenericInformation("NODE_ACCESS_TOKEN", taskToken);
            scriptTasks.add(scriptTaskInstall);
        }

        if (!(task.getInstallation().getStart().isEmpty() &&
                task.getInstallation().getPreStart().isEmpty() &&
                task.getInstallation().getPostStart().isEmpty())) {
            if (!task.getInstallation().getStart().isEmpty()) {
                scriptTaskStart = PAFactory.createBashScriptTask(task.getName() + "_start" + taskNameSuffix,
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getStart());
            } else {
                scriptTaskStart = PAFactory.createBashScriptTask(task.getName() + "_start" + taskNameSuffix,
                        "echo \"Installation script is empty. Nothing to be executed.\"");
            }

            if (!task.getInstallation().getPreStart().isEmpty()) {
                scriptTaskStart.setPreScript(PAFactory.createSimpleScript(
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getPreStart(),
                        "bash"));
            }
            if (!task.getInstallation().getPostStart().isEmpty()) {
                scriptTaskStart.setPostScript(PAFactory.createSimpleScript(
                        Utils.getContentWithFileName("export_env_var_script.sh") + scriptsSeparation +
                                task.getInstallation().getPostStart(),
                        "bash"));
            }
            if(scriptTaskInstall != null) {
                scriptTaskStart.addDependence(scriptTaskInstall);
            }
            scriptTaskStart.addGenericInformation("NODE_ACCESS_TOKEN", taskToken);
            scriptTasks.add(scriptTaskStart);
        }
        return scriptTasks;
    }

    private ScriptTask createInfraTask(Task task, Deployment deployment, String taskNameSuffix, String nodeToken) {
        LOGGER.debug("Acquiring node AWS script file: " + getClass().getResource(File.separator + "acquire_node_aws_script.groovy").toString());
        ScriptTask deployNodeTask = PAFactory.createGroovyScriptTaskFromFile("acquireAWSNode_" + task.getName() + taskNameSuffix,
                "acquire_node_aws_script.groovy");

        deployNodeTask.setPreScript(PAFactory.createSimpleScriptFromFIle("pre_acquire_node_script.groovy", "groovy"));

        Map<String, TaskVariable> variablesMap = new HashMap<>();
        variablesMap.put("NS_name", new TaskVariable("NS_name",
                deployment.getPaCloud().getNodeSourceNamePrefix() + deployment.getLocationName()));
        variablesMap.put("nVMs", new TaskVariable("nVMs", "1", "PA:Integer", false));
        variablesMap.put("synchronous", new TaskVariable("synchronous", "true", "PA:Boolean", false));
        variablesMap.put("timeout", new TaskVariable("timeout", "300", "PA:Long", false));
        ObjectMapper mapper = new ObjectMapper();
        String nodeConfigJson = "{\"image\": \"" + deployment.getLocationName() + "/" + deployment.getImageProviderId() + "\", " +
                "\"vmType\": \"" + deployment.getHardwareProviderId() + "\", " +
                "\"nodeTags\": \"" + deployment.getNodeName();
        if (task.getPortsToOpen() == null || task.getPortsToOpen().isEmpty()) {
            nodeConfigJson += "\"}";
        } else {
            try {
                nodeConfigJson += "\", \"portToOpens\": " + mapper.writeValueAsString(task.getPortsToOpen()) + "}";
            } catch (IOException e) {
                LOGGER.error(e.getStackTrace());
            }
        }
        variablesMap.put("nodeConfigJson", new TaskVariable("nodeConfigJson", nodeConfigJson, "PA:JSON", false));
        variablesMap.put("token", new TaskVariable("token", nodeToken));
        LOGGER.debug("Variables to be added to the task: " + variablesMap.toString());
        deployNodeTask.setVariables(variablesMap);

        return deployNodeTask;
    }

    private ScriptTask createEmsDeploymentTask(EmsDeploymentRequest emsDeploymentRequest, String taskNameSuffix, String nodeToken) {
        LOGGER.debug("Preparing EMS deployment task");
        ScriptTask emsDeploymentTask = PAFactory.createComplexScriptTaskFromFiles("emsDeployment" + taskNameSuffix,"emsdeploy_mainscript.groovy","groovy","emsdeploy_prescript.sh","bash","emsdeploy_postscript.sh","bash");
        Map<String, TaskVariable> variablesMap = emsDeploymentRequest.getWorkflowMap();
        emsDeploymentTask.addGenericInformation("NODE_ACCESS_TOKEN", nodeToken);
        emsDeploymentTask.setVariables(variablesMap);
        return  emsDeploymentTask;
    }

    /**
     * Register a set of node as an operation for scale up
     * @param nodeNames Name of the nodes to be created and provisioned
     * @param jobId The name of the Job to be allocated
     * @param taskName the name of the task whose node are to be allocated
     * @return 0 if the operation went successful, 1 if the scaling failed because no job/task was node found, 2 if the scaling failed because no deployment to clone are available.
     */
    public int addScaleOutTask(List<String> nodeNames, String jobId, String taskName) {
        Validate.notEmpty(nodeNames,"The provided nodes list should not be empty");
        Validate.notNull(jobId,"The provided jobId should not be null.");
        EntityManagerHelper.begin();

        // Let's find the jobId to retrieve the task
        Optional<Job> optJob = Optional.ofNullable(EntityManagerHelper.find(Job.class,jobId));
        if (!optJob.isPresent()) {
            LOGGER.error(String.format("Job [%s] not found", jobId));
            return 1;
        }
        // Let's find the task:
        Optional<Task> optTask = Optional.ofNullable(EntityManagerHelper.find(Task.class,optJob.get().findTask(taskName)));
        if (!optTask.isPresent()) {
            LOGGER.error(String.format("Task [%s] not found", taskName));
            return 1;
        }

        // Let's retrieve the deployment to clone
        Optional<Deployment> optDeployment = Optional.ofNullable(optTask.get().getDeployments().get(0));
        if (!optDeployment.isPresent()) {
            LOGGER.error(String.format("No previous deployment found in task [%s] ",taskName));
            return 2;
        }

        // Let's clone the deployment/node as needed.
        Deployment oldDeployment = optDeployment.get();
        nodeNames.stream().map(nodeName -> {
            Deployment newDeployment = new Deployment();
            newDeployment.setPaCloud(oldDeployment.getPaCloud());
            newDeployment.setNodeName(nodeName);
            newDeployment.setLocationName(oldDeployment.getLocationName());
            newDeployment.setIsDeployed(false);
            newDeployment.setImageProviderId(oldDeployment.getImageProviderId());
            newDeployment.setHardwareProviderId(oldDeployment.getHardwareProviderId());
            EmsDeploymentRequest newEmsDeploymentReq = oldDeployment.getEmsDeployment().clone(nodeName);
            newDeployment.setEmsDeployment(newEmsDeploymentReq);
            return newDeployment;
        }).forEach( deployment -> {
            optTask.get().addDeployment(deployment);
            EntityManagerHelper.persist(deployment.getEmsDeployment());
            EntityManagerHelper.persist(deployment);
            EntityManagerHelper.persist(optTask.get());
        });

        EntityManagerHelper.commit();
        return 0;
    }

    /**
     * Unregister a set of node as a scale-down operation
     * @param nodeNames A list of node to be removed
     * @param jobId The name of the job to scale down the nodes
     * @param taskName the name of the task whose nodes are to be removed
     * @return 0 if the operation went successful, 2 if the operation avorted to prevent last node to be removed.
     */
    public int addScaleInTask(List<String> nodeNames, String jobId, String taskName) {
        Validate.notEmpty(nodeNames,"The provided nodes list should not be empty");
        Validate.notNull(jobId,"The provided jobId should not be null.");
        EntityManagerHelper.begin();

        // Let's find the jobId to retrieve the task
        Optional<Job> optJob = Optional.ofNullable(EntityManagerHelper.find(Job.class,jobId));
        if (!optJob.isPresent()) {
            LOGGER.error(String.format("Job [%s] not found", jobId));
            return 1;
        }

        // Let's find the task:
        Optional<Task> optTask = Optional.ofNullable(EntityManagerHelper.find(Task.class,optJob.get().findTask(taskName)));
        if (!optTask.isPresent()) {
            LOGGER.error(String.format("Task [%s] not found", taskName));
            return 1;
        }

        // Validating there will still be at least one deployment in the task
        if (optTask.get().getDeployments().size() - nodeNames.size() < 1) {
            LOGGER.error("I stop the scale-in: the task will loose its last deployment");
            return 2;
        }

        // For supplied node, I retrieve their deployment
        List<Deployment> deployments = nodeNames.stream().map(node -> EntityManagerHelper.find(Deployment.class,node)).filter(deployment -> (deployment != null)).collect(Collectors.toList());

        // For deployed node, I flag their removal
        List<String> nodesToBeRemoved = deployments.stream().filter(deployment -> deployment.getIsDeployed()).map(Deployment::getNodeName).collect(Collectors.toList());
        // For every node, I remove the deployment entree
        deployments.forEach(
                deployment -> {
                    EntityManagerHelper.remove(deployment);
                    EntityManagerHelper.persist(deployment);
                }
        );
        // I commit the removal of deployed node
        removeNodes(nodesToBeRemoved,false);

        EntityManagerHelper.commit();
        return 0;
    }

    /**
     * Translate a Morphemic task skeleton into a list of ProActive tasks
     * @param task A Morphemic task skeleton
     * @param job The related job skeleton
     * @return A list of ProActive tasks
     */
    public List<ScriptTask> buildPATask(Task task, Job job) {
        List<ScriptTask> scriptTasks = new LinkedList<>();
        List<String> tasksTokens = new LinkedList<>();

        if (task.getDeployments() == null || task.getDeployments().isEmpty()) {
            LOGGER.warn("The task " + task.getName() + " does not have a deployment. It will be scheduled on any free node.");
            scriptTasks.addAll(createAppTasks(task, "", "", job));
            task.setDeploymentFirstSubmittedTaskName(scriptTasks.get(0).getName());
            task.setDeploymentLastSubmittedTaskName(scriptTasks.get(scriptTasks.size()-1).getName());
        }
        else {
            task.getDeployments().forEach(deployment -> {
                // Creating infra deployment tasks
                String token = task.getTaskId() + tasksTokens.size();
                String suffix = "_" + tasksTokens.size();
                scriptTasks.add(createInfraTask(task, deployment, suffix, token));
                // If the infrastructure comes with the deployment of the EMS, we set it up.
                Optional.ofNullable(deployment.getEmsDeployment()).ifPresent(emsDeploymentRequest -> scriptTasks.add(createEmsDeploymentTask(emsDeploymentRequest,suffix,token)));
                LOGGER.debug("Token added: " + token);
                tasksTokens.add(token);
                deployment.setIsDeployed(true);

                // Creating application deployment tasks
                List<ScriptTask> appTasks = createAppTasks(task, suffix, token, job);
                task.setDeploymentLastSubmittedTaskName(appTasks.get(appTasks.size()-1).getName().substring(0, appTasks.get(appTasks.size()-1).getName().lastIndexOf(suffix)));

                // Creating infra preparation task
                appTasks.add(0, createInfraPreparationTask(task, suffix, token, job));
                appTasks.get(1).addDependence(appTasks.get(0));

                // Add dependency between infra and application deployment tasks
                appTasks.get(0).addDependence(scriptTasks.get(scriptTasks.size()-1));

                scriptTasks.addAll(appTasks);
            });
            task.setDeploymentFirstSubmittedTaskName(scriptTasks.get(0).getName().substring(0, scriptTasks.get(0).getName().lastIndexOf("_0")));
        }

        scriptTasks.forEach(scriptTask -> task.addSubmittedTaskName(scriptTask.getName()));

        return scriptTasks;
    }

    private ScriptTask createInfraPreparationTask(Task task, String suffix, String token, Job job) {
        ScriptTask prepareInfraTask;
        Map<String, TaskVariable> taskVariablesMap = new HashMap<>();
        String taskName = "prepareInfra_" + task.getName() + suffix;

        if (!task.getPortsToOpen().isEmpty()) {
            prepareInfraTask = PAFactory.createBashScriptTaskFromFile(taskName, "prepare_infra_script.sh");
            prepareInfraTask.setPostScript(PAFactory.createSimpleScriptFromFIle("post_prepare_infra_script.groovy",
                    "groovy"));
            //TODO: Taking into consideration multiple provided ports
            taskVariablesMap.put("providedPortName", new TaskVariable("providedPortName",
                    task.getPortsToOpen().get(0).getRequestedName()));
            taskVariablesMap.put("providedPortValue", new TaskVariable("providedPortValue",
                    task.getPortsToOpen().get(0).getValue().toString()));
            if (!task.getParentTasks().isEmpty()) {
                //TODO: Taking into consideration multiple parent tasks with multiple communications
                taskVariablesMap.put("requestedPortName", new TaskVariable("requestedPortName",
                        job.findTask(task.getParentTasks().get(0)).getPortsToOpen().get(0).getRequestedName()));
            }
        } else if (!task.getParentTasks().isEmpty()) {
            prepareInfraTask = PAFactory.createBashScriptTaskFromFile(taskName, "prepare_infra_script.sh");
            //TODO: Taking into consideration multiple parent tasks with multiple communications
            taskVariablesMap.put("requestedPortName", new TaskVariable("requestedPortName",
                    job.findTask(task.getParentTasks().get(0)).getPortsToOpen().get(0).getRequestedName()));
        } else {
            prepareInfraTask = PAFactory.createBashScriptTask(taskName,
                    "echo \"No ports to open and not parent tasks. Nothing to be prepared in VM.\"");
        }

        if (task.getInstallation().getOperatingSystemType().getOperatingSystemFamily().toLowerCase(Locale.ROOT).equals("ubuntu") &&
                task.getInstallation().getOperatingSystemType().getOperatingSystemVersion() < 2000) {
            LOGGER.info("Adding apt lock handler script since task: " + task.getName() +
                    " is meant to be executed in: " +
                    task.getInstallation().getOperatingSystemType().getOperatingSystemFamily() +
                    " version: " + task.getInstallation().getOperatingSystemType().getOperatingSystemVersion());
            prepareInfraTask.setPreScript(PAFactory.createSimpleScriptFromFIle("wait_for_lock_script.sh",
                    "bash"));
        }

        prepareInfraTask.setVariables(taskVariablesMap);
        prepareInfraTask.addGenericInformation("NODE_ACCESS_TOKEN", token);

        return prepareInfraTask;
    }

    private void setAllMandatoryDependencies(TaskFlowJob paJob, Job jobToSubmit) {
        jobToSubmit.getTasks().forEach(task -> {
            if (task.getParentTasks() != null && !task.getParentTasks().isEmpty()) {
                task.getParentTasks().forEach(parentTaskName -> {
                    paJob.getTasks().forEach(paTask -> {
                        paJob.getTasks().forEach(paParentTask -> {
                            if (paTask.getName().contains(task.getName()) && paParentTask.getName().contains(parentTaskName)) {
                                if (paTask.getName().contains(task.getDeploymentFirstSubmittedTaskName()) &&
                                        paParentTask.getName().contains(jobToSubmit.findTask(parentTaskName).getDeploymentLastSubmittedTaskName())) {
                                    paTask.addDependence(paParentTask);
                                }
                            }
                        });
                    });
                });
            }
        });
    }

    /**
     * Submit a job constructed in lazy-mode to the ProActive Scheduler
     * @param jobId A constructed job identifier
     * @return The submitted job id
     */
    public long submitJob(String jobId) {
        Job jobToSubmit = EntityManagerHelper.find(Job.class, jobId);
        EntityManagerHelper.refresh(jobToSubmit);
        LOGGER.info("Job found to submit: " + jobToSubmit.toString());

        TaskFlowJob paJob = new TaskFlowJob();
        paJob.setName(jobToSubmit.getName());
        LOGGER.info("Job created: " + paJob.toString());

        EntityManagerHelper.begin();

        jobToSubmit.getTasks().forEach(task -> {
            List<ScriptTask> scriptTasks = buildPATask(task, jobToSubmit);

            scriptTasks.forEach(scriptTask -> {
                try {
                    paJob.addTask(scriptTask);
                } catch (UserException e) {
                    LOGGER.error("Task " + task.getName() + " could not be added due to: " + e.toString());
                }
            });
            EntityManagerHelper.persist(task);
        });

        setAllMandatoryDependencies(paJob, jobToSubmit);

        paJob.setProjectName("Morphemic");

        long submittedJobId = schedulerGateway.submit(paJob).longValue();
        jobToSubmit.setSubmittedJobId(submittedJobId);

        EntityManagerHelper.persist(jobToSubmit);
        EntityManagerHelper.commit();
        LOGGER.info("Job submitted successfully. ID = " + submittedJobId);

        return(submittedJobId);
    }

    /**
     * Get a ProActive job state
     * @param jobId A job ID
     * @return The job state
     */
    public JobState getJobState(String jobId) {
        Job submittedJob = EntityManagerHelper.find(Job.class, jobId);
        JobState jobState = schedulerGateway.getJobState(String.valueOf(submittedJob.getSubmittedJobId()));
        LOGGER.info("Job state of job: " + jobId + " retrieved successfully: " + jobState.toString());
        return jobState;
    }

    /**
     * Wait for execution and get results of a job
     * @param jobId A job ID
     * @param timeout The timeout
     * @return The job result
     */
    public JobResult waitForJob(String jobId, long timeout) {
        Job submittedJob = EntityManagerHelper.find(Job.class, jobId);
        JobResult jobResult = schedulerGateway.waitForJob(String.valueOf(submittedJob.getSubmittedJobId()), timeout);
        LOGGER.info("Results of job: " + jobId + " fetched successfully: " + jobResult.toString());
        return jobResult;
    }

    /**
     * Kill a job
     * @param jobId A job ID
     * @return True if the job has been killed, False otherwise
     */
    public boolean killJob(String jobId) {
        Job submittedJob = EntityManagerHelper.find(Job.class, jobId);
        boolean result = schedulerGateway.killJob(String.valueOf(submittedJob.getSubmittedJobId()));
        if (result) {
            LOGGER.info("The job : " + jobId + " could be killed successfully.");
        } else {
            LOGGER.error("The job : " + jobId + " could not be killed.");
        }
        return result;
    }

    /**
     * Wait for a task
     * @param jobId A job ID
     * @param taskName A task name
     * @param timeout The waiting timeout
     * @return The task results
     */
    public Map<String, TaskResult> waitForTask(String jobId, String taskName, long timeout) {
        Job submittedJob = EntityManagerHelper.find(Job.class, jobId);
        Task createdTask = submittedJob.findTask(taskName);
        Map<String, TaskResult> taskResultsMap = new HashMap<>();
        createdTask.getSubmittedTaskNames().forEach(submittedTaskName -> {
            taskResultsMap.put(submittedTaskName, schedulerGateway.waitForTask(
                    String.valueOf(submittedJob.getSubmittedJobId()),
                    submittedTaskName,
                    timeout));
        });
        LOGGER.info("Results of task: " + taskName + " fetched successfully: " + taskResultsMap.toString());
        return taskResultsMap;
    }

    /**
     * Get a task result
     * @param jobId A job ID
     * @param taskName A task name
     * @return The task results
     */
    public Map<String, TaskResult> getTaskResult(String jobId, String taskName) {
        Job submittedJob = EntityManagerHelper.find(Job.class, jobId);
        Task createdTask = submittedJob.findTask(taskName);
        Map<String, TaskResult> taskResultsMap = new HashMap<>();
        createdTask.getSubmittedTaskNames().forEach(submittedTaskName -> {
            taskResultsMap.put(submittedTaskName, schedulerGateway.getTaskResult(
                    String.valueOf(submittedJob.getSubmittedJobId()),
                    submittedTaskName));
        });

        TaskResult taskResult = schedulerGateway.getTaskResult(String.valueOf(submittedJob.getSubmittedJobId()),
                createdTask.getSubmittedTaskNames()
                        .get(createdTask.getSubmittedTaskNames().size() - 1));
        LOGGER.info("Results of task: " + taskName + " fetched successfully: " + taskResultsMap.toString());
        return taskResultsMap;
    }
}
