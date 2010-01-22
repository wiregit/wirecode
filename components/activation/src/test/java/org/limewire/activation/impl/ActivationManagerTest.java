package org.limewire.activation.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.core.impl.CoreGlueModule;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.setting.ActivationSettings;
import org.limewire.util.OSUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeWireCoreModule;

public class ActivationManagerTest extends LimeTestCase {
    
    protected Injector injector;
    private ActivationResponseFactory responseFactory;
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
    private static final Comparator<ActivationItem> comparator = new Comparator<ActivationItem>() {
        @Override
        public int compare(ActivationItem o1, ActivationItem o2) {
            String name1 = o1.getModuleID().name();
            String name2 = o2.getModuleID().name();
            return name1.compareTo(name2);
        }
    };

    public ActivationManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ActivationManagerTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = Guice.createInjector(Stage.DEVELOPMENT, getModules());
        responseFactory = injector.getInstance(ActivationResponseFactory.class);
    }
    
    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCoreModule());
        modules.add(new CoreGlueModule());
        return modules.toArray(new Module[modules.size()]);
    }

    public void testStartServiceSuccessfulActivationWithServer() throws Exception {
        
        String successfulLookupJson = "{\"response\":\"valid\",\"lid\":\"DAVVXXMEBWU3\"," +
                "\"guid\":\"44444444444444444444444444444444\",\"refresh\":1440," +
                "\"installations\":10,\"mcode\":\"20100114|1xm8,2xm8,3xm8,4xm8\"," +
                "\"modulecount\":4,\"modules\":[{\"id\":1,\"name\":\"Turbo-charged downloads\"," +
                "\"pur\":\"20090920\",\"exp\":\"20100920\",\"status\":\"active\"}," +
                "{\"id\":2,\"name\":\"Optimized search results\",\"pur\":\"20090920\"," +
                "\"exp\":\"20100920\",\"status\":\"active\"},{\"id\":3," +
                "\"name\":\"Tech support\",\"pur\":\"20090920\",\"exp\":\"20150920\"," +
                "\"status\":\"unavailable\"},{\"id\":4,\"name\":\"AVG - bundled antivirus software\"," +
                "\"pur\":\"20090920\",\"exp\":\"20500920\",\"status\":\"active\"}]}";
        
        ActivationCommunicator comm = getCommunicatorByJsonResponse(successfulLookupJson); 
        ActivationManagerImpl activationManager = getActivationManager(comm);
        
        ActivationSettings.ACTIVATION_KEY.set("L4RXLP28XVQ5");
        activationManager.start();
        assertTrue("Timed out waiting for activation competion.", 
            waitForSuccessfulActivation(activationManager, 10));
        
        // check for activation items
        List<ActivationItem> items = activationManager.getActivationItems();
        Collections.sort(items, comparator);
        assertEquals(4, items.size());
        ActivationItem item = items.get(0);
        assertEquals(ActivationID.AVG_MODULE, item.getModuleID());
        ActivationItem.Status avgExpectedStatus = 
            OSUtils.isAVGCompatibleWindows() ? ActivationItem.Status.ACTIVE : ActivationItem.Status.UNUSEABLE_OS;
        assertEquals(avgExpectedStatus, item.getStatus());
        assertEquals("20090920", format.format(item.getDatePurchased()));
        assertEquals("20500920", format.format(item.getDateExpired()));
        
        item = items.get(1);
        assertEquals(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, item.getModuleID());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20090920", format.format(item.getDatePurchased()));
        assertEquals("20100920", format.format(item.getDateExpired()));
                
        
        item = items.get(2);
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, item.getModuleID());
        assertEquals(ActivationItem.Status.UNAVAILABLE, item.getStatus());
        assertEquals("20090920", format.format(item.getDatePurchased()));
        assertEquals("20150920", format.format(item.getDateExpired()));
        
        item = items.get(3);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, item.getModuleID());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20090920", format.format(item.getDatePurchased()));
        assertEquals("20100920", format.format(item.getDateExpired()));
    }
    
    public void testStartServiceInvalidKeyShouldNotEvenGoToServer() throws Exception {
        ActivationSettings.ACTIVATION_KEY.set("invalid Key");
        final AtomicBoolean serverContacted = new AtomicBoolean(false);
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                serverContacted.set(true);
                return null;
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.start();
        assertFalse("Activation was successful, but an invalid key error was expected.", 
            waitForSuccessfulActivation(activationManager, 5));
        assertEquals(activationManager.getActivationState(), ActivationState.NOT_AUTHORIZED);
        assertEquals(activationManager.getActivationError(), ActivationError.INVALID_KEY);
        assertEquals(activationManager.getActivationItems(), Collections.<ActivationItem>emptyList());
        assertFalse(serverContacted.get());
    }
    
    public void testStartServiceRetriesServerStaysDown() throws Exception {
        ActivationSettings.ACTIVATION_KEY.set("L4RXLP28XVQ5");
        final AtomicInteger retriesCount = new AtomicInteger(0);
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                retriesCount.incrementAndGet();
                throw new IOException("Server is down!");
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.start();
        assertFalse("Activation was successful, but a time out failure was expected.", 
            waitForSuccessfulActivation(activationManager, 85));
        assertEquals(ActivationState.NOT_AUTHORIZED, activationManager.getActivationState());
        assertEquals(ActivationError.COMMUNICATION_ERROR, activationManager.getActivationError());
        assertEquals(6, retriesCount.get());
        assertEquals(activationManager.getActivationItems(), Collections.<ActivationItem>emptyList());
    }
    
    public void testStartServiceRetriesServerReturnsResponseAfterFewRetries() throws Exception {
        
        final String json = "{\n" +
                "   \"response\":\"valid\",\n" +
                "   \"lid\":\"L4RXLP28XVQ5\",\n" +
                "   \"guid\":\"B3CCBED9E255F33F84F2D2111331256D\",\n" +
                "   \"refresh\":1440,\n" +
                "   \"mcode\":\"20140920|1xm7,2xm7,3xm7,4xm7\",\n" +
                "   \"installations\":1,\n" +
                "   \"modules\":[\n" +
                "      {\n" +
                "         \"id\":1,\n" +
                "         \"name\":\"Turbo-charged downloads\",\n" +
                "         \"pur\":\"20090920\",\n" +
                "         \"exp\":\"20140920\",\n" +
                "         \"status\":\"active\"\n" +
                "      },\n" +
                "   ],\n" +
                "   \"duration\":\"0.005184\"\n" +
                "}";
        
        ActivationSettings.ACTIVATION_KEY.set("L4RXLP28XVQ5");
        final AtomicInteger retriesCount = new AtomicInteger(0);
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                int currentCount = retriesCount.incrementAndGet();
                if (currentCount < 3) {
                    throw new IOException("Server is down!");
                }
                return responseFactory.createFromJson(json);
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.start();
        assertTrue("Timed out waiting for activation competion.", 
            waitForSuccessfulActivation(activationManager, 40));
        assertEquals(ActivationState.AUTHORIZED, activationManager.getActivationState());
        assertEquals(ActivationError.NO_ERROR, activationManager.getActivationError());
        assertEquals(3, retriesCount.get());
        List<ActivationItem> items = activationManager.getActivationItems();
        assertEquals(items.size(), 1);
        ActivationItem item = items.get(0);
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, item.getModuleID());
        assertEquals("Turbo-charged downloads", item.getLicenseName());
        assertEquals(ActivationItem.Status.ACTIVE, item.getStatus());
        assertEquals("20090920", format.format(item.getDatePurchased()));
        assertEquals("20140920", format.format(item.getDateExpired()));
    }
    
    // tests the "notfound" json response.  In particular, tests to make sure that:
    //
    // 1. After receiving "notfound", LW should erase Mcode and License Key
    // 2. Should not contact the activation server on startup
    //
    public void testNotFoundResponseErasesKeyAndMcodeNoAutoStart() throws Exception {
        String KEY = "L4RXLP28XVQ5";
        ActivationSettings.ACTIVATION_KEY.set(KEY);
        final String json = "{\"response\":\"notfound\",\"lid\":\"HT5YXS7CWGRG\"," +
                             "\"guid\":\"44444444444444444444444444444444\",\"refresh\":1440," +
                             "\"mcode\":\"\",\"duration\":\"0.001737\"}";
        
        final AtomicInteger retriesCount = new AtomicInteger(0);
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                retriesCount.incrementAndGet();
                return responseFactory.createFromJson(json);
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.activateKey(KEY);
        assertFalse(waitForSuccessfulActivation(activationManager, 5));
        assertEquals(activationManager.getActivationState(), ActivationState.NOT_AUTHORIZED);
        assertEquals(activationManager.getActivationError(), ActivationError.INVALID_KEY);
        assertEquals(activationManager.getActivationItems(), Collections.<ActivationItem>emptyList());
        assertEquals("", ActivationSettings.ACTIVATION_KEY.get());
        assertEquals("", ActivationSettings.M_CODE.get());
        
        // after "notfound" is received and processed, calling start() on activation manager
        // should not result in contacting the activation server
        assertEquals(1, retriesCount.get());
        activationManager = getActivationManager(comm);
        activationManager.start();
        waitForSuccessfulActivation(activationManager, 5);
        assertEquals(1, retriesCount.get());
    }
    
    // tests the "stop" json response.  In particular, tests to make sure that:
    //
    // 1. After receiving "stop", LW should erase License Key, but Mcode STILL EXISTS.
    // 2. Should not contact the activation server on startup
    //
    public void testStopResponseErasesKeyButMcodeStaysNoAutoStart() throws Exception {
        String KEY = "L4RXLP28XVQ5";
        String MCODE = "cvnb";
        ActivationSettings.ACTIVATION_KEY.set(KEY);
        ActivationSettings.M_CODE.set(MCODE);
        final String json = "{\"response\":\"stop\",\"lid\":" +
                             "\"HT5YXS7CWGRG\",\"guid\":\"44444444444444444444444444444444\"," +
                             "\"refresh\":0,\"mcode\":\"cvnb\",\"duration\":\"0.001739\"}";
        
        final AtomicInteger retriesCount = new AtomicInteger(0);
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                retriesCount.incrementAndGet();
                return responseFactory.createFromJson(json);
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.activateKey(KEY);
        assertFalse(waitForSuccessfulActivation(activationManager, 5));
        assertEquals(activationManager.getActivationState(), ActivationState.NOT_AUTHORIZED);
        assertEquals(activationManager.getActivationError(), ActivationError.INVALID_KEY);
        assertEquals(activationManager.getActivationItems(), Collections.<ActivationItem>emptyList());
        assertEquals("", ActivationSettings.ACTIVATION_KEY.get());
        assertEquals(MCODE, ActivationSettings.M_CODE.get());
        
        // after "stop" is received and processed, calling start() on activation manager
        // should not result in contacting the activation server
        assertEquals(1, retriesCount.get());
        activationManager = getActivationManager(comm);
        activationManager.start();
        assertFalse(waitForSuccessfulActivation(activationManager, 5));
        assertEquals(1, retriesCount.get());
    }
    
    
    private boolean waitForSuccessfulActivation(final ActivationManagerImpl activationManager,
                                             int delay) throws Exception {

       
        final CountDownLatch latch = new CountDownLatch(1);        
        activationManager.addListener(new EventListener<ActivationEvent>() {
            @Override
            public void handleEvent(ActivationEvent event) {
                ActivationState state = event.getData();
                if (isSuccessfulState(state)) {
                    latch.countDown();        
                }
            }
        });
        // check if activation is done, before waiting for a state change.
        ActivationState state = activationManager.getActivationState();
        if (isSuccessfulState(state)) {
            return true;
        }
        return latch.await(delay, TimeUnit.SECONDS);        
    }
     
    
    private ActivationCommunicator getCommunicatorByJsonResponse(final String json) {
        return new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                return responseFactory.createFromJson(json);        
            }
        };    
    }
    
    /**
     * @return an {@link ActivationManager} object with a
     * stub/mocked {@link ActivationCommunicator}
     */
    private ActivationManagerImpl getActivationManager(ActivationCommunicator comm) {
        ActivationModel model = injector.getInstance(ActivationModel.class);
        ActivationSerializer serializer = injector.getInstance(ActivationSerializer.class);
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        ActivationResponseFactory factory = injector.getInstance(ActivationResponseFactory.class);
        return new ActivationManagerImpl(scheduler, comm, model, serializer, factory);
    }
        
    private boolean isSuccessfulState(ActivationState state) {
        return (state == ActivationState.AUTHORIZED);
    }

}
