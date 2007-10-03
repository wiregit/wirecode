package com.limegroup.gnutella.gui.search;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.gui.mp3.PlayListItemTest;


import junit.framework.Test;
import junit.framework.TestCase;

public class GZippedStringBasicSpecialResultsDatabaseImplTest extends BaseTestCase {
    
    public GZippedStringBasicSpecialResultsDatabaseImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(StringInputStreamTest.class);
    }

    public void testFromFile() throws IOException {
        runTest("test.txt.gz", "cat", 1);
    }  
    
    public void testFromFile50() throws IOException {
        runTest("test50.txt.gz", "cat", 3);
    } 
    
    private void runTest(String filename, String query, int numEntries) throws IOException {
        final String path = "tests/com/limegroup/gnutella/gui/search/" + filename;

        final GZippedStringBasicSpecialResultsDatabaseImpl db 
            = new GZippedStringBasicSpecialResultsDatabaseImpl(new GZIPInputStream(new FileInputStream(path)));
        
        List<Map<String,String>> lst = db.getSearchResults(query);
        assertEquals(numEntries, lst.size());
    }
}
