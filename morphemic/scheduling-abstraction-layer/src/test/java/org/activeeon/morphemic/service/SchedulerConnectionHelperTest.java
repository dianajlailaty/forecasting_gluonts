package org.activeeon.morphemic.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.authentication.ConnectionInfo;
import org.ow2.proactive_grid_cloud_portal.smartproxy.RestSmartProxyImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulerConnectionHelperTest {

    private final String DUMMY_CONNECTION_URL = "http://notExist.activeeon.com:8080";

    private final String DUMMY_USERNAME = "username";

    private final String DUMMY_PASSWORD = "password";

    private final ConnectionInfo DUMMY_CONNECTION_INFO = new ConnectionInfo(DUMMY_CONNECTION_URL,
            DUMMY_USERNAME,
            DUMMY_PASSWORD,
            null,
            true);

    @Mock
    RestSmartProxyImpl restSmartProxy;

    /**
     * Set up and mock all needed parameters and inject them, if needed, into the SchedulerConnectionHelper class
     * using the class setters
     */
    @BeforeEach
    void setUp() {
        // Enable all mocks
        MockitoAnnotations.openMocks(this);

        // Mock the Rest Smart Proxy icConnected method to return true
        when(restSmartProxy.isConnected()).thenReturn(false);

        // Mock the Rest Smart Proxy init method to do nothing when it is called instead of creating an actual
        // proxy on an incorrect URL ==> This will avoid exceptions
        doNothing().when(restSmartProxy).init(DUMMY_CONNECTION_INFO);

        // Inject the current Rest Smart Proxy into the SchedulerConnectionHelper class
        SchedulerConnectionHelper.setRestSmartProxy(restSmartProxy);

        // Temporary disable all Logging from the RMConnectionHelper class
        // It is enabled after the tests are completed
        Logger.getLogger(SchedulerConnectionHelper.class).setLevel(Level.OFF);
    }

    /**
     * Test the init method of the SchedulerConnectionHelper class
     * The test consists in calling the actual init method of the SchedulerConnectionHelper class and then verify
     * if initialized Gateway URL is equal to the Dummy URL passed in the parameters
     */
    @Test
    void init() {
        SchedulerConnectionHelper.init(DUMMY_CONNECTION_URL);
        assertEquals(DUMMY_CONNECTION_URL,SchedulerConnectionHelper.getPaURL());
    }

    /**
     * Test the connect method of the SchedulerConnectionHelper class
     * The test consists in calling the actual connect method of the SchedulerConnectionHelper class using the
     * dummy username and password and then check if the the SchedulerConnectionHelper isActive variable is true
     */
    @Test
    void connect() {
        SchedulerConnectionHelper.connect(DUMMY_USERNAME, DUMMY_PASSWORD);
        assertTrue(SchedulerConnectionHelper.getIsActive());
    }

    /**
     * Test the disconnect method in SchedulerConnectionHelper class
     * The test consists in calling the actual disconnect method of the SchedulerConnectionHelper class and then
     * check if the SchedulerConnectionHelper getIsActive method return false
     */
    @Test
    void disconnect() {
        SchedulerConnectionHelper.disconnect();
        assertFalse(SchedulerConnectionHelper.getIsActive());
    }

    /**
     * Enable all Logging in RMConnectionHelper class
     */
    @AfterEach
    void enableLogging() {
        Logger.getLogger(SchedulerConnectionHelper.class).setLevel(Level.ALL);
    }
}