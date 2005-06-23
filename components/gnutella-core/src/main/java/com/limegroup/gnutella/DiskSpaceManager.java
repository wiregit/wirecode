package com.limegroup.gnutella;

import com.limegroup.gnutella.util.SystemUtils;

/**
 * This class is responsible for handling low disk space conditions, and doing a callback to user reporting such.
 *
 */
public class DiskSpaceManager {
    
    
    protected long _diskSpaceLimitMB=50;  // Cheesy default
    
    
    /**
     * Internal function to determine if a drive should be downloaded to (less than limit)
     * @param path the save path to check
     * @return true iff we should download to this location currently
     */
    private boolean checkShouldDownloadToDrive( String path ) throws IllegalArgumentException {
        long free=-1;
        String drive;
                                                //  Windows Network share: "\\Share\Folder\LoremIpsum"
        if( path.startsWith("\\\\") ) {         //                          ^^
            int nextLoc=path.indexOf("\\", 2);  //                                 ^
            int lastLoc=path.indexOf("\\", nextLoc+1);//                                  ^
            
            if( nextLoc!=-1 ) {
                if( lastLoc==-1 )   
                    drive=path+"\\";            //  No trailing '\\', add one
                else
                    drive=path.substring(0,lastLoc+1);  //  Should include path's trailing '\\'
                
                Assert.that( drive.endsWith("\\") && drive.startsWith("\\\\"), "Failed parsing network drive" );
                free=getFreeDiskSpaceMB( drive );
            }
        } else if( path.substring(1,3).equals(":\\") ) {//  Windows/DOS drive format
            drive=path.substring(0,3);
            
            Assert.that( drive.endsWith("\\"), "Failed parsing local or mapped drive" );
            free=getFreeDiskSpaceMB( drive );
        }
        
        return (free>=_diskSpaceLimitMB) || (free==-1);
    }
    
    /**
     * Internal function to check free disk space - only works with 
     * 
     * @param drive the drive to check (format: "c:\\" or "\\\\networkshare\\folder\\"
     * @return -1 if there was a problem checking the drive (wrong OS) 
     * @throws IllegalArgumentException if the passed string doesn't appear to be a drive
     */
    protected long getFreeDiskSpaceMB( String drive ) throws IllegalArgumentException {
        if(!drive.endsWith("\\") )
            throw new IllegalArgumentException("Invalid target for getFreeDiskSpace: "+drive);
        if(!SystemUtils.supportsFreeSpace() )
            return -1;
            
        return SystemUtils.getDiskSpaceMB(drive);
    }
    
}
