package org.limewire.http;

import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.limewire.util.BaseTestCase;

public class HttpCoreUtilsTest extends BaseTestCase {

    public HttpCoreUtilsTest(String name) {
        super(name);
    }

    public void testHasHeader() {
        BasicHttpResponse msg = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
        BasicHeader header1 = new BasicHeader("key", "value1");
        BasicHeader header2 = new BasicHeader("key", "value2");
        msg.addHeader(header1);
        msg.addHeader(header2);
        
        assertTrue(HttpCoreUtils.hasHeader(msg, "key", "value1"));
        assertTrue(HttpCoreUtils.hasHeader(msg, "key", "value2"));
        assertFalse(HttpCoreUtils.hasHeader(msg, "key", "value3"));
        assertFalse(HttpCoreUtils.hasHeader(msg, "key1", "value1"));
        assertFalse(HttpCoreUtils.hasHeader(msg, "", ""));
        
        msg.removeHeaders("key");
        assertFalse(HttpCoreUtils.hasHeader(msg, "key1", "value1"));
        assertFalse(HttpCoreUtils.hasHeader(msg, "key1", "value2"));
    }

}
