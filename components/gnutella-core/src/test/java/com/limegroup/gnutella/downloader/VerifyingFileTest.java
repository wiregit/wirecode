package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class VerifyingFileTest extends BaseTestCase {
    
    public VerifyingFileTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        return buildTestSuite(VerifyingFileTest.class);
    }
    
    static VerifyingFile vf;
    
    private static final String filename = 
        "com/limegroup/gnutella/metadata/mpg4_golem160x90first120.avi";
    private static final File completeFile = CommonUtils.getResourceFile(filename);
       
    private static HashTree hashTree;
    
    private static final String sha1 = 
        "urn:sha1:UBJSGDTCVZDSBS4K3ZDQJV5VQ3WTBCOK";
    
    private static MyDownloader downloader;
    
    public static void globalSetUp() throws Exception {
        try {
            hashTree = (HashTree) PrivilegedAccessor.invokeMethod(
                HashTree.class, "createHashTree", 
                new Object[] { new Long(completeFile.length()), new FileInputStream(completeFile),
                            sha1 },
                new Class[] { long.class, InputStream.class, URN.class }
            );
        } catch(InvocationTargetException ite) {
            throw (Exception)ite.getCause();
        }
    }
    
    public void setUp() {
        vf = new VerifyingFile((int)completeFile.length());
        downloader = new MyDownloader();
        try {
        vf.open(new File("outfile"),downloader);
        }catch(IOException e) {
            ErrorService.error(e);
        }
    }
    
    private static class MyDownloader extends ManagedDownloader {
        public boolean notified;
        public MyDownloader() {
            super(new RemoteFileDesc[0], new IncompleteFileManager(),null);
        }
        
        public HashTree getHashTree() {
            return hashTree;
        }
        
        public int getChunkSize() {
            return hashTree.getNodeSize();
        }
        
        public void promptAboutCorruptDownload() {
            notified = true;
        }
    }
}
