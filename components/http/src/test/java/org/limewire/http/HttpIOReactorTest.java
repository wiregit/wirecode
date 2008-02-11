package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class HttpIOReactorTest extends TestCase {

    public void testHttpIOReactor() throws Exception {
        try {
            new HttpIOReactor(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testExecute() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        try {
            reactor.execute(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testShutdown() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpIOReactor reactor = new HttpIOReactor(params);
        try {
            reactor.shutdown();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    // TODO: Test acceptConnection!

}
