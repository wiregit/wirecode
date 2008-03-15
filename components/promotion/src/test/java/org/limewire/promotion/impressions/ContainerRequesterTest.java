package org.limewire.promotion.impressions;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

public class ContainerRequesterTest extends AbstractEventQueryDataTest {

    public ContainerRequesterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContainerRequesterTest.class);
    }

    private ContainerRequester req;

    @Override
    protected void setUp() throws Exception {
        req = new TestContainerRequester();
    }
    

    // ------------------------------------------------------------------------
    // Tests    
    // ------------------------------------------------------------------------    
    
    
    public void test1Impressions() {
        runTest(2);
    }
    
    
    // ------------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------------    
    
    private void runTest(int n) {
        req.request(url(), id(), getEvents(n));
    }    

    private Set<UserQueryEvent> getEvents(int numEvents) {
        return getEvents(numEvents, 1);
    }

    private Set<UserQueryEvent> getEvents(int numEvents, int numSets) {
        Set<UserQueryEvent> eventSets = new HashSet<UserQueryEvent>();
        for (int i = 0; i < numSets; i++) {
            eventSets.add(getUserQueryEvent(numEvents));
        }
        return eventSets;
    }

    private String url() {
        return "http://jeffpalm.com/lwp/getBuckets.php";
    }

    private long id() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
