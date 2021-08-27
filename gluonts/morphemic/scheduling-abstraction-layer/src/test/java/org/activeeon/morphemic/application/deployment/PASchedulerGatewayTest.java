package org.activeeon.morphemic.application.deployment;

import lombok.SneakyThrows;
import org.activeeon.morphemic.service.SchedulerConnectionHelper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.job.JobIdImpl;
import org.ow2.proactive_grid_cloud_portal.smartproxy.RestSmartProxyImpl;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PASchedulerGatewayTest {

    private static final Logger LOGGER = Logger.getLogger(PASchedulerGatewayTest.class);

    private final String DUMMY_USERNAME = "username";

    private final String DUMMY_PASSWORD = "password";

    private final TaskFlowJob DUMMY_JOB = new TaskFlowJob();

    private final long DUMMY_JOB_ID = 1234;

    private final String DUMMY_TASK_NAME = "dummyTask";

    private final String DUMMY_TASK_ID = "dummyTaskId";

    private final long DUMMY_RESULT = 0;

    private final File DUMMY_XML_FILE = new File("");

    private final Map<String, String> DUMMY_VARIABLES = new HashMap<>();

    private final long DUMMY_TIMEOUT = 10000;

    private final List<String> DUMMY_JOB_IDS = IntStream.range(1,4).mapToObj(i -> "JobId"+i).collect(Collectors.toList());

    private PASchedulerGateway schedulerGateway;

    // Enable the mocking of static methods
    MockedStatic<SchedulerConnectionHelper> mb = Mockito.mockStatic(SchedulerConnectionHelper.class);

    @Mock
    private RestSmartProxyImpl restSmartProxy;

    @Mock
    private JobResult jobResult;

    @Mock
    private Map<Long, Map<String, Serializable>> dummyResults;

    @Mock
    private JobState jobState;

    @Mock
    private TaskResult taskResult;

    @Mock
    private TaskId taskId;

    /**
     * Enable all mocks
     * Set up the required resources to build the tests
     */
    @SneakyThrows
    @BeforeEach
    void setUp() {
        // Enable all mocks
        MockitoAnnotations.openMocks(this);

        String DUMMY_CONNECTION_URL = "http://notExist.activeeon.com:8080";
        schedulerGateway = new PASchedulerGateway(DUMMY_CONNECTION_URL);

        // For the connect function of PASchedulerGateway class
        // Mock the restSmartProxy in the SchedulerConnectionHelper connect method to return a successful connection
        // when valid credentials are used and return false otherwise
        mb.when(()->SchedulerConnectionHelper.connect(DUMMY_USERNAME,DUMMY_PASSWORD)).then(implementation->{
            when(restSmartProxy.isConnected()).thenReturn(true);
            return restSmartProxy;
        });
        mb.when(()->SchedulerConnectionHelper.connect((not(eq(DUMMY_USERNAME))),not(eq(DUMMY_PASSWORD)))).then(implementation->{
           when(restSmartProxy.isConnected()).thenReturn(false);
            return restSmartProxy;
        });

        // For the disconnect function of PASchedulerGateway class
        // Mock the restSmartProxy in the SchedulerConnectionHelper disconnect method to return a successful disconnection
        mb.when(SchedulerConnectionHelper::disconnect).then(implementation->{
            mb.when(()->restSmartProxy.isConnected()).thenReturn(false);
            return restSmartProxy;
        });

        // For submit with Job method of PASchedulerGateway class
        // Mock the restSmartProxy to return a successful job submission with a dummy id when a dummy TaskFlowJob is submitted
        try {
            when(restSmartProxy.submit(DUMMY_JOB)).thenReturn(new JobIdImpl(DUMMY_JOB_ID, ""));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For submit with XML file method of PASchedulerGateway class
        // Mock the restSmartProxy to return a successful job submission with a dummy id when a dummy XML file is submitted
        try {
            when(restSmartProxy.submit(DUMMY_XML_FILE)).thenReturn(new JobIdImpl(DUMMY_JOB_ID, ""));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For submit with XML file and variables method of PASchedulerGateway class
        // Mock the restSmartProxy to return a successful job submission with a dummy id when a dummy XML file and dummy variables are submitted
        try {
            when(restSmartProxy.submit(DUMMY_XML_FILE, DUMMY_VARIABLES)).thenReturn(new JobIdImpl(DUMMY_JOB_ID, ""));
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the getJobState method of PASchedulerGateway class
        // Mock the restSmartProxy to return a dummy JobState when the method is called
        try {
            when(jobState.getId()).thenReturn(new JobIdImpl(DUMMY_RESULT, ""));
            when(restSmartProxy.getJobState(String.valueOf(DUMMY_JOB_ID))).thenReturn(jobState);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the waitForJob method of PASchedulerGateway class
        // Mock the restSmartProxy to return a dummy JobResult when the method is called
        try {
            when(jobResult.getJobId()).thenReturn(new JobIdImpl(DUMMY_JOB_ID, ""));
            when(restSmartProxy.waitForJob(String.valueOf(DUMMY_JOB_ID), DUMMY_TIMEOUT)).thenReturn(jobResult);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the getJobResultMaps method of PASchedulerGateway class
        // Mock the restSmartProxy to return a dummy JobResults when the method is called
        try {
            // To avoid creating a map, the MAP class is mocked in sort that the size() function return a static value
            when(dummyResults.size()).thenReturn(3);
            when(restSmartProxy.getJobResultMaps(DUMMY_JOB_IDS)).thenReturn(dummyResults);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the killJob method of PASchedulerGateway class
        // Mock the restSmartProxy to return true when the method is called
        try {
            when(restSmartProxy.killJob(String.valueOf(DUMMY_JOB_ID))).thenReturn(true);
            when(restSmartProxy.killJob(not(eq(String.valueOf(DUMMY_JOB_ID))))).thenReturn(false);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the waitForTask method of PASchedulerGateway class
        // Mock the restSmartProxy to return a dummy TaskResult when the method is called
        try {
            when(taskId.value()).thenReturn(DUMMY_TASK_ID);
            when(taskResult.getTaskId()).thenReturn(taskId);
            when(restSmartProxy.waitForTask(String.valueOf(DUMMY_JOB_ID), DUMMY_TASK_NAME, DUMMY_TIMEOUT)).thenReturn(taskResult);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }

        // For the getTaskResult method of PASchedulerGateway class
        // Mock the restSmartProxy to return a dummy TaskResult when the method is called
        try {
            when(taskId.value()).thenReturn(DUMMY_TASK_ID);
            when(taskResult.getTaskId()).thenReturn(taskId);
            when(restSmartProxy.getTaskResult(String.valueOf(DUMMY_JOB_ID), DUMMY_TASK_NAME)).thenReturn(taskResult);
        }catch (Exception e){
            LOGGER.debug(e.getMessage());
        }


        // For all the remaining methods
        schedulerGateway.setRestSmartProxy(restSmartProxy);

        // Temporary disable all Logging from the RMConnectionHelper class
        // It is enabled after the tests are completed
        Logger.getLogger(SchedulerConnectionHelper.class).setLevel(Level.OFF);
        Logger.getLogger(PASchedulerGateway.class).setLevel(Level.OFF);
    }

    /**
     * Enable all loggers and close the static mocking after each test
     */
    @AfterEach
    void tearDown() {
        // Enable all loggers
        Logger.getLogger(SchedulerConnectionHelper.class).setLevel(Level.ALL);
        Logger.getLogger(PASchedulerGateway.class).setLevel(Level.ALL);

        // Close the static mocking
        mb.close();
    }

    /**
     * Test if the submitWithJob method of the PASchedulerGateway class return the expected value
     */
    @Test
    void submitWithJob() {
        Long jobId = schedulerGateway.submit(DUMMY_JOB).longValue();
        assertEquals(DUMMY_JOB_ID,jobId);
    }

    /**
     * Test if the submitWithJob method (with an XML file as input) of the PASchedulerGateway class return the expected value
     */
    @Test
    void submitWithXML() {
        Long jobId = schedulerGateway.submit(DUMMY_XML_FILE).longValue();
        assertEquals(DUMMY_JOB_ID,jobId);
    }

    /**
     * Test if the submitWithJob method (with an XML file and a list of variables as inputs) of the PASchedulerGateway class return the expected value
     */
    @Test
    void submitWithXMLFileAndAMapOfVariables() {
        Long jobId = schedulerGateway.submit(DUMMY_XML_FILE, DUMMY_VARIABLES).longValue();
        assertEquals(DUMMY_JOB_ID,jobId);
    }

    /**
     * Test if the getJobState method of the PASchedulerGateway class return the expected result
     */
    @Test
    void getJobState() {
        JobState actualJobState = schedulerGateway.getJobState(String.valueOf(DUMMY_JOB_ID));
        assertEquals(DUMMY_RESULT, actualJobState.getId().longValue());
    }

    /**
     * Test if the waitForJob method of the PASchedulerGateway class return the expected result
     */
    @Test
    void waitForJob() {
        JobResult actualJobResult = schedulerGateway.waitForJob(String.valueOf(DUMMY_JOB_ID),DUMMY_TIMEOUT);
        assertEquals(DUMMY_JOB_ID, actualJobResult.getJobId().longValue());
    }

    /**
     * Test if the getJobResults method of the PASchedulerGateway class return the expected result
     */
    @SneakyThrows
    @Test
    void getJobResultMaps() {
        Map<Long, Map<String, Serializable>> actualResults = schedulerGateway.getJobResultMaps(DUMMY_JOB_IDS);
        assertEquals(3, actualResults.size());
    }

    /**
     * Test if the killJob method of the PASchedulerGateway class return the expected result
     */
    @Test
    void killJob() {
        assertTrue(schedulerGateway.killJob(String.valueOf(DUMMY_JOB_ID)));
        long DUMMY_INVALID_JOB_ID = 5678;
        assertFalse(schedulerGateway.killJob(String.valueOf(DUMMY_INVALID_JOB_ID)));
    }

    /**
     * Test if the waitForTask method of the PASchedulerGateway class return the expected result
     */
    @Test
    void waitForTask() {
        TaskId actualTaskId = schedulerGateway.waitForTask(String.valueOf(DUMMY_JOB_ID), DUMMY_TASK_NAME, DUMMY_TIMEOUT).getTaskId();
        assertEquals(DUMMY_TASK_ID, actualTaskId.value());
    }

    /**
     * Test if the getTaskResult method of the PASchedulerGateway class return the expected result
     */
    @Test
    void getTaskResult() {
        TaskId actualTaskId = schedulerGateway.getTaskResult(String.valueOf(DUMMY_JOB_ID), DUMMY_TASK_NAME).getTaskId();
        assertEquals(DUMMY_TASK_ID, actualTaskId.value());
    }

    /**
     * Test the connection method of the PASchedulerGateway class
     * If a valid username and password are used the isConnected() method is expected to return true
     * Otherwise it should return false
     */
    @Test
    void connect() {
        schedulerGateway.connect(DUMMY_USERNAME,DUMMY_PASSWORD);
        assertTrue(schedulerGateway.getRestSmartProxy().isConnected());
        schedulerGateway.connect("wrongUsername", "wrongPassword");
        assertFalse(schedulerGateway.getRestSmartProxy().isConnected());
    }

    /**
     * Test the disconnect method of the PASchedulerGateway class
     * When the function is called, the isConnected() method is expected to return false
     */
    @Test
    void disconnect() {
        schedulerGateway.disconnect();
        assertFalse(schedulerGateway.getRestSmartProxy().isConnected());
    }
}