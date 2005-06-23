package com.limegroup.gnutella;

import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.SystemUtils;

import junit.framework.Test;

public class DiskSpaceManagerTest extends com.limegroup.gnutella.util.BaseTestCase {

    public DiskSpaceManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DiskSpaceManagerTest.class);
    }    
    

    protected void setUp() throws Exception {
        super.setUp();
        
        _manager=new DiskSpaceManagerStub();
        
        // make SystemUtils loaded, since baseTestCase un-sets this
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.TRUE);
    }

    DiskSpaceManagerStub _manager;
    
    // ------ The actual tests ------ //
    public void testDownloadSpaceLimits() throws Exception {
        boolean bShould;
        
        _manager.setCurrentDiskSpace(500);  //  MB
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","c:\\")).booleanValue();
        assertTrue( "Should be able to download", bShould );
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","\\\\NetworkShareName\\SharedFolder\\")).booleanValue();
        assertTrue( "Should be able to download", bShould );
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","\\\\NetworkShareName\\SharedFolder")).booleanValue();
        assertTrue( "Should be able to download", bShould );
        
        _manager.setCurrentDiskSpace(5);    //  MB        
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","c:\\")).booleanValue();
        assertFalse( "Should not be able to download", bShould );
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","\\\\NetworkShareName\\SharedFolder\\")).booleanValue();
        assertFalse( "Should not be able to download", bShould );
        bShould=((Boolean)PrivilegedAccessor.invokeMethod(_manager,"checkShouldDownloadToDrive","\\\\NetworkShareName\\SharedFolder")).booleanValue();
        assertFalse( "Should not be able to download", bShould );
        
    }
    
    
    
    
    
    public static class DiskSpaceManagerStub extends DiskSpaceManager{
        private long _currentDiskSpace=0;
        
        
        protected long getFreeDiskSpaceMB( String drive ) throws IllegalArgumentException {
            if(!drive.endsWith("\\") )
                throw new IllegalArgumentException("Invalid target for getFreeDiskSpace: "+drive);
            if(!SystemUtils.supportsFreeSpace() )
                return -1;
                
            return _currentDiskSpace;
        }


        public long getCurrentDiskSpace() {
            return _currentDiskSpace;
        }

        public void setCurrentDiskSpace(long diskSpace) {
            _currentDiskSpace = diskSpace;
        }
        
        
    }
}
