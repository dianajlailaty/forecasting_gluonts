package org.activeeon.morphemic.infrastructure.deployment;

import lombok.SneakyThrows;
import org.activeeon.morphemic.service.RMConnectionHelper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.resourcemanager.common.NSState;
import org.ow2.proactive_grid_cloud_portal.common.RMRestInterface;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PAResourceManagerGatewayTest {

    private static final Logger LOGGER = Logger.getLogger(PAResourceManagerGatewayTest.class);

    PAResourceManagerGateway paResourceManagerGateway;

    private final String DUMMY_USERNAME = "username";

    private final String DUMMY_PASSWORD = "password";

    private final String DUMMY_NODE_SOURCE = "nodeSource";

    private final String DUMMY_NODE_SOURCE_URL = "http://notExist.activeeon.com:8080/nodes/1435";

    private final String INVALID_DUMMY_NODE_SOURCE_URL = "http://notExist.activeeon.com:8080/nodes/3412";

    private final String INVALID_DUMMY_NODE_SOURCE = "invalidNodeSource";

    private final String DUMMY_SESSION_ID = "sessionId";

    private final boolean preempt = true;

    private final List<String> DUMMY_TAGS = IntStream.range(1,4).mapToObj(i-> "Tag"+ i).collect(Collectors.toList());

    private final Set<String> DUMMY_NODES_URLS = new HashSet<>();

    // Enable the mocking of static methods
    MockedStatic<RMConnectionHelper> mb = Mockito.mockStatic(RMConnectionHelper.class);

    @Mock
    private RMRestInterface rmRestInterface;

    /**
     * Enable all mocks
     * Set up the required resources to build the tests
     */
    @SneakyThrows
    @BeforeEach
    void setUp() {
        // Enable all mocks
        MockitoAnnotations.openMocks(this);

        for(int i=0; i<4; i++){
            DUMMY_NODES_URLS.add("http://notExist.activeeon.com:808"+i);
        }

        String DUMMY_CONNECTION_URL = "http://notExist.activeeon.com:8080";
        paResourceManagerGateway = new PAResourceManagerGateway(DUMMY_CONNECTION_URL);

        mb.when(RMConnectionHelper::getSessionId).thenReturn(DUMMY_SESSION_ID);

        // For the connect method of the PAResourceManagerGateway class
        mb.when(()->RMConnectionHelper.connect(DUMMY_USERNAME, DUMMY_PASSWORD)).thenAnswer(invocation -> {
            when(rmRestInterface.isActive(anyString())).thenReturn(false);
            mb.when(RMConnectionHelper::getSessionId).thenReturn(DUMMY_SESSION_ID);
           return null;
        });

        // For the disconnect method of the PAResourceManagerGateway class
        mb.when(RMConnectionHelper::disconnect).thenAnswer(invocation -> {
            mb.when(RMConnectionHelper::getSessionId).thenReturn("");
            return null;
        });


        // For the searchNodes method of the PAResourceManagerGateway class
        try {
            when(rmRestInterface.searchNodes(RMConnectionHelper.getSessionId(), DUMMY_TAGS, true)).thenReturn(DUMMY_NODES_URLS);
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the undeployNodeSource method of the PAResourceManagerGateway class
        try {
            NSState correctNSState = new NSState();
            correctNSState.setResult(true);
            NSState incorrectNSState = new NSState();
            incorrectNSState.setResult(false);
            when(rmRestInterface.undeployNodeSource(RMConnectionHelper.getSessionId(), DUMMY_NODE_SOURCE, preempt)).thenReturn(correctNSState);
            when(rmRestInterface.undeployNodeSource(RMConnectionHelper.getSessionId(), not(eq(DUMMY_NODE_SOURCE)), preempt)).thenReturn(incorrectNSState);
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the removeNodeSource method of the PAResourceManagerGateway class
        try {
            when(rmRestInterface.removeNodeSource(RMConnectionHelper.getSessionId(), DUMMY_NODE_SOURCE, preempt)).thenReturn(true);
            when(rmRestInterface.removeNodeSource(RMConnectionHelper.getSessionId(), not(eq(DUMMY_NODE_SOURCE)), preempt)).thenReturn(false);
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the releaseNodeSource method of the PAResourceManagerGateway class
        try {
            when(rmRestInterface.releaseNode(RMConnectionHelper.getSessionId(), DUMMY_NODE_SOURCE_URL)).thenReturn(true);
            when(rmRestInterface.releaseNode(RMConnectionHelper.getSessionId(), not(eq(DUMMY_NODE_SOURCE_URL)))).thenReturn(false);
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the removeNode method of the PAResourceManagerGateway class
        try {
            when(rmRestInterface.removeNode(RMConnectionHelper.getSessionId(), DUMMY_NODE_SOURCE_URL, preempt)).thenReturn(true);
            when(rmRestInterface.removeNode(RMConnectionHelper.getSessionId(), not(eq(DUMMY_NODE_SOURCE_URL)), preempt)).thenReturn(false);
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For all test methods
        paResourceManagerGateway.setRmRestInterface(rmRestInterface);

        // Temporary disable all Logging from the RMConnectionHelper class
        // It is enabled after the tests are completed
        Logger.getLogger(RMConnectionHelper.class).setLevel(Level.OFF);
        Logger.getLogger(PAResourceManagerGateway.class).setLevel(Level.OFF);
    }

    /**
     * Enable all loggers and close the static mocking after each test
     */
    @AfterEach
    void tearDown() {
        // Enable all loggers
        Logger.getLogger(RMConnectionHelper.class).setLevel(Level.ALL);
        Logger.getLogger(PAResourceManagerGateway.class).setLevel(Level.ALL);

        // Close the static mocking
        mb.close();
    }

    /**
     * Test the connect method of the PAResourceManagerGateway class
     */
    @SneakyThrows
    @Test
    void connect() {
        try {
            paResourceManagerGateway.connect(DUMMY_USERNAME, DUMMY_PASSWORD);
            assertEquals(DUMMY_SESSION_ID, paResourceManagerGateway.getSessionId());
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Test the disconnect method of the PAResourceManagerGateway class
     */
    @Test
    void disconnect() {
        paResourceManagerGateway.disconnect();
        assertEquals("", paResourceManagerGateway.getSessionId());
    }

    /**
     *
     */
    @Ignore
    @Test
    void getAsyncDeployedNodesInformation() {
        // TODO
    }

    /**
     *
     */
    @Ignore
    @Test
    void deploySimpleAWSNodeSource() {
        // TODO
    }

    /**
     * Test the searchNodes method of the PAResourceManagerGateway class
     * The result should be equal to the dummy list defined in the setUp function
     */
    @SneakyThrows
    @Test
    void searchNodes() {
        try {
            List<String> result = new ArrayList<>(paResourceManagerGateway.searchNodes(DUMMY_TAGS, true));
            assertEquals(new ArrayList<>(DUMMY_NODES_URLS).toString(), result.toString());
            assertEquals(DUMMY_NODES_URLS.size(), result.size());
        }catch(Exception e){
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Test the undeployNodeSource of the PAResourceManagerGateway class
     * The result NSState should be equal to the dummy NSState defined in the setUp method
     */
    @Test
    void undeployNodeSource() {
        try {
            NSState currentNSState = paResourceManagerGateway.undeployNodeSource(DUMMY_NODE_SOURCE, preempt);
            assertTrue(currentNSState.isResult());
            NSState incorrectCurrentNSState = paResourceManagerGateway.undeployNodeSource(INVALID_DUMMY_NODE_SOURCE, preempt);
            assertFalse(incorrectCurrentNSState.isResult());
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Test the removeNodeSource of the PAResourceManagerGateway class
     * Testing for a valid and invalid NodeSource
     */
    @Test
    void removeNodeSource() {
        try{
            assertTrue(paResourceManagerGateway.removeNodeSource(DUMMY_NODE_SOURCE, preempt));
            assertFalse(paResourceManagerGateway.removeNodeSource(INVALID_DUMMY_NODE_SOURCE, preempt));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Test the releaseNode of the PAResourceManagerGateway class
     * Testing for a valid and invalid NodeSource url
     */
    @Test
    void releaseNode() {
        try{
            assertTrue(paResourceManagerGateway.releaseNode(DUMMY_NODE_SOURCE_URL));
            assertFalse(paResourceManagerGateway.releaseNode(INVALID_DUMMY_NODE_SOURCE_URL));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }
    }

    /**
     * Test the removeNode of the PAResourceManagerGateway class
     * Testing for a valid and invalid NodeSource url
     */
    @Test
    void removeNode() {
        try{
            assertTrue(paResourceManagerGateway.removeNode(DUMMY_NODE_SOURCE_URL, preempt));
            assertFalse(paResourceManagerGateway.removeNode(INVALID_DUMMY_NODE_SOURCE_URL, preempt));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }
    }
}