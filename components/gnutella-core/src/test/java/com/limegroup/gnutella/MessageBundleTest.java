package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Test the message bundles to ensure uniqueness of keys.
 */
public final class MessageBundleTest extends com.limegroup.gnutella.util.BaseTestCase {
    
    public MessageBundleTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MessageBundleTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testUniqueKeys() throws Exception {
	    File f = CommonUtils.getResourceFile("MessagesBundle.properties");

        Set readKeys = new HashSet();
	    BufferedReader br = new BufferedReader(new FileReader(f));
	    boolean read = false;
	    while(true) {
	        String line = br.readLine();
	        if(line == null)
	            break;
	        read = true;   
	        line = line.trim();
	        if(line.startsWith("#"))
	            continue;
            else if(line.equals(""))
                continue;

	        int eq = line.indexOf("=");
	        if(eq == -1)
	            fail("no '=' in line: " + line + "!");
	        String key = line.substring(0, eq).trim();
	        if(readKeys.contains(key))
	            fail("Duplicate key: " + key);
	        else
	            readKeys.add(key);
	    }
	    
	    assertTrue("read nothing!", read);
	    
	    br.close();
    }
}