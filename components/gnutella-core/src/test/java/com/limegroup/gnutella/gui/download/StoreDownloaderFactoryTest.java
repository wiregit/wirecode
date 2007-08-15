package com.limegroup.gnutella.gui.download;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.store.StoreDescriptor;


public class StoreDownloaderFactoryTest extends LimeTestCase{

    public StoreDownloaderFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StoreDownloaderFactoryTest.class);
    }

    
    public void testInvalidStoreDownloaderFactoryArgs(){
        try {
            StoreDownloaderFactory store = new StoreDownloaderFactory(null);
            fail("StoreDescriptor cannot be null");
        }catch(NullPointerException e){};
    }
    
    public void testStoreDownloaderFactorySetup() throws MalformedURLException{
        
        File file = CommonUtils.getResourceFile("build.xml");
        URL url = file.toURL();
        URN urn = null;
        try {
            urn = (URN) calculateAndCacheURN(file).toArray()[0];
        } catch (Exception e) {

        }
        String fileName = "fileName.txt";
        long size = 100;
        
        StoreDescriptor descriptor = new StoreDescriptor(url, urn, fileName, size);
        
        StoreDownloaderFactory store = new StoreDownloaderFactory(descriptor);
        
        long sizeReturn = store.getFileSize();
        assertEquals( new Long(size), new Long(sizeReturn));
        
        URN urnReturn = store.getURN();
        assertEquals(urnReturn, urn);
        
        File fileReturn = store.getSaveFile();
        assertEquals(new File(SharingSettings.getSaveLWSDirectory(), fileName), fileReturn);
        
        File newFile = new File(SharingSettings.getSaveLWSDirectory(),"test.txt");
        store.setSaveFile(newFile);
        fileReturn = store.getSaveFile();
        
        assertEquals(newFile, fileReturn);
    }
}
