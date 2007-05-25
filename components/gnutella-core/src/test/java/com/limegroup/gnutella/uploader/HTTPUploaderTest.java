package com.limegroup.gnutella.uploader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import junit.framework.Test;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocListener;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.util.LimeTestCase;

public class HTTPUploaderTest extends LimeTestCase {
    
    public HTTPUploaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HTTPUploaderTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    private void check(AlternateLocation loc, String ip, int port, boolean tls) {
        DirectAltLoc d = (DirectAltLoc)loc;
        IpPort host = d.getHost();
        assertEquals(ip, host.getAddress());
        assertEquals(port, host.getPort());
        if(tls) {
            assertInstanceof(Connectable.class, host);
            assertTrue(((Connectable)host).isTLSCapable());
        } else {
            if(host instanceof Connectable)
                assertFalse(((Connectable)host).isTLSCapable());
        }
    }
    
    public void testReadAltLocsWithTLS() throws Exception {
        String data = "X-Alt: tls=3D8,1.2.3.4:5213,5.4.3.2:1,8.3.2.1,6.3.2.1,5.2.1.3:52,5.3.2.6:18,43.41.42.42:41,41.42.42.43:42,89.98.89.98:89,98.89.98.89:98\r\n";
        InputStream in = new ByteArrayInputStream(data.getBytes());
        
        final AtomicInteger received = new AtomicInteger(0);
        AltLocListener listener = new AltLocListener() {
            public void locationAdded(AlternateLocation loc) {
                switch(received.getAndIncrement()) {
                case 0: check(loc, "1.2.3.4",     5213, false); break;
                case 1: check(loc, "5.4.3.2",     1,    false); break;
                case 2: check(loc, "8.3.2.1",     6346, true);  break;
                case 3: check(loc, "6.3.2.1",     6346, true);  break;
                case 4: check(loc, "5.2.1.3",     52,   true);  break;
                case 5: check(loc, "5.3.2.6",     18,   true);  break;
                case 6: check(loc, "43.41.42.42", 41,   false); break;
                case 7: check(loc, "41.42.42.43", 42,   true);  break;
                case 8: check(loc, "89.98.89.98", 89,   true);  break;
                case 9: check(loc, "98.89.98.89", 98,   false); break;
                default: throw new IllegalStateException("unexpected loc: " + loc + ", i: " + received);
                }
            }
        };
        
        AltLocManager.instance().addListener(HugeTestUtils.SHA1, listener);
        
        try {
            HTTPUploader uploader = new StubUploader();
            uploader.readHeader(in);
        } finally {
            AltLocManager.instance().removeListener(HugeTestUtils.SHA1, listener);
        }
        
        assertEquals(10, received.get()); // incremented one+
    }
    

    
    private static class StubUploader extends HTTPUploader {
        
        StubUploader() {
            super();
            try {
                setFileDesc(getFileDesc());
            } catch(IOException iox) {
                throw new RuntimeException(iox);
            }
        }

        @Override
        public FileDesc getFileDesc() {
            return new StubFileDesc();
        }
        
    }
    
    private static class StubFileDesc extends FileDesc {
        @Override
        public File getFile() {
            return new File("");
        }

        @Override
        public InputStream createInputStream() throws FileNotFoundException {
            return null;
        }

        @Override
        public URN getSHA1Urn() {
            return HugeTestUtils.SHA1;
        }
        
    }
    
}
