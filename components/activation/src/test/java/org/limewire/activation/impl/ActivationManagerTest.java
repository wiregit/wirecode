package org.limewire.activation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeWireCoreModule;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.core.impl.CoreGlueModule;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.setting.ActivationSettings;
import org.limewire.util.OSUtils;

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

    public void testGoodKeySuccessfulActivationWithServer() throws Exception {
        
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
        waitForActivationCompletion(activationManager, 10);
        
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
    
    public void testInvalidKeyShouldNotEvenGoToServer() throws Exception {
        ActivationSettings.ACTIVATION_KEY.set("invalid Key");
        ActivationCommunicator comm = new ActivationCommunicator() {
            @Override public ActivationResponse activate(String key) throws IOException, InvalidDataException {
                throw new RuntimeException("Should not get to activate in ActivationCommunicator!");
            }
        };
        ActivationManagerImpl activationManager = getActivationManager(comm);
        activationManager.start();
        waitForActivationCompletion(activationManager, 5);
        assertEquals(activationManager.getActivationState(), ActivationState.NOT_AUTHORIZED);
        assertEquals(activationManager.getActivationError(), ActivationError.INVALID_KEY);
        assertEquals(activationManager.getActivationItems(), Collections.<ActivationItem>emptyList());
    }
    
    private void waitForActivationCompletion(final ActivationManagerImpl activationManager,
                                             int delay) throws Exception {

       
        final CountDownLatch latch = new CountDownLatch(1);        
        activationManager.addListener(new EventListener<ActivationEvent>() {
            @Override
            public void handleEvent(ActivationEvent event) {
                ActivationState state = event.getData();
                if (isTerminalState(state)) {
                    latch.countDown();        
                }
            }
        });
        // check if activation is done, before waiting for a state change.
        ActivationState state = activationManager.getActivationState();
        if (isTerminalState(state)) {
            return;
        }
        assertTrue("Activation attempt timed out after " + delay + " seconds",
                   latch.await(delay, TimeUnit.SECONDS));
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
        
    private boolean isTerminalState(ActivationState state) {
        return (state == ActivationState.AUTHORIZED || state == ActivationState.NOT_AUTHORIZED);
    }
    
}
