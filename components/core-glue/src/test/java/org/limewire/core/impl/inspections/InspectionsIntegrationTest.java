package org.limewire.core.impl.inspections;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;

import org.limewire.core.impl.CoreGlueModule;
import org.limewire.core.settings.InspectionsSettings;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.DataCategory;
import org.limewire.io.InvalidDataException;
import org.limewire.util.StringUtils;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.Resource;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeWireCoreModule;

/**
 * Real world push inspections integration test.  Brings up LW Core and
 * performs real inspections
 * 
 */
public class InspectionsIntegrationTest extends LimeTestCase {
    
    private static final String INSPECTIONS_REQUEST = "/request";
    private static final String INSPECTIONS_SUBMIT = "/submit";
    private static final String SPEC_FILENAME = "specFile";
    
    /* test inspection points */
    @InspectablePrimitive("test primitive") public static int INSPECTION_ONE;
    @InspectionPoint("test inspectable") public static Inspectable INSPECTION_TWO;
    @InspectionPoint("throwing inspectable") public static Inspectable INSPECTION_THROW;
    @InspectionPoint("null inspectable") public static Inspectable INSPECTION_NULL;
    @InspectablePrimitive(value="usage category", category= DataCategory.USAGE) public static int INSPECTION_USAGE;
    @InspectablePrimitive(value="second usage", category= DataCategory.USAGE) public static int INSPECTION_USAGE_2;
    @InspectablePrimitive(value="third usage", category= DataCategory.USAGE) public static int INSPECTION_USAGE_3;
    
    protected Injector injector;
    private ServerController serverController;
    private InspectionsCommunicatorImpl ic;

    public InspectionsIntegrationTest(String name) {
        super(name);
    }


    @Override
    protected void setUp() throws Exception {
        injector = createInjector(getModules());
        initializeInspectionPoints();
        ensureInspectionsCommunicatorStopped();
        initSettings();
        serverController = new ServerController();
    }

    @Override
    public void tearDown() throws Exception {
        serverController.stopServer();
        ensureInspectionsCommunicatorStopped();
    }
    
    private void ensureInspectionsCommunicatorStopped() {
        if (ic != null) {
            ic.stop();
            ic = null;
        }    
    }
    
    private void startInspectionsCommunicator() {
        ic = (InspectionsCommunicatorImpl)injector.getInstance(InspectionsCommunicator.class);
        ic.initialize();
        ic.start();   
    }
    
    private void initSettings() {
        InspectionsSettings.PUSH_INSPECTIONS_ON.set(1f);
        InspectionsSettings.INSPECTION_SPEC_REQUEST_URL.set("http://localhost:8123/request");
        InspectionsSettings.INSPECTION_SPEC_SUBMIT_URL.set("http://localhost:8123/submit");
        InspectionsSettings.INSPECTION_SPEC_MINIMUM_INTERVAL.set(0);
    }

    private void initializeInspectionPoints() {
        INSPECTION_ONE = 1;
        INSPECTION_USAGE = 2;
        INSPECTION_TWO = new Inspectable() {
            public int count = 0;

            @Override public Object inspect() {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("integer insp", 5);
                map.put("string insp", "test");
                map.put("count value", count++);
                return map;
            }
        };
        INSPECTION_THROW = new Inspectable() {
            @Override public Object inspect() { throw new RuntimeException("error in inspection"); }
        };
        INSPECTION_NULL = new Inspectable() {
            @Override public Object inspect() { return null; }
        };
        
    }
    
    private Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.DEVELOPMENT, modules);
    }
    
    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCoreModule());
        modules.add(new CoreGlueModule());
        return modules.toArray(new Module[modules.size()]);
    }

    /**
     * Test repeating scheduled inspections.
     */
    public void testScheduledRepeatingInspections() throws Exception {
                
        List<String> inspections = 
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        
        // inspect INSPECTION_ONE after a 3 second delay, and every 5 second interval
        specs.add(new InspectionsSpec(inspections, 3L, 5L));
        
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);

        // after each inspection result is received, increment INSPECTION_ONE
        // to make sure the value is incremented the next time we get INSPECTION_ONE inspection results
        ServerResultNotifier resultNotifier = new ServerResultNotifier(3) {
            @Override public void handleReceivedData(byte[] received) {
                super.handleReceivedData(received);
                INSPECTION_ONE++;
            }
        };
        serverController.startServer(resultNotifier);
        startInspectionsCommunicator();
        
        boolean receivedDataBeforeTimeout = serverController.waitForServerResults(15);
        assertTrue(receivedDataBeforeTimeout);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(3, listInspDataEncoded.size());
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        InspectionDataContainer listInspData2 = parseInspectionData(listInspDataEncoded.get(1));
        InspectionDataContainer listInspData3 = parseInspectionData(listInspDataEncoded.get(2));
        assertEquals(1, listInspData1.getResultCount());
        assertEquals(1, listInspData2.getResultCount());
        assertEquals(1, listInspData3.getResultCount());
        
        // verify each inspection result
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(inspections.get(0)))));
        assertEquals(Integer.valueOf(2), Integer.valueOf(new String((byte[])listInspData2.getData(inspections.get(0)))));
        assertEquals(Integer.valueOf(3), Integer.valueOf(new String((byte[])listInspData3.getData(inspections.get(0)))));
    }

    /**
     * Test multiple inspection points contained in 1 inspection spec.
     */
    public void testMultipleInspectionsInOneInspectionsSpec() throws Exception {
        List<String> inspections =
            Arrays.asList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE",
                          "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_TWO");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 5, 0));
   
        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();
   
        // wait for inspection data to arrive at web server
        long origTime = System.currentTimeMillis();
        
        // expecting 1, and only 1 
        boolean receivedMoreThanOneInspectionData = serverController.waitForServerResults(15);
        assertFalse(receivedMoreThanOneInspectionData);
   
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(2, listInspData1.getResultCount());
        assertEquals(5, Math.round(((listInspData1.getTimestamp()-origTime)/1000.0)));
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(
                inspections.get(0)))));
   
        Map<?,?> map = (Map<?,?>)listInspData1.getData(inspections.get(1));
        int integerInsp = ((Long)map.get("integer insp")).intValue();
        String stringInsp = new String((byte[])map.get("string insp"));
        int countValue = ((Long)map.get("count value")).intValue();
        assertEquals(5, integerInsp);
        assertEquals("test", stringInsp);
        assertEquals(0, countValue);
    }

    /**
     * Test when the http server returns inspection points that the client
     * cannot perform because the inspection points do not exist.
     */
    @SuppressWarnings("unchecked")
    public void testScheduledNonExistentInspections() throws Exception {
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:DOES_NOT_EXIST");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 3L, 0));

        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();
        
        // expecting 1, and only 1 
        boolean receivedMoreThanOneInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedMoreThanOneInspectionData);

        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();

        // expecting no inspections results
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer inspData = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(1, inspData.getResultCount());
        Map<String, Object> inspresult = (Map<String, Object>)inspData.getData(inspections.get(0));
        assertEquals(1, inspresult.size());
        assertEquals("java.lang.NoSuchFieldException: DOES_NOT_EXIST",
                StringUtils.toUTF8String((byte[])inspresult.get("error")));
    }

    /**
     * Test an inspection point which throws an Exception during the inspect() call.
     */
    public void testThrowingInspection() throws Exception {
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_THROW");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 2, 0));

        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();
    
        // expecting 1, and only 1 
        boolean receivedMoreThanOneInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedMoreThanOneInspectionData);

        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();

        // expecting inspection result with error
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer inspData = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(1, inspData.getResultCount());
        Map<?, ?> map = (Map<?, ?>)inspData.getData(inspections.get(0));
        assertEquals(1, map.size());
        assertEquals("java.lang.RuntimeException: error in inspection",
                     StringUtils.toUTF8String((byte[])map.get("error")));
    }

    /**
     * Test when the http server returns an empty list of inspections in its inspection spec.
     */
    public void testServerSendsNoInspections() throws Exception {
        List<InspectionsSpec> specs = Collections.emptyList();
        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();
    
        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(10);
        assertFalse(receivedInspectionData);

        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();

        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());
    }
    
    /**
     * Tests default inspections result processor, which maintains a queue of
     * inspections results and a dedicated sending thread.
     *
     * This test attempts to insert more inspections results into the queue
     * than the queue capacity
     *
     * @throws Exception on error
     */
    public void testUnsuccessfulSendOverflow() throws Exception {
        final int SEND_QUEUE_SIZE = 10;
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();

        // try to put (SEND_QUEUE_SIZE+5) inspections results simultaneously.
        // The server delay in responding will make sure excess inspections results
        // are attempted
        for (int i=0; i< SEND_QUEUE_SIZE + 5; i++) {
            specs.add(new InspectionsSpec(inspections, 1, 9000));
        }
        
        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.setSubmissionDelayBeforeResponse(1000);
        serverController.startServer(new ServerResultNotifier(SEND_QUEUE_SIZE+1));
        startInspectionsCommunicator();
    
        boolean receivedInspectionData = serverController.waitForServerResults(16);
        assertTrue(receivedInspectionData);

        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();

        // expecting only as many results as the size of the queue + 1
        // 1 is attempted to be sent, SEND_QUEUE_SIZE are queued up and sent sequentially,
        // Remaining excess inspection results
        // are not sent, and their inspection specs cancelled
        assertEquals(SEND_QUEUE_SIZE+1, listInspDataEncoded.size());
    }

    /**
     * Test client behavior when the server is not up .
     */
    public void testServerNotUp() throws Exception {
        InspectionsSettings.INSPECTION_SPEC_REQUEST_URL.set("http://localhost:9999/request");
        InspectionsSettings.INSPECTION_SPEC_SUBMIT_URL.set("http://localhost:9999/submit");

        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();
        
        boolean receivedData = serverController.waitForServerResults(5);
        assertFalse(receivedData);
        assertEquals(0, serverController.getReceivedInspectionData().size());
        
        // confirm that no inspections are ever set because of failure to contact the LW http server
        Field field = ic.getClass().getDeclaredField("inspectionsSpecs");
        field.setAccessible(true);
        Object inspectionsReceivedFromServer = field.get(ic);
        assertEquals(Collections.emptyList(), inspectionsReceivedFromServer);
    }

    /**
     * Test client behavior when the server returns garbage data (data that cannot
     * be bdecoded and gunzipped into inspection spec information).
     */
    public void testBadDataFromServer() throws Exception {
        serverController.setInspSpecsToReturnBytes("sdukfdfgdhsdkfhskhdfsd".getBytes());
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();
        
        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(15);
        assertFalse(receivedInspectionData);

        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();

        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());
    }
    
    /**
     * Test an inspection point which specifies an interval that is too short.
     */
    public void testInvalidInterval() throws Exception {
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
   
        InspectionsSettings.INSPECTION_SPEC_MINIMUM_INTERVAL.set(60);
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 0, 5));

        // start web server and lw services
        serverController.setInspSpecsToReturn(specs);        
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();

        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(0, listInspDataEncoded.size());
    }
    
    /**
     * Test an inspection point which specifies a negative interval
     */
    public void testNegativeInterval() throws Exception {
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 0, -1));
   
        // start web server and lw services
        serverController.setInspSpecsToReturn(specs);        
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();

        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(0, listInspDataEncoded.size());
    }
    
    /**
     * Test an inspection point which specifies a negative interval
     */
    public void testNegativeInterval2() throws Exception {
        List<String> inspections =
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 0, -100));
   
        // start web server and lw services
        serverController.setInspSpecsToReturn(specs);        
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();

        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(0, listInspDataEncoded.size());
    }

    /**
     * Test when the http server returns a 404 server error.
     */
    public void testErrorFromServer() throws Exception {
        serverController.startServer(new ServerResultNotifier(1));
        startInspectionsCommunicator();
        
        // expecting no responses
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertFalse(receivedInspectionData);
   
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
   
        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());
    }
    
    /**
     * Test usage inspections results when {@link ApplicationSettings#ALLOW_ANONYMOUS_STATISTICS_GATHERING}
     * is on.
     * 
     * Since the setting is on, we should receive results for all inspections, including
     * results for inspections for which DataCategory = USAGE.
     *
     */
    public void testUsageSettingTrue() throws Exception {
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.set(true);
        List<String> inspections =
            Arrays.asList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_USAGE",
                    "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 1L, 3L));

        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();

        // wait until we get 2 responses, up to 5 seconds
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertTrue(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(2, listInspDataEncoded.size());
        
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(2, listInspData1.getResultCount());
        assertEquals(Integer.valueOf(2), Integer.valueOf(new String((byte[])listInspData1.getData(inspections.get(0)))));        
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(inspections.get(1)))));
        
        InspectionDataContainer listInspData2 = parseInspectionData(listInspDataEncoded.get(1));
        assertEquals(2, listInspData2.getResultCount());
        assertEquals(Integer.valueOf(2), Integer.valueOf(new String((byte[])listInspData2.getData(inspections.get(0)))));        
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData2.getData(inspections.get(1)))));
    }
    
    /**
     * Test usage inspections results when {@link ApplicationSettings#ALLOW_ANONYMOUS_STATISTICS_GATHERING}
     * is off.
     * 
     * Since the setting is off, we should not receive any results for inspections for which
     * DataCategory = USAGE.  We should get an error, and then thereafter, we should not receive
     * any results for that USAGE inspection point.  But we should continue to receive results for
     * non-usage inspection points. 
     *
     */
    public void testUsageSettingFalse() throws Exception {
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.set(false);
        List<String> inspections =
            Arrays.asList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_USAGE",
                    "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 1L, 3L));

        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();

        // wait until we get 2 responses, up to 5 seconds
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertTrue(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(2, listInspDataEncoded.size());

        // 1st result should contain 2 inspection results:
        //    INSPECTION_USAGE returns error because ALLOW_ANONYMOUS_STATISTICS_GATHERING is false.
        //                     The Client should not send this inspection again.
        //    INSPECTION_ONE   returns result because it is not a usage inspection.
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(2, listInspData1.getResultCount());
        Map<?, ?> map = (Map<?, ?>)listInspData1.getData(inspections.get(0));
        assertEquals(1, map.size());
        assertTrue(StringUtils.toUTF8String((byte[])map.get("error")).endsWith(
            " is usage data, but usage data collection not allowed"));
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(inspections.get(1)))));
        
        // 2nd result should contain 1 inspection result, the non-usage inspection
        InspectionDataContainer listInspData2 = parseInspectionData(listInspDataEncoded.get(1));
        assertEquals(1, listInspData2.getResultCount());
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData2.getData(inspections.get(1)))));
    }

    /**
     * Test usage inspections results when {@link ApplicationSettings#ALLOW_ANONYMOUS_STATISTICS_GATHERING}
     * is off.  In this test, all inspection points used are USAGE inspection points.
     */
    public void testUsageSettingFalseWhenAllInspectionsAreUsage() throws Exception {
        ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.set(false);
        List<String> inspections =
            Arrays.asList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_USAGE",
                    "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_USAGE_2",
                    "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_USAGE_3");
        List<InspectionsSpec> specs = new ArrayList<InspectionsSpec>();
        specs.add(new InspectionsSpec(inspections, 1L, 3L));

        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(specs);
        serverController.startServer(new ServerResultNotifier(2));
        startInspectionsCommunicator();

        // make sure we only receive 1 set of inspection results, and none thereafter
        // this set of inspection results contains only errors saying that "usage collection not allowed."
        boolean receivedInspectionData = serverController.waitForServerResults(7);
        assertFalse(receivedInspectionData);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(1, listInspDataEncoded.size());

        // 1st result should contain 2 inspection results:
        //    INSPECTION_USAGE returns error because ALLOW_ANONYMOUS_STATISTICS_GATHERING is false.
        //                     The Client should not send this inspection again.
        //    INSPECTION_ONE   returns result because it is not a usage inspection.
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(3, listInspData1.getResultCount());
        Map<?, ?> map = (Map<?, ?>)listInspData1.getData(inspections.get(0));
        Map<?, ?> map2 = (Map<?, ?>)listInspData1.getData(inspections.get(1));
        Map<?, ?> map3 = (Map<?, ?>)listInspData1.getData(inspections.get(2));
        assertEquals(1, map.size());
        assertTrue(StringUtils.toUTF8String((byte[])map.get("error")).endsWith(
            " is usage data, but usage data collection not allowed"));
        assertEquals(1, map2.size());
        assertTrue(StringUtils.toUTF8String((byte[])map2.get("error")).endsWith(
            " is usage data, but usage data collection not allowed"));
        assertEquals(1, map3.size());
        assertTrue(StringUtils.toUTF8String((byte[])map3.get("error")).endsWith(
            " is usage data, but usage data collection not allowed"));
        
    }
    
    /**
     * Test that server redirects are followed.
     */
    public void testRedirectsFollowed() throws Exception {
   
        // start server which returns inspections specs
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.singletonList(
                "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        inspSpecs.add(new InspectionsSpec(inspections, 3, 0));
        
        // start web server and lw services
        // initialize server with specs I want returned
        serverController.setInspSpecsToReturn(inspSpecs);
        serverController.startServer(new ServerResultNotifier(1));
        
        // start server which redirects to inspections server
        ResourceHandler requestHandler = new ResourceHandler() {
            @Override
            public void handleGet(org.mortbay.http.HttpRequest httpRequest,
                              org.mortbay.http.HttpResponse httpResponse,
                              String s, java.lang.String s1,
                              org.mortbay.util.Resource resource) throws java.io.IOException {
                String path = httpRequest.getURI().getPath();
                String urlRedirect = "http://localhost:8123" + path + "?" + httpRequest.getQuery();
                httpResponse.sendRedirect(urlRedirect);
                httpResponse.commit();
                httpRequest.setHandled(true);
            }
        };

        HttpServer redirectingServer = new HttpServer();
        SocketListener listener = new SocketListener();
        listener.setPort(8124);
        listener.setMinThreads(1);
        redirectingServer.addListener(listener);
        HttpContext context = redirectingServer.addContext("");
        context.setResourceBase("");
        requestHandler.setAcceptRanges(true);
        requestHandler.setDirAllowed(true);
        context.addHandler(requestHandler);
        context.addHandler(new NotFoundHandler());
        redirectingServer.start();
        
        
        // modify settings so that client points to redirecting server for request URL only
        // inspections submissions to the server currently do not redirect
        InspectionsSettings.INSPECTION_SPEC_REQUEST_URL.set("http://localhost:8124/request");

        // start inspections communicator
        startInspectionsCommunicator();

        // wait for inspection to be performed, and results sent to the redirecting server
        boolean receivedInspectionData = serverController.waitForServerResults(5);
        assertTrue(receivedInspectionData);

        // verify inspections results from inspections server are what we expect
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(1, listInspData1.getResultCount());
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(
                inspections.get(0)))));
    } 
    
    private InspectionDataContainer parseInspectionData(byte[] data) throws InvalidDataException {
        return new InspectionDataContainer(data);
    }

    /**
     * HTTP Request handler code. May override {@link #sendInspectionSpecs}
     * method to specify behavior than just uploading the expected file.
     */
    private class ServerController extends ResourceHandler {
        
        private final String SERVER_ROOT_DIR = _baseDir.getAbsolutePath();
        private List<byte[]> inspectionDataEncoded = new ArrayList<byte[]>();
        private int delay = 0;           // delay in seconds prior to response
        private ServerResultNotifier serverResultNotifier = null;
        
        private final HttpServer server = new HttpServer();
        private final CountDownLatch latch = new CountDownLatch(1);
        

        void startServer(ServerResultNotifier serverResultNotifier) throws Exception {
            this.serverResultNotifier = serverResultNotifier;
            SocketListener listener = new SocketListener();
            listener.setPort(8123);
            listener.setMinThreads(1);
            server.addListener(listener);

            HttpContext context = server.addContext("");

            context.setResourceBase(SERVER_ROOT_DIR);
            setAcceptRanges(true);
            setDirAllowed(true);

            context.addHandler(this);
            context.addHandler(new NotFoundHandler());
            server.start();
        }
        
        void stopServer() {
            try {
                server.stop();
            } catch (InterruptedException e) {
                // ignoring
            }
        }
        
        void setInspSpecsToReturn(List<InspectionsSpec> specs) throws IOException {
            setInspSpecsToReturnBytes(InspectionsTestUtils.getGzippedAndBencoded(specs));    
        }
        
        void setInspSpecsToReturnBytes(byte[] specAsBytes) throws IOException {
            FileOutputStream os = new FileOutputStream(SERVER_ROOT_DIR + "/" + SPEC_FILENAME);
            os.write(specAsBytes);
            os.close();
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void handleGet(org.mortbay.http.HttpRequest httpRequest,
                              org.mortbay.http.HttpResponse httpResponse,
                              String s, java.lang.String s1,
                              org.mortbay.util.Resource resource) throws java.io.IOException {
            super.handleGet(httpRequest, httpResponse, s, s1, resource);
            Set<String> params = httpRequest.getURI().getParameterNames();
            String path = httpRequest.getURI().getPath();
            assertTrue(params.containsAll(Arrays.asList("lv", "guid", "urs")));
            if (path.equals(INSPECTIONS_REQUEST)) {
                sendInspectionSpecs(httpRequest, httpResponse);
            } else if (path.equals(INSPECTIONS_SUBMIT)) {
                // set success on response
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
                int lengthOfData = httpRequest.getContentLength();
                byte[] b = new byte[lengthOfData];
                assertEquals(lengthOfData, httpRequest.getInputStream().read(b));
                inspectionDataEncoded.add(b);
                delayResponseIfNecessary();
                if (serverResultNotifier != null) {
                    serverResultNotifier.handleReceivedData(b);
                }
            } else {
                fail("Invalid request: " + path);
            }
            httpResponse.commit();
            httpRequest.setHandled(true);
        }
        
        void setSubmissionDelayBeforeResponse(int delay) {
            this.delay = delay;    
        }
        
        private void delayResponseIfNecessary() {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                }
            }
        }

        protected void sendInspectionSpecs(org.mortbay.http.HttpRequest httpRequest,
                                           org.mortbay.http.HttpResponse httpResponse) 
                                           throws IOException {
            // send back gzipped-bencoded list of maps
            httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
            httpResponse.setContentType("binary/octet-stream");
            Resource res = getResource(SPEC_FILENAME);
            sendData(httpRequest, httpResponse, null, res, true);
            httpResponse.commit();
            httpRequest.setHandled(true);
        }
        
        List<byte []> getReceivedInspectionData() {
            return inspectionDataEncoded;
        }

        public boolean waitForServerResults(int timeout) throws Exception {
            return latch.await(timeout, TimeUnit.SECONDS);
        }

        void finishedResults() {
            latch.countDown();    
        }
    }
    
    private class ServerResultNotifier {
        int numResultsExpected;
        
        ServerResultNotifier(int numResultsExpected) {
            this.numResultsExpected = numResultsExpected;    
        }
        
        public void handleReceivedData(byte[] received) {
            numResultsExpected--;
            if (numResultsExpected == 0) {
                serverController.finishedResults();
            }
        }
    }
}
