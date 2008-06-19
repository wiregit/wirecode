package org.limewire.swarm.http.gnutella.interceptor;

import java.util.List;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.http.MalformedHeaderException;
import org.limewire.swarm.http.SwarmExecutionContext;
import org.limewire.util.BaseTestCase;

public class AvailableRangesInterceptorTest extends BaseTestCase {
    

    public AvailableRangesInterceptorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AvailableRangesInterceptorTest.class);
    }
    
    public void testAvailableRanges() throws Exception {
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("X-Available-Ranges", "bytes 1-5, 15-20,8-10  , 30-40, 40-50, 55-54, 60-78"));
        
        HttpContext context = new BasicHttpContext();
        assertNull(context.getAttribute(SwarmExecutionContext.HTTP_AVAILABLE_RANGES));
        new AvailableRangesInterceptor().process(response, context);
        IntervalSet ranges = (IntervalSet)context.getAttribute(SwarmExecutionContext.HTTP_AVAILABLE_RANGES);
        assertNotNull(ranges);
        
        List<Range> rangeList = ranges.getAllIntervalsAsList();
        assertEquals(Range.createRange(1, 5),   rangeList.get(0));
        assertEquals(Range.createRange(8, 10),  rangeList.get(1));
        assertEquals(Range.createRange(15, 20), rangeList.get(2));
        assertEquals(Range.createRange(30, 50), rangeList.get(3));
        assertEquals(Range.createRange(60, 78), rangeList.get(4));
        assertEquals(5, rangeList.size());
    }
    
    public void testInvalidRanges() throws Exception {
        HttpContext context = new BasicHttpContext();
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, null));
        response.addHeader(new BasicHeader("X-Available-Ranges", "bytes 1-"));
        
        try {
            new AvailableRangesInterceptor().process(response, context);
            fail();
        } catch(MalformedHeaderException mhe) {}
    }

}
