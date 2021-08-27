package org.activeeon.morphemic.infrastructure.deployment;

import org.activeeon.morphemic.service.RMConnectionHelper;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.log4j.Logger;
import org.ow2.proactive.resourcemanager.common.NSState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.dto.RMStateFull;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.exception.RMNodeException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive_grid_cloud_portal.common.RMRestInterface;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.PermissionRestException;
import org.ow2.proactive_grid_cloud_portal.scheduler.exception.RestException;

import javax.security.auth.login.LoginException;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class PAResourceManagerGateway {

    private RMRestInterface rmRestInterface;

    static final int MAX_CONNECTION_RETRIES = 10;

    static final int INTERVAL = 10000;

    private static final Logger LOGGER = Logger.getLogger(PAResourceManagerGateway.class);

    /**
     * Get, in an asynchronous way, deployed nodes names
     * @param nodeSource The name of the node source
     * @return List of deployed nodes names
     * @throws InterruptedException In case the process is interrupted
     */
    public List<String> getAsyncDeployedNodesInformation(String nodeSource) throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Callable<List<String>> callable = () -> {
            int retries = 0;

            List<String> deployedNodes = null;
            boolean gotResponse = false;

            while (!gotResponse && retries <= MAX_CONNECTION_RETRIES) {
                try {
                    Thread.sleep(INTERVAL);
                    deployedNodes = getDeployedNodesInformation(nodeSource);
                    if (!deployedNodes.isEmpty()) {
                        gotResponse = true;
                    } else {
                        retries++;
                    }
                } catch (Exception e) {
                    retries++;
                }
            }
            if (gotResponse) {
                LOGGER.info("Got nodes names after " + retries + " failed attempts.");
                return deployedNodes;
            }
            throw new ConnectTimeoutException("Impossible to get deployed nodes names from RM.");
        };

        Future<List<String>> connectionResponse = executorService.submit(callable);
        List<String> deployedNodes = null;
        try {
            deployedNodes = connectionResponse.get();
        } catch (ExecutionException ee) {
            LOGGER.error("ExecutionException: " + ee);
        }
        executorService.shutdown();
        return deployedNodes;
    }


    /**
     * Construct a gateway to the ProActive Resource Manager
     * @param paURL ProActive URL (exp: http://try.activeeon.com:8080/)
     */
    public PAResourceManagerGateway(String paURL){
        // Initialize the gateway from the RMConnectionHelper class
        rmRestInterface = RMConnectionHelper.init(paURL);
    }


    /**
     * Connect to the ProActive server
     * @param username Username
     * @param password Password
     * @throws LoginException In case the login is not valid
     * @throws KeyException In case the password is not valid
     * @throws RMException In case an error happens in the RM
     * @throws NotConnectedException In case the session id is invalid
     */
    public void connect(String username, String password) throws LoginException, KeyException, RMException {
        RMConnectionHelper.connect(username,password);
    }

    /**
     * Disconnect from the ProActive server
     */
    public void disconnect(){
        RMConnectionHelper.disconnect();
    }

    /**
     * Get deployed nodes names
     * @param nodeSource The name of the node source
     * @return List of deployed nodes names
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     */
    private List<String> getDeployedNodesInformation(String nodeSource) throws NotConnectedException, PermissionRestException {
        List<String> deployedNodes = new ArrayList<>();
        LOGGER.debug("Getting full RM state ...");
        RMStateFull rmState = rmRestInterface.getRMStateFull(RMConnectionHelper.getSessionId());
        LOGGER.debug("Full monitoring got.");
        LOGGER.debug("Searching for deployed nodes information ...");
        for (RMNodeEvent rmNodeEvent : rmState.getNodesEvents()) {
            if (rmNodeEvent.getNodeSource().equals(nodeSource)) {
                String nodeUrl = rmNodeEvent.getNodeUrl();
                deployedNodes.add(nodeUrl.substring(nodeUrl.lastIndexOf('/') + 1));
            }
        }
        LOGGER.info(deployedNodes.size() + " nodes found!");

        return deployedNodes;
    }

    /**
     * Deploy a simple AWS node source
     * @param awsUsername A valid AWS user name
     * @param awsKey A valid AWS secret key
     * @param rmHostname The RM host name
     * @param nodeSourceName The name of the node source
     * @param numberVMs The number of needed VMs
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     */
    public void deploySimpleAWSNodeSource(String awsUsername, String awsKey, String rmHostname, String nodeSourceName, Integer numberVMs) throws NotConnectedException, PermissionRestException {
        // Getting NS configuration settings
        String infrastructureType = "org.ow2.proactive.resourcemanager.nodesource.infrastructure.AWSEC2Infrastructure";
        String[] infrastructureParameters = {awsUsername, //username
                awsKey, //secret
                numberVMs.toString(), //N of VMs
                "1", //N VMs per node
                "", //image
                "", //OS
                "", //awsKeyPair
                "", //ram
                "", //Ncore
                "", //sg
                "", //subnet
                rmHostname, //host
                "http://" + rmHostname + ":8080/connector-iaas", //connector-iaas url
                "http://" + rmHostname + ":8080/rest/node.jar", //node jar url
                "",
                "300000", //timeout
                ""};
        LOGGER.debug("infrastructureParameters: " + Arrays.toString(infrastructureParameters));
        String[] infrastructureFileParameters = {""};
        String policyType = "org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy";
        String[] policyParameters = {"ALL","ME"};
        String[] policyFileParameters = {};
        String nodesRecoverable = "true";

        LOGGER.debug("Creating NodeSource ...");
        rmRestInterface.defineNodeSource(RMConnectionHelper.getSessionId(), nodeSourceName,infrastructureType, infrastructureParameters, infrastructureFileParameters, policyType, policyParameters, policyFileParameters, nodesRecoverable);
        LOGGER.info("NodeSource created.");

        LOGGER.debug("Deploying the NodeSource ...");
        rmRestInterface.deployNodeSource(RMConnectionHelper.getSessionId(), nodeSourceName);
        LOGGER.info("NodeSource VMs deployed.");

    }

    /**
     * Search the nodes with specific tags.
     * @param tags a list of tags which the nodes should contain. When not specified or an empty list, all the nodes known urls are returned
     * @param all When true, the search return nodes which contain all tags;
     *            when false, the search return nodes which contain any tag among the list tags.
     * @return the set of urls which match the search condition
     */
    public List<String> searchNodes(List<String> tags, boolean all) throws NotConnectedException, RestException {
        LOGGER.debug("Search for nodes ...");
        List<String> nodesUrls = new ArrayList<>(rmRestInterface.searchNodes(RMConnectionHelper.getSessionId(), tags, all));
        LOGGER.debug("Nodes found: " + nodesUrls);
        return nodesUrls;
    }

    /**
     * Undeploy a node source
     * @param nodeSourceName The name of the node source to undeploy
     * @param preempt If true undeploy node source immediately without waiting for nodes to be freed
     * @return The result of the action, possibly containing the error message
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     */
    public NSState undeployNodeSource(String nodeSourceName, Boolean preempt) throws NotConnectedException, PermissionRestException {
        LOGGER.debug("Undeploying node source ...");
        NSState nsState = rmRestInterface.undeployNodeSource(RMConnectionHelper.getSessionId(), nodeSourceName, preempt);
        LOGGER.info("Node source undeployed!");
        return nsState;
    }

    /**
     * Remove a node source
     * @param nodeSourceName The name of the node source to remove
     * @param preempt If true remove node source immediately without waiting for nodes to be freed
     * @return True if the node source is removed successfully, false or exception otherwise
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     */
    public Boolean removeNodeSource(String nodeSourceName, Boolean preempt) throws NotConnectedException, PermissionRestException {
        LOGGER.debug("Removing node source ...");
        Boolean result = rmRestInterface.removeNodeSource(RMConnectionHelper.getSessionId(), nodeSourceName, preempt);
        LOGGER.info("Node source removed!");
        return result;
    }

    /**
     * Release a node
     * @param nodeUrl The URL of the node to remove
     * @return True if the node is removed successfully, false or exception otherwise
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     * @throws RMNodeException In case the RM throws a Node exception
     */
    public Boolean releaseNode(String nodeUrl) throws NotConnectedException, PermissionRestException, RMNodeException {
        LOGGER.debug("Releasing node ...");
        Boolean result = rmRestInterface.releaseNode(RMConnectionHelper.getSessionId(), nodeUrl);
        LOGGER.info("Node released!");
        return result;
    }

    /**
     * Remove a node
     * @param nodeUrl The URL of the node to remove
     * @param preempt If true remove node immediately without waiting for node to be freed
     * @return True if the node is removed successfully, false or exception otherwise
     * @throws NotConnectedException In case the user is not connected
     * @throws PermissionRestException In case the user does not have valid permissions
     */
    public Boolean removeNode(String nodeUrl, Boolean preempt) throws NotConnectedException, PermissionRestException {
        LOGGER.debug("Removing node ...");
        Boolean result = rmRestInterface.removeNode(RMConnectionHelper.getSessionId(), nodeUrl, preempt);
        LOGGER.info("Node removed!");
        return result;
    }

    // For testing purpose only

    public RMRestInterface getRmRestInterface() {
        return rmRestInterface;
    }

    public void setRmRestInterface(RMRestInterface rmRestInterface) {
        this.rmRestInterface = rmRestInterface;
    }

    public String getSessionId(){
        return RMConnectionHelper.getSessionId();
    }
}
