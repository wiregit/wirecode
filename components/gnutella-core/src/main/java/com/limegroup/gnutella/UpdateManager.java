package com.limegroup.gnutella;

import com.limegroup.gnutella.handshaking.*;

/**
 * Used for parsing the signed_update_file.xml and updating any values locally.
 * Has a singleton pattern.
 */
public class UpdateManager {
    
    private String latestVersion;
    private static UpdateManager INSTANCE=null;
    

    private UpdateManager() {
        //Use a class called UpdateFileParser for this.
        //TODO1: parse the xml file from disk and find out the version.
        latestVersion = "3.0.0";//temporary solution
    }
    
    public static synchronized UpdateManager instance() {
        if(INSTANCE==null)
            INSTANCE = new UpdateManager();
        return INSTANCE;
    }
    
    public synchronized String getVersion() {
        Assert.that(latestVersion!=null,"version not initilaized");
        return latestVersion;
    }
    
    /**
     * This method will be called just once from RouterService at startup
     * and it should prompt the user if there is an update message.
     */
    void postGuiInit(ActivityCallback gui) {
//          if(latestVersion > CommonUtils.getVersion())
//              gui.promptUser();
    }

    public void checkAndUpdate(Connection connection) {
        final Connection c = connection;
        Thread checker = new Thread() {
            public void run() {
                String newVersion = 
                c.getProperty(ConnectionHandshakeHeaders.X_VERSION);
                System.out.println("Sumeet:myVersion:"+
                                   latestVersion+" theirs: "+newVersion);
                if(!isGreaterVersion(newVersion))
                     return;
                System.out.println("Sumeet...getting file.");
                //TODO1:connect to host
                //TODO1:download file
                //TODO1:verify authenticity
                //TODO1:parseFile using UpdateFileParser
                synchronized(UpdateManager.this) {
                    //write the file to disk
                    //update the version number
                }
            }
        };
        checker.start();      
    }

    private boolean isGreaterVersion(String version) {
        //TODO1: figure out which is bigger and return true if
        //v1 is bigger than v2
        if(version==null)
            return false;
        return true;
        
    }
}
