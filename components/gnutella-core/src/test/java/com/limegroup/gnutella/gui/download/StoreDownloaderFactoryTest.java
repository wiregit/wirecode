package com.limegroup.gnutella.gui.download;

import java.io.File;

import junit.framework.Test;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.StoreDownloader;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.LimeTestCase;


public class StoreDownloaderFactoryTest extends LimeTestCase{

    public StoreDownloaderFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StoreDownloaderFactoryTest.class);
    }

    
    public void testInvalidStoreDownloaderFactoryArgs(){
        try {
            new StoreDownloaderFactory(null, null);
            fail("StoreDescriptor cannot be null");
        }catch(NullPointerException e){};
    }
    
    public void testStoreDownloaderFactorySetup() throws Exception{
        
        File file = CommonUtils.getResourceFile("build.xml");

        URN urn = URN.createSHA1Urn(file);
        

        String fileName = "fileName.txt";
        long size = 100;
        
        // create rfd with the same filename as passed into the factory
        RemoteFileDesc rfd = StoreDownloader.createRemoteFileDesc(file.toURL(), fileName, urn, size);
        
        StoreDownloaderFactory store = new StoreDownloaderFactory(rfd, fileName);
        
        // test the file size
        long sizeReturn = store.getFileSize();
        assertEquals( size, sizeReturn);
        
        // test the urn
        URN urnReturn = store.getURN();
        assertEquals(urnReturn, urn);
        
        // test the filename
        File fileReturn = store.getSaveFile();
        fileReturn.deleteOnExit();
        assertEquals(new File(SharingSettings.getSaveLWSDirectory(), fileName), fileReturn);
        
        
        // try changing the file name
        File newFile = new File(SharingSettings.getSaveLWSDirectory(),"test.txt");
        store.setSaveFile(newFile);
        fileReturn = store.getSaveFile();
        assertEquals(newFile, fileReturn);
        
        // test changing to null file name
        store.setSaveFile(null);
        assertEquals(newFile, store.getSaveFile());

        
        // create a factory with no filename, should default back to rfd filename
        store = new StoreDownloaderFactory(rfd, null);
        
        // test the filename
        fileReturn = store.getSaveFile();      
        assertEquals(new File(SharingSettings.getSaveLWSDirectory(), fileName), fileReturn);
        
    }
}
