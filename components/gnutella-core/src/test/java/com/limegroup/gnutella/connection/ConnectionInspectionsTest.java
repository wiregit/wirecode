package com.limegroup.gnutella.connection;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.AbstractModule;
import org.limewire.util.Clock;
import org.limewire.inspection.Inspector;
import org.limewire.inspection.InspectionException;

import com.google.inject.Injector;

public class ConnectionInspectionsTest extends TestCase {
    
    private static final String CONN_TIME = "com.limegroup.gnutella.connection.ConnectionInspections,connectionTime";
    private static final String LOAD_TIME = "com.limegroup.gnutella.connection.ConnectionInspections,loadTime";
    
    public ConnectionInspectionsTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void testConnectionInspections() throws InspectionException {
        Mockery mockery = new Mockery();
        final Clock clock = mockery.mock(Clock.class);
        mockery.checking(new Expectations() {{
            allowing(clock).now();
            will(onConsecutiveCalls(returnValue(1000l),
                    returnValue(5000l), returnValue(7000l)));
        }});
        Injector injector = LimeTestUtils.createInjectorNonEagerly(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Clock.class).toInstance(clock);
            }
        });
        ConnectionInspections connectionReporter = injector.getInstance(ConnectionInspections.class);
        Inspector insp = injector.getInstance(Inspector.class);
        connectionReporter.setLoadTime(1500);
        assertEquals(0L, Long.parseLong((String)insp.inspect(CONN_TIME, true)));
        assertEquals(1500L, Long.parseLong((String)insp.inspect(LOAD_TIME, true)));
        
        connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), ConnectionLifecycleEventType.CONNECTING));
        assertEquals(0L, Long.parseLong((String)insp.inspect(CONN_TIME, true)));
        assertEquals(1500L, Long.parseLong((String)insp.inspect(LOAD_TIME, true)));
        
        connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), ConnectionLifecycleEventType.CONNECTION_INITIALIZED));
        assertEquals(4000L, Long.parseLong((String)insp.inspect(CONN_TIME, true)));
        assertEquals(1500L, Long.parseLong((String)insp.inspect(LOAD_TIME, true)));
        
        // mockery expectation exception if additional http requests
        // are made upon subsequent ConnectionLifecycleEvents
        for(ConnectionLifecycleEventType eventType : ConnectionLifecycleEventType.values()) {
            connectionReporter.handleConnectionLifecycleEvent(new ConnectionLifecycleEvent(new Object(), eventType));            
        }
        // make sure the load and connection time inspection points did not change
        assertEquals(4000L, Long.parseLong((String)insp.inspect(CONN_TIME, true)));
        assertEquals(1500L, Long.parseLong((String)insp.inspect(LOAD_TIME, true)));
        
        mockery.assertIsSatisfied();
    }
}
