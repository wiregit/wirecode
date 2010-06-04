package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GGEP;
import org.limewire.io.URN;
import org.limewire.io.UrnSet;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.messages.HUGEExtension;

public class ResponseFactoryImplTest extends LimeTestCase {

    private URN nms1Urn; 
    
    private ResponseFactoryImpl factory;

    @Override
    protected void setUp() throws Exception {
        byte[] sha1 = new byte[20];
        Arrays.fill(sha1, (byte)1);
        nms1Urn = URN.createNMS1FromBytes(sha1);
        factory = (ResponseFactoryImpl) LimeTestUtils.createInjector().getInstance(ResponseFactory.class);
    }
    
    public static Test suite() {
        return buildTestSuite(ResponseFactoryImplTest.class);
    }

    public void testUpdateUrnsLeavesUntouchedWhenNull() {
        Set<URN> urns = Collections.emptySet();
        Set<URN> result = ResponseFactoryImpl.updateUrns(urns, null);
        assertSame(urns, result);
    }

    public void testUpdateUrnsLeavesUntouchedWhenUrnSetAlready() {
        Set<URN> urns = new UrnSet();
        Set<URN> result = ResponseFactoryImpl.updateUrns(urns, UrnHelper.SHA1);
        assertSame(urns, result);
    }
    
    public void testUpdateUrnsChangesSetIfNecessary() {
        Set<URN> urns = Collections.emptySet();
        Set<URN> result = ResponseFactoryImpl.updateUrns(urns, UrnHelper.SHA1);
        assertInstanceof(UrnSet.class, result);
    }
    
    public void testExtBytesContainNMS1Urn() throws Exception {
        FileDescStub fileDesc = new FileDescStub();
        fileDesc.addUrn(nms1Urn);
        Response response = factory.createResponse(fileDesc, true);
        HUGEExtension huge = new HUGEExtension(response.getExtBytes());
        GGEP ggep = huge.getGGEP();
        assertTrue(ggep.hasValueFor("NM"));
        byte[] nms1 = ggep.getBytes("NM");
        assertEquals(nms1Urn.getBytes(), nms1);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.writeToStream(out);
        
        Response response2 = factory.createFromStream(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(response2.getUrns().contains(nms1Urn));
    }
    
    public void testCreateFromStreamReadsNMS1Urn() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteUtils.int2leb(1, out); // index
        ByteUtils.int2leb(5, out); // size
        out.write(StringUtils.toAsciiBytes("hello")); // filename
        out.write(0); // end of filename
        // huge block
        out.write(StringUtils.toAsciiBytes(UrnHelper.SHA1.toString())); // sha1
        out.write(0x1c); // huge delimiter
        // ggep inside of huge block
        out.write(0xc3); // magic ggep number  
        out.write(0x80 | 2); // flags: 0x80 = last , 2 = length of header
        out.write(StringUtils.toAsciiBytes("NM")); // ggep header
        out.write(0x40 | 20); // length of value = 20
        out.write(nms1Urn.getBytes());
        out.write(0); // end of response
        
        Response response = factory.createFromStream(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(response.getUrns().contains(nms1Urn));
    }
}
