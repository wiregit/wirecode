package com.limegroup.gnutella.filters;

import java.util.concurrent.Callable;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Unit tests for DuplicateFilter
 */
// TODO convert to BaseTestCase, get rid of LimeXMLDocument dependencies in last test case
public class DuplicateFilterTest extends BaseTestCase {

    DuplicateFilter filter;
    PingRequest pr;
    QueryRequest qr;
    private Mockery context;

    public DuplicateFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DuplicateFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        filter = new DuplicateFilter();
        filter.setLag(50);
        context = new Mockery();
        qr = context.mock(QueryRequest.class);
        pr = context.mock(PingRequest.class);
    }

    private void addDefaultReturnValues() {
        // specify default return values
        context.checking(new Expectations() {{
            allowing(pr).getHops(); will(returnValue((byte)2));
            allowing(qr).getHops(); will(returnValue((byte)2));
            allowing(qr).getQuery(); will(returnValue("blah"));
            allowing(qr).getGUID(); will(new CallableAction(new Callable<byte[]>() {
                public byte[] call() throws Exception {
                    return new GUID().bytes();
                }
            }));
        }});
    }

    public void testPingAndQueryWithSameGUIDAreRejected() throws Exception {
        final GUID guid = new GUID();
        context.checking(new Expectations() {{
            exactly(1).of(pr).getGUID();
            will(returnValue(guid.bytes()));
            exactly(2).of(qr).getGUID();
            will(returnValue(guid.bytes()));
        }});
        addDefaultReturnValues();

        assertTrue(filter.allow(pr));
        assertFalse(filter.allow(qr));

        waitForGUIDFilterToBePurged();

        assertTrue(filter.allow(qr));

        context.assertIsSatisfied();
    }

    public void testSameGUIDPingIsNotAllowedBeforeTimeout() throws Exception {
        final GUID guid = new GUID();
        context.checking(new Expectations() {{
            exactly(3).of(pr).getGUID();
            will(returnValue(guid.bytes()));
        }});
        addDefaultReturnValues();

        assertTrue(filter.allow(pr));
        assertFalse(filter.allow(pr));

        waitForGUIDFilterToBePurged();

        assertTrue(filter.allow(pr));
        context.assertIsSatisfied();
    }

    public void testSameGUIDDifferentHopCountAllowed() {
        context.checking(new Expectations() {{
            GUID guid = new GUID();
            allowing(pr).getGUID(); will(returnValue(guid.bytes()));
            one(pr).getHops(); will(returnValue((byte)2));
            one(pr).getHops(); will(returnValue((byte)3));
        }});
        addDefaultReturnValues();

        assertTrue(filter.allow(pr));
        assertTrue(filter.allow(pr));

        context.assertIsSatisfied();
    }

    // wait for guid filter to be purged
    private void waitForGUIDFilterToBePurged() throws Exception {
        synchronized (filter) {
            try {
                int lag = filter.getLag() * 2;
                assertGreaterThan(0, lag);
                filter.wait(lag);
            } catch (InterruptedException e) { }
        }
    }

    private static class CallableAction extends CustomAction {

        private Callable callable;

        public CallableAction(Callable callable) {
            super("Calls a callable");
            this.callable = callable;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            return callable.call(); 
        }
    }
}    
