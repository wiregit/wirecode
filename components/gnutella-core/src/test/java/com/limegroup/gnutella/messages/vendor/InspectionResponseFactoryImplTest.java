package com.limegroup.gnutella.messages.vendor;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.Inflater;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.inspection.Inspector;
import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class InspectionResponseFactoryImplTest extends LimeTestCase {

    public InspectionResponseFactoryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InspectionResponseFactoryImplTest.class);
    }
    
    private Mockery mockery;
    private Inspector inspector;
    private Injector injector;
    
    public void setUp() throws Exception {
        mockery = new Mockery();
        inspector = mockery.mock(Inspector.class);
        Module m = new AbstractModule() {
            public void configure() {
                bind(Inspector.class).toInstance(inspector);
            }
        };
        injector = LimeTestUtils.createInjector(m);
    }
    
    public void testLoadsProperties() throws Exception {
        final File props = new File(CommonUtils.getCurrentDirectory(),"inspection.props");
        mockery.checking(new Expectations() {{
            one(inspector).load(props);
        }});
        injector.getInstance(InspectionResponseFactoryImpl.class); // loads the props
        mockery.assertIsSatisfied();
    }
    
    public void testRespondsToRequest() throws Exception {
     
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        mockery.checking(new Expectations() {{
            allowing(inspector).load(with(Matchers.any(File.class)));
            one(request).getRequestedFields();
            will(returnValue(new String[]{"asdf"}));
            one(request).requestsTimeStamp();
            will(returnValue(Boolean.TRUE));
            one(request).supportsEncoding();
            will(returnValue(Boolean.FALSE));
            allowing(request).getGUID();
            will(returnValue(new GUID().bytes()));
            one(inspector).inspect("asdf");
            will(returnValue("inspected"));
        }});
        InspectionResponseFactory factory = injector.getInstance(InspectionResponseFactoryImpl.class);
        InspectionResponse[] resp = factory.createResponses(request);
        mockery.assertIsSatisfied();
        assertEquals(1, resp.length);
        assertNotNull(resp[0]);
        byte [] payload = resp[0].getPayload();
        Inflater i = new Inflater();
        i.setInput(payload);
        i.finished();
        byte [] uncompressed = new byte[1024];
        i.inflate(uncompressed);
        Map<String,Object> o = (Map<String,Object>) Token.parse(uncompressed);
        assertTrue(o.containsKey("-1"));
        assertTrue(Arrays.equals("inspected".getBytes(),(byte[])o.get("0")));
    }
}
