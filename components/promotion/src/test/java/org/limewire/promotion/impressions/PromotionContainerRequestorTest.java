package org.limewire.promotion.impressions;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.promotion.PromotionBinder;
import org.limewire.promotion.PromotionBinderCallback;

public class PromotionContainerRequestorTest extends AbstractEventQueryDataTest {

    public PromotionContainerRequestorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PromotionContainerRequestorTest.class);
    }
    

    // ------------------------------------------------------------------------
    // Tests    
    // ------------------------------------------------------------------------    
    
    
    public void testValid0() {
        runTest(0,true);
    }    
    
    public void testValid1() {
        runTest(1,true);
    }
    
    public void testValid2() {
        runTest(2,true);
    }    
    
    public void testValid3() {
        runTest(3,true);
    } 
    
    public void testValid4() {
        runTest(4,true);
    } 
    
    public void testValid5() {
        runTest(5,true);
    }  
    
    public void testInvalid0() {
        runTest(0,false);
    }    
    
    public void testInvalid1() {
        runTest(1,false);
    }
    
    public void testInvalid2() {
        runTest(2,false);
    }    
    
    public void testInvalid3() {
        runTest(3,false);
    } 
    
    public void testInvalid4() {
        runTest(4,false);
    } 
    
    public void testInvalid5() {
        runTest(5,false);
    }      
    
    
    // ------------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------------    
    
    private void runTest(int n, final boolean isValid) {
        new TestPromotionContainerRequestorImpl(isValid).request(url(), id(), getEvents(n), new PromotionBinderCallback() {

            public void process(PromotionBinder binder) {
                if (isValid) {
                    assertNotNull(binder);
                } else {
                    assertNull(binder);
                }
            }
            
        });
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

    /**
     * We don't care about this url.
     * @return
     */
    private String url() {
        return "http://doesn.matter.com";
    }

    private long id() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
