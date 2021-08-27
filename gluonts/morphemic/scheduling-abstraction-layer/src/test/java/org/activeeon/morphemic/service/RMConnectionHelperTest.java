package org.activeeon.morphemic.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive_grid_cloud_portal.common.RMRestInterface;

import javax.security.auth.login.LoginException;
import java.security.KeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class RMConnectionHelperTest {

    private final String DUMMY_USERNAME = "USERNAME";

    private final String DUMMY_PASSWORD = "PASSWORD";

    private final String DUMMY_SESSION_ID = "SESSION_ID";

    @Mock
    RMRestInterface rmRestInterface;


    /**
     *
     * Set up and mock all needed parameters and inject them, if needed, in the RMConnectionHelper
     * class using the class setters
     *
     * @throws LoginException In case the login is not valid
     * @throws KeyException In case the password is not valid
     * @throws RMException In case an error happens in the RM
     * @throws NotConnectedException In case if the RM is not connected
     */
    @BeforeEach
    void setUp() throws LoginException, KeyException, RMException, NotConnectedException {
        // Enable all mocks
        MockitoAnnotations.openMocks(this);

        // Mock the RM Interface connect method to return the dummy sessionId, since we are testing on invalid URL,
        // username and password
        when(rmRestInterface.rmConnect(DUMMY_USERNAME,DUMMY_PASSWORD)).thenReturn(DUMMY_SESSION_ID);

        // Mock the RM Interface disconnect method to set the RM Interface inactive when the disconnect is called
        // for the dummy sessionId
        doAnswer(invocation -> when(rmRestInterface.isActive(DUMMY_SESSION_ID)).thenReturn(false))
                .when(rmRestInterface).rmDisconnect(DUMMY_SESSION_ID);

        // Inject the current mocked RM Interface to the RMConnectionHelper class
        RMConnectionHelper.setRmRestInterface(rmRestInterface);

        // Initialize a testing user preference variable
        // It is used to store the session
        String DUMMY_PREFERENCE_ID = "TESTING_PREF";
        RMConnectionHelper.setSessionPreferencesId(DUMMY_PREFERENCE_ID);

        // Temporary disable all Logging from the RMConnectionHelper class
        // It is enabled after the tests are completed
        Logger.getLogger(RMConnectionHelper.class).setLevel(Level.OFF);
    }

    /**
     * Test the init method of the RMConnectionHelper class
     * The test consists in calling the actual init function and verify if it has created the correct RMInterface
     * instance and check if the actual sessionId of the class is empty
     */
    @Test
    void init() {
        String DUMMY_CONNECTION_URL = "http://notExist.activeeon.com:8080";
        RMRestInterface initRMRestInterface = RMConnectionHelper.init(DUMMY_CONNECTION_URL);
        String DUMMY_RM_INTERFACE_MESSAGE = "org.ow2.proactive_grid_cloud_portal.common.RMRestInterface";
        assertTrue(initRMRestInterface.toString().contains(DUMMY_RM_INTERFACE_MESSAGE));
        assertEquals(RMConnectionHelper.getSessionId(), "");
    }

    /**
     *
     * Test the connect method of the RMConnectionHelper class
     * The test consists in calling the actual connect function and verify if the created sessionId for the dummy username
     * and password are equal to their values as defined in the mocked RMInterface
     *
     * @throws LoginException In case the login is not valid
     * @throws KeyException In case the password is not valid
     * @throws RMException In case an error happens in the RM
     */
    @Test
    void connect() throws LoginException, KeyException, RMException {
        RMConnectionHelper.connect(DUMMY_USERNAME,DUMMY_PASSWORD);
        assertEquals(DUMMY_SESSION_ID,RMConnectionHelper.getSessionId());
    }

    /**
     * Test the disconnect method of the RMConnectionHelper class
     * The test consists in calling the actual disconnect method and then verify if the sessionId was deleted
     */
    @Test
    void disconnect() {
        RMConnectionHelper.disconnect();
        assertEquals("", RMConnectionHelper.getSessionId());
    }

    /**
     * Test the getSessionId method of the RMConnectionHelper class
     * The test consists of checking if the actual getSessionId method return the
     * correct initial value of the sessionId
     */
    @Test
    void getSessionId() {
        assertEquals(RMConnectionHelper.getSessionId(), "");
    }

    /**
     * Enable all Logging in RMConnectionHelper class
     */
    @AfterEach
    void enableLogging() {
        Logger.getLogger(RMConnectionHelper.class).setLevel(Level.ALL);
    }
}