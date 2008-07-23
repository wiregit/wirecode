package org.limewire.logging;

import org.apache.commons.logging.Log;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.logging.LogImpl;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;

public class LogImplTest extends BaseTestCase {
    
    
    private Mockery context;

    public LogImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LogImplTest.class);
    }    
   
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    public void testDebugWithArgs() {
        final Log delegate = context.mock(Log.class);
        context.checking(new Expectations() {{
            allowing(delegate).isDebugEnabled();
            will(returnValue(true));
        }});
        LogImpl log = new LogImpl(delegate);
        
        context.checking(new Expectations() {{
            one(delegate).debug("hello, world");
            one(delegate).debug("hello");
            one(delegate).debug("hello");
        }});
        
        log.debug("hello, {0}", "world");
        log.debug("hello");
        log.debug("hello", "ignored");
        
        
        context.assertIsSatisfied();
    }
    
    
}
