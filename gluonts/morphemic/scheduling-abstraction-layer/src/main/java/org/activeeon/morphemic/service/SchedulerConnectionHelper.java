package org.activeeon.morphemic.service;

import org.apache.log4j.Logger;
import org.ow2.proactive.authentication.ConnectionInfo;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive_grid_cloud_portal.smartproxy.RestSmartProxyImpl;


public class SchedulerConnectionHelper {

    private static final Logger LOGGER = Logger.getLogger(SchedulerConnectionHelper.class);

    private static final String SCHEDULER_REST_PATH = "/rest";

    private static RestSmartProxyImpl restSmartProxy = new RestSmartProxyImpl();

    private static String paURL;

    // For testing purpose only
    private static boolean isActive;


    /**
     *
     * Initialize the gateway URL
     *
     * @param url URL for the ProActive Rest service
     */
    public static void init(String url) {
        paURL = url;
    }

    /**
     *
     * Connect to the Scheduler gateway
     *
     * @param username Username
     * @param password Password
     * @return The initialized Scheduler gateway
     */
    public static synchronized RestSmartProxyImpl connect(String username, String password) {
        LOGGER.debug("Connecting to Scheduler ...");
        //TODO: TO improve the concatenation of URLs
        ConnectionInfo connectionInfo = new ConnectionInfo(paURL + SCHEDULER_REST_PATH,
                username,
                password,
                null,
                true);
        // Check if the proxy is connected
        // If not make a new connection
        if(!restSmartProxy.isConnected()) {
            restSmartProxy.init(connectionInfo);
            LOGGER.info("Connected to Scheduler");
            isActive = true;
        }else{
            LOGGER.info("Already connected to Scheduler");
            isActive = true;
        }
        return restSmartProxy;
    }

    /**
     *
     * Disconnect from the Scheduler
     *
     */
    public static synchronized RestSmartProxyImpl disconnect() {
        try {
            if(restSmartProxy.isConnected()) {
                restSmartProxy.disconnect();
                isActive = false;
                LOGGER.info("Disconnected from Scheduler");
            }else{
                LOGGER.info("Already disconnected from Scheduler");
            }
        } catch (PermissionException e) {
            LOGGER.warn("WARNING: Not able to disconnect due to: " + e.toString());
        }
        return restSmartProxy;
    }

    // All of the following methods are used for Test Driven Dev
    public static boolean getIsActive() {
        return isActive;
    }

    public static String getPaURL() {
        return paURL;
    }

    public static void setRestSmartProxy(RestSmartProxyImpl restSmartProxy) {
        SchedulerConnectionHelper.restSmartProxy = restSmartProxy;
    }
}
