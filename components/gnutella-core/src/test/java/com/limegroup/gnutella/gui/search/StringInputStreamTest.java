package com.limegroup.gnutella.gui.search;



import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.search.SearchField.SearchFieldDocument;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class StringInputStreamTest extends LimeTestCase {

    public StringInputStreamTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(StringInputStreamTest.class); 
    }
 
    public void testNull() throws IOException {
        final StringInputStream s = new StringInputStream(null);
        assertEquals(-1, s.read());
    }
    
    public void testEmpty() throws IOException {
        final StringInputStream s = new StringInputStream("");
        assertEquals(-1, s.read());
    }
    
    public void testNull2() throws IOException {
        final StringInputStream s = new StringInputStream(null);
        assertEquals(-1, s.read());
        assertEquals(-1, s.read());
    }
    
    public void testEmpty2() throws IOException {
        final StringInputStream s = new StringInputStream("");
        assertEquals(-1, s.read());
        assertEquals(-1, s.read());
    }
    
    public void testA() throws IOException {
        final StringInputStream s = new StringInputStream("a");
        assertEquals('a', s.read());
        assertEquals(-1, s.read());
    }
    
    public void testAB() throws IOException {
        final StringInputStream s = new StringInputStream("ab");
        assertEquals('a', s.read());
        assertEquals('b', s.read());
        assertEquals(-1, s.read());
    }
    
    public void testABC() throws IOException {
        final StringInputStream s = new StringInputStream("abc");
        assertEquals('a', s.read());
        assertEquals('b', s.read());
        assertEquals('c', s.read());
        assertEquals(-1, s.read());
    }
}
