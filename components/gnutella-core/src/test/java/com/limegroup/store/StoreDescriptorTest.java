package com.limegroup.store;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 *  Tests the store descriptor contruction and return details
 *
 */

public class StoreDescriptorTest extends LimeTestCase{

    public StoreDescriptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StoreDescriptorTest.class);
    }
    
    public void testInvalidStoreDescriptorArgs() throws MalformedURLException{
        File file = CommonUtils.getResourceFile("build.xml");
        URL url = file.toURL();
        URN urn = null;
        try {
            urn = (URN) calculateAndCacheURN(file).toArray()[0];
        } catch (Exception e) {

        }
        String fileName = "fileName.txt";
        long size = 100;
        
        try {
            StoreDescriptor store = new StoreDescriptor(null, urn, fileName, size);
            fail("URL cannot be null");
        } catch(NullPointerException e){}
        try {
            StoreDescriptor store = new StoreDescriptor(url, null, fileName, size);
            fail("URN sha hash cannot be null");
        } catch(NullPointerException e){}
        try {
            StoreDescriptor store = new StoreDescriptor(url, urn, null, size);
            fail("Filename cannot be null");
        } catch(NullPointerException e){}
        try {
            StoreDescriptor store = new StoreDescriptor(url, urn, "", size);
            fail("Cannot accept empty string file name");
        } catch(IllegalArgumentException e){}
        try {
            StoreDescriptor store = new StoreDescriptor(url, urn, fileName, -1);
            fail();
        }
        catch(IllegalArgumentException e){}
        try {
            StoreDescriptor store = new StoreDescriptor(url, urn, fileName, Constants.MAX_FILE_SIZE + 1);
            fail();
        }
        catch(IllegalArgumentException e){}
    }

    public void testStoreDescriptorSetup() throws MalformedURLException {
        File file = CommonUtils.getResourceFile("build.xml");
        URL url = file.toURL();
        URN urn = null;
        try {
            urn = (URN) calculateAndCacheURN(file).toArray()[0];
        } catch (Exception e) {

        }
        String fileName = "fileName.txt";
        long size = 100;
        
        StoreDescriptor store = new StoreDescriptor(url, urn, fileName, size);
        
        String nameReturn = store.getFileName();
        assertEquals(nameReturn, fileName);
        
        URN urnReturn = store.getSHA1Urn();
        assertEquals(urn, urnReturn);
        
        Long returnSize = store.getSize();
        assertEquals( new Long(returnSize), new Long(size));
        
        URL urlReturn = store.getURL();
        assertEquals(url, urlReturn);
    }

}
