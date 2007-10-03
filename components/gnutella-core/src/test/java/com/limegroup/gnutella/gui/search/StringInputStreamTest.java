package com.limegroup.gnutella.gui.search;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.gui.mp3.PlayListItemTest;

import junit.framework.Test;
import junit.framework.TestCase;

public class StringInputStreamTest extends BaseTestCase {
    
    public StringInputStreamTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(StringInputStreamTest.class);
    }
    
    public void testEmpty() {
        runTest("");
    }
    
    public void testShort() {
        runTest("1");
    }
    
    public void testBigger() {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<10000; i++) sb.append((char)((int)Math.random()*255));
        runTest(sb.toString());
    }
    
    protected final void runTest(String s) {
        try {
            runTestInternal(s);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    void runTestInternal(String s) throws IOException {
        final BasicSpecialResultsDatabaseImpl.StringInputStream in = new BasicSpecialResultsDatabaseImpl.StringInputStream(s);
        int c;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((c = in.read()) != -1) out.write(0xff & c);
        final String read = new String(out.toByteArray());
        assertEquals(s, read);

    }
}
