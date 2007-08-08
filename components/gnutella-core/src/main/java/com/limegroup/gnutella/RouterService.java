package com.limegroup.gnutella;


import java.io.File;
import java.io.FileFilter;
import java.net.Socket;

import org.limewire.inspection.InspectablePrimitive;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.LimeWireUtils;


// DPINJ: Going away...
public class RouterService {
    
    /////-------------------------
    // DPINJ: TODOS!!!! ----------
    //  UDPService was always init'd (which internally scheduled) immediately when RS was touched.
    //     It now is constructed lazily when needed.
    //     Check if this is a problem, and if so, fix it.
     
    
    /**
	 * Whether or not we are running at full power.
	 */
    @InspectablePrimitive
	private static boolean _fullPower = true;
    
    private static final byte [] MYGUID, MYBTGUID;
	static {
	    byte [] myguid=null;
	    try {
	        myguid = GUID.fromHexString(ApplicationSettings.CLIENT_ID.getValue());
	    }catch(IllegalArgumentException iae) {
	        myguid = GUID.makeGuid();
	        ApplicationSettings.CLIENT_ID.setValue((new GUID(myguid)).toHexString());
	    }
	    MYGUID=myguid;
	    
	    byte []mybtguid = new byte[20];
	    mybtguid[0] = 0x2D; // - 
	    mybtguid[1] = 0x4C; // L
	    mybtguid[2] = 0x57; // W
	    System.arraycopy(LimeWireUtils.BT_REVISION.getBytes(),0, mybtguid,3, 4);
        mybtguid[7] = 0x2D; // -
	    System.arraycopy(MYGUID,0,mybtguid,8,12);
	    MYBTGUID = mybtguid;
	}
    
    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    public static boolean hasActiveUploads() {
        ProviderHacks.getUploadSlotManager().measureBandwidth();
        try {
            return ProviderHacks.getUploadSlotManager().getMeasuredBandwidth() > 0;
        } catch (InsufficientDataException ide) {
        }
        return false;
    }

    /**
     * @return the bandwidth for uploads in bytes per second
     */
    public static float getRequestedUploadSpeed() {
        // if the user chose not to limit his uploads
        // by setting the upload speed to unlimited
        // set the upload speed to 3.4E38 bytes per second.
        // This is de facto not limiting the uploads
        int uSpeed = UploadSettings.UPLOAD_SPEED.getValue();
        if (uSpeed == 100) {
            return Float.MAX_VALUE; 
        } else {
            // if the uploads are limited, take messageUpstream
            // for ultrapeers into account, - don't allow lower 
            // speeds than 1kb/s so uploads won't stall completely
            // if the user accidently sets his connection speed 
            // lower than his message upstream

            // connection speed is in kbits per second and upload speed is in percent
            float speed = ConnectionSettings.CONNECTION_SPEED.getValue() / 8f * uSpeed / 100f;
            
            // reduced upload speed if we are an ultrapeer
            speed -= ProviderHacks.getConnectionManager().getMeasuredUpstreamBandwidth();
            
            // we need bytes per second
            return Math.max(speed, 1f) * 1024f;
        }
    }

    /**
     * Sets full power mode.
     */
    public static void setFullPower(boolean newValue) {
        if(_fullPower != newValue) {
            _fullPower = newValue;
            // FIXME implement throttle switching for uploads and downloads
            // NormalUploadState.setThrottleSwitching(!newValue);
            // HTTPDownloader.setThrottleSwitching(!newValue);
        }
    }

    /**
	 * Push uploads from firewalled clients.
	 */
	public static void acceptUpload(Socket socket, HTTPConnectionData data) {
	    ProviderHacks.getHTTPUploadAcceptor().acceptConnection(socket, data);
	}

	public static byte [] getMyGUID() {
	    return MYGUID;
	}
	
	public static byte [] getMyBTGUID() {
		return MYBTGUID;
	}
    
    /**
     * Returns the number of uploads in progress.
     */
    public static int getNumUploads() {
        return ProviderHacks.getUploadManager().uploadsInProgress() + ProviderHacks.getTorrentManager().getNumActiveTorrents();
    }

    /**
     * Returns the number of queued uploads.
     */
    public static int getNumQueuedUploads() {
        return ProviderHacks.getUploadManager().getNumQueuedUploads();
    }
    
    /**
     * Deletes all preview files.
     */
    static void cleanupPreviewFiles() {
        //Cleanup any preview files.  Note that these will not be deleted if
        //your previewer is still open.
        File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
        if (incompleteDir == null)
            return; // if we could not get the incomplete directory, simply return.
        
        
        File[] files = incompleteDir.listFiles();
        if(files == null)
            return;
        
        for (int i=0; i<files.length; i++) {
            String name = files[i].getName();
            if (name.startsWith(IncompleteFileManager.PREVIEW_PREFIX))
                files[i].delete();  //May or may not work; ignore return code.
        }
    }
    
    static void cleanupTorrentMetadataFiles() {
        if(!ProviderHacks.getFileManager().isLoadFinished()) {
            return;
        }
        
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                return FileUtils.getFileExtension(f).equals("torrent");
            }
        };
        
        File[] file_list = FileManager.APPLICATION_SPECIAL_SHARE.listFiles(filter);
        if(file_list == null) {
            return;
        }
        long purgeLimit = System.currentTimeMillis() 
            - SharingSettings.TORRENT_METADATA_PURGE_TIME.getValue()*24L*60L*60L*1000L;
        File tFile;
        for(int i = 0; i < file_list.length; i++) {
            tFile = file_list[i];
            if(!ProviderHacks.getFileManager().isFileShared(tFile) &&
                    tFile.lastModified() < purgeLimit) {
                tFile.delete();
            }
        }
    }
    
    /**
     * Reloads the IP Filter data & adjusts spam filters when ready.
     */
    public static void reloadIPFilter() {
        ProviderHacks.getIpFilter().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                adjustSpamFilters();
            }
        });
        ProviderHacks.getHostileFilter().refreshHosts();
    }

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public static void adjustSpamFilters() {
        UDPReplyHandler.setPersonalFilter(SpamFilter.newPersonalFilter());
        
        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for(ManagedConnection c : ProviderHacks.getConnectionManager().getConnections()) {
            if(ProviderHacks.getIpFilter().allow(c)) {
                c.setPersonalFilter(SpamFilter.newPersonalFilter());
                c.setRouteFilter(SpamFilter.newRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }
        
        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }

    /**
     * Returns the number of files being shared locally.
     */
    public static int getNumSharedFiles( ) {
        return( ProviderHacks.getFileManager().getNumFiles() );
    }
    
    /**
     * Returns the number of files which are awaiting sharing.
     */
    public static int getNumPendingShared() {
        return( ProviderHacks.getFileManager().getNumPendingFiles() );
    }

	
    
    /**
	 * Creates and returns a new chat to the given host and port.
     * 
     * <p>{@link Chatter#start()} needs to be invoked to initiate the connection. 
	 */
	public static Chatter createChat(String host, int port) {
		Chatter chatter = ProviderHacks.getChatManager().request(host, port);
		return chatter;
	}
}
