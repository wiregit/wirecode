package com.limegroup.gnutella;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.SimpleTimer;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.service.ErrorService;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;


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
     *  Returns the number of initialized messaging connections.
     */
    public static int getNumInitializedConnections() {
    	return ProviderHacks.getConnectionManager().getNumInitializedConnections();
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
     * Schedules the given task for repeated fixed-delay execution on this's
     * backend thread.  <b>The task must not block for too long</b>, as 
     * a single thread is shared among all the backend.
     *
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     * @exception IllegalStateException this is cancelled
     * @exception IllegalArgumentException delay or period negative
     * @see org.limewire.concurrent.SimpleTimer#schedule(java.lang.Runnable,long,long)
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, long period) {
        return SimpleTimer.sharedTimer().scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
    }
    
    /**
     * @return an object that can be used as a <tt>getScheduledExecutorService</tt>
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
    	return SimpleTimer.sharedTimer();
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port.
     * Blocks until the connection established.  Throws IOException if
     * the connection failed.
     * @return a connection to the request host
     * @exception IOException the connection failed
     */
    public static ManagedConnection connectToHostBlocking(String hostname, int portnum, ConnectType type)
		throws IOException {
        return ProviderHacks.getConnectionManager().createConnectionBlocking(hostname, portnum, type);
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port. 
     * Returns immediately without blocking.  If hostname would connect
     * us to ourselves, returns immediately.
     */
    public static void connectToHostAsynchronously(String hostname, int portnum, ConnectType type) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.0.0.1" since
        //they are aliases for this machine.
		
        byte[] cIP = null;
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
            cIP = addr.getAddress();
        } catch(UnknownHostException e) {
            return;
        }
        if ((cIP[0] == 127) && (portnum==ProviderHacks.getAcceptor().getPort(true)) &&
			ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
			return;
        } else {
            byte[] managerIP=ProviderHacks.getAcceptor().getAddress(true);
            if (Arrays.equals(cIP, managerIP)
                && portnum==ProviderHacks.getAcceptor().getPort(true))
                return;
        }

        if (!ProviderHacks.getAcceptor().isBannedIP(cIP)) {
            ProviderHacks.getConnectionManager().createConnectionAsynchronously(hostname, portnum, type);
		}
    }
    
    /**
     * Determines if you're connected to the given host.
     */
    public static boolean isConnectedTo(InetAddress addr) {
        // ideally we would check download sockets too, but
        // because of the way ManagedDownloader is built, it isn't
        // too practical.
        // TODO: rewrite ManagedDownloader
        
        String host = addr.getHostAddress();
        return ProviderHacks.getConnectionManager().isConnectedTo(host) ||
               ProviderHacks.getUDPMultiplexor().isConnectedTo(addr) ||
               ProviderHacks.getUploadManager().isConnectedTo(addr); // ||
               // dloadManager.isConnectedTo(addr);
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public static void connect() {
        adjustSpamFilters();
        
        //delegate to connection manager
        ProviderHacks.getConnectionManager().connect();
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public static void disconnect() {
		// Delegate to connection manager
        ProviderHacks.getConnectionManager().disconnect(false);
    }

    /**
     * Closes and removes the given connection.
     */
    public static void removeConnection(ManagedConnection c) {
        ProviderHacks.getConnectionManager().remove(c);
    }

    /**
     * Clears the hostcatcher.
     */
    public static void clearHostCatcher() {
        ProviderHacks.getHostCatcher().clear();
    }

    /**
     * Returns the number of downloads in progress.
     */
    public static int getNumDownloads() {
        return ProviderHacks.getDownloadManager().downloadsInProgress();
    }
    
    /**
     * Returns the number of active downloads.
     */
    public static int getNumActiveDownloads() {
        return ProviderHacks.getDownloadManager().getNumActiveDownloads();
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
     * Count up all the messages on active connections
     */
    public static int getActiveConnectionMessages() {
		return ProviderHacks.getConnectionManager().getActiveConnectionMessages();
    }

    /**
     * Count how many connections have already received N messages
     */
    public static int countConnectionsWithNMessages(int messageThreshold) {
		return ProviderHacks.getConnectionManager().countConnectionsWithNMessages(messageThreshold);
    }
    
    /** 
     * Returns a new GUID for passing to query.
     * This method is the central point of decision making for sending out OOB 
     * queries.
     */
    public static byte[] newQueryGUID() {
        byte []ret;
        // DPINJ: Use passed in NetworkManager!!!
        if (ProviderHacks.getNetworkManager().isOOBCapable() && OutOfBandThroughputStat.isOOBEffectiveForMe())
            ret = GUID.makeAddressEncodedGuid(ProviderHacks.getNetworkManager().getAddress(), ProviderHacks.getNetworkManager().getPort());
        else
            ret = GUID.makeGuid();
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(ret);
        return ret;
    }

    /**
     * Searches the network for files of the given type with the given
     * GUID, query string and minimum speed.  If type is null, any file type
     * is acceptable.<p>
     *
     * ActivityCallback is notified asynchronously of responses.  These
     * responses can be matched with requests by looking at their GUIDs.  (You
     * may want to wrap the bytes with a GUID object for simplicity.)  An
     * earlier version of this method returned the reply GUID instead of taking
     * it as an argument.  Unfortunately this caused a race condition where
     * replies were returned before the GUI was prepared to handle them.
     * 
     * @param guid the guid to use for the query.  MUST be a 16-byte
     *  value as returned by newQueryGUID.
     * @param query the query string to use
     * @param minSpeed the minimum desired result speed
     * @param type the desired type of result (e.g., audio, video), or
     *  null if you don't care 
     */
    public static void query(byte[] guid, String query, MediaType type) {
		query(guid, query, "", type);
	}

    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, MediaType)
     */
    public static void query(byte[] guid, String query) {
        query(guid, query, null);
    }

	/**
	 * Searches the network for files with the given metadata.
	 * 
	 * @param richQuery metadata query to insert between the nulls,
	 *  typically in XML format
	 * @see query(byte[], String, MediaType)
	 */
	public static void query(final byte[] guid, 
							 final String query, 
							 final String richQuery, 
							 final MediaType type) {

		try {
            QueryRequest qr = null;
            // DPINJ: Use a passed in networkManager!!!
            if (ProviderHacks.getNetworkManager().isIpPortValid() && (new GUID(guid)).addressesMatch(ProviderHacks.getNetworkManager().getAddress(), 
                    ProviderHacks.getNetworkManager().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = ProviderHacks.getQueryRequestFactory().createOutOfBandQuery(guid, query, richQuery,
                                                       type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = ProviderHacks.getQueryRequestFactory().createQuery(guid, query, richQuery, type);
            recordAndSendQuery(qr, type);
		} catch(Throwable t) {
			ErrorService.error(t);
		}
	}


	/**
	 * Sends a 'What Is New' query on the network.
	 */
	public static void queryWhatIsNew(final byte[] guid, final MediaType type) {
		try {
            QueryRequest qr = null;
            if (GUID.addressesMatch(guid, ProviderHacks.getNetworkManager().getAddress(), ProviderHacks.getNetworkManager().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = ProviderHacks.getQueryRequestFactory().createWhatIsNewOOBQuery(guid, (byte)2, type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = ProviderHacks.getQueryRequestFactory().createWhatIsNewQuery(guid, (byte)2, type);

            if(FilterSettings.FILTER_WHATS_NEW_ADULT.getValue())
                ProviderHacks.getMutableGUIDFilter().addGUID(guid);
    
            recordAndSendQuery(qr, type);
		} catch(Throwable t) {
			ErrorService.error(t);
		}
	}

    /** Just aggregates some common code in query() and queryWhatIsNew().
     */ 
    private static void recordAndSendQuery(final QueryRequest qr, 
                                           final MediaType type) {
        ProviderHacks.getQueryStats().recordQuery();
        ProviderHacks.getResponseVerifier().record(qr, type);
        ProviderHacks.getSearchResultHandler().addQuery(qr); // so we can leaf guide....
        ProviderHacks.getMessageRouter().sendDynamicQuery(qr);
    }

	/**
	 * Accessor for the last time a query was originated from this host.
	 *
	 * @return a <tt>long</tt> representing the number of milliseconds since
	 *  January 1, 1970, that the last query originated from this host
	 */
	public static long getLastQueryTime() {
		return ProviderHacks.getQueryStats().getLastQueryTime();
	}

    /** Purges the query from the QueryUnicaster (GUESS) and the ResultHandler
     *  (which maintains query stats for the purpose of leaf guidance).
     *  @param guid The GUID of the query you want to get rid of....
     */
    public static void stopQuery(GUID guid) {
        ProviderHacks.getQueryUnicaster().purgeQuery(guid);
        ProviderHacks.getSearchResultHandler().removeQuery(guid);
        ProviderHacks.getMessageRouter().queryKilled(guid);
        if(RouterService.isSupernode())
            ProviderHacks.getQueryDispatcher().addToRemove(guid);
        ProviderHacks.getMutableGUIDFilter().removeGUID(guid.bytes());
    }

    /** 
     * Returns true if the given response is of the same type as the the query
     * with the given guid.  Returns 100 if guid is not recognized.
     *
     * @param guid the value returned by query(..).  MUST be 16 bytes long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#matchesType(byte[], Response) 
     */
    public static boolean matchesType(byte[] guid, Response response) {
        return ProviderHacks.getResponseVerifier().matchesType(guid, response);
    }

    public static boolean matchesQuery(byte [] guid, Response response) {
        return ProviderHacks.getResponseVerifier().matchesQuery(guid, response);
    }
    /** 
     * Returns true if the given response for the query with the given guid is a
     * result of the Madragore worm (8KB files of form "x.exe").  Returns false
     * if guid is not recognized.  <i>Ideally this would be done by the normal
     * filtering mechanism, but it is not powerful enough without the query
     * string.</i>
     *
     * @param guid the value returned by query(..).  MUST be 16 byts long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#isMandragoreWorm(byte[], Response) 
     */
    public static boolean isMandragoreWorm(byte[] guid, Response response) {
        return ProviderHacks.getResponseVerifier().isMandragoreWorm(guid, response);
    }
    
    /**
     * Returns a collection of IpPorts, preferencing hosts with open slots.
     * If isUltrapeer is true, this preferences hosts with open ultrapeer slots,
     * otherwise it preferences hosts with open leaf slots.
     *
     * Preferences via locale, also.
     * 
     * @param num How many endpoints to try to get
     */
    public static Collection<IpPort> getPreferencedHosts(boolean isUltrapeer, String locale, int num) {
        
        Set<IpPort> hosts = new IpPortSet();
        
        if(isUltrapeer)
            hosts.addAll(ProviderHacks.getHostCatcher().getUltrapeersWithFreeUltrapeerSlots(locale,num));
        else
            hosts.addAll(ProviderHacks.getHostCatcher().getUltrapeersWithFreeLeafSlots(locale,num));
        
        // If we don't have enough hosts, add more.
        
        if(hosts.size() < num) {
            //we first try to get the connections that match the locale.
            for(IpPort ipp : ProviderHacks.getConnectionManager().getInitializedConnectionsMatchLocale(locale)) {
                if(hosts.size() >= num)
                    break;
                hosts.add(ipp);
            }
            
            //if we still don't have enough hosts, get them from the list
            //of all initialized connection
            if(hosts.size() < num) {
                for(IpPort ipp : ProviderHacks.getConnectionManager().getInitializedConnections()) {
                    if(hosts.size() >= num)
                        break;
                    hosts.add(ipp);
                }
            }
        }
        
        return hosts;
    }

    /**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isFullyConnected() {
		return ProviderHacks.getConnectionManager().isFullyConnected();
	}    

	/**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isConnected() {
		return ProviderHacks.getConnectionManager().isConnected();
	}
	
	/**
	 * Returns whether or not this client is attempting to connect.
	 */
	public static boolean isConnecting() {
	    return ProviderHacks.getConnectionManager().isConnecting();
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
     * Tries to "smart download" <b>any</b> [sic] of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * SaveLocationException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, SaveLocationException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.  
     *
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID guid of the query that returned the results (i.e. files)
	 * @param overwrite true iff the download should proceedewithout
     *  checking if it's on disk
	 * @param saveDir can be null, then the save directory from the settings
	 * is used
	 * @param fileName can be null, then one of the filenames of the 
	 * <code>files</code> array is used
	 * array is used
     * @return the download object you can use to start and resume the download
     * @throws SaveLocationException if there is an error when setting the final
     * file location of the download 
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
	public static Downloader download(RemoteFileDesc[] files, 
	                                  List<? extends RemoteFileDesc> alts, GUID queryGUID,
                                      boolean overwrite, File saveDir,
									  String fileName)
		throws SaveLocationException {
		return ProviderHacks.getDownloadManager().download(files, alts, queryGUID, overwrite, saveDir,
								   fileName);
	}
	
	public static Downloader download(RemoteFileDesc[] files, 
									  List<? extends RemoteFileDesc> alts,
									  GUID queryGUID,
									  boolean overwrite)
		throws SaveLocationException {
		return download(files, alts, queryGUID, overwrite, null, null);
	}	
	
	/**
	 * Stub for calling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
	 * @throws SaveLocationException 
	 */
	public static Downloader download(RemoteFileDesc[] files,
                                      GUID queryGUID, 
                                      boolean overwrite, File saveDir, String fileName)
		throws SaveLocationException {
		return download(files, RemoteFileDesc.EMPTY_LIST, queryGUID,
				overwrite, saveDir, fileName);
	}
	
	public static Downloader download(RemoteFileDesc[] files,
									  boolean overwrite, GUID queryGUID) 
		throws SaveLocationException {
		return download(files, queryGUID, overwrite, null, null);
	}	
        
	/**
	 * Creates a downloader for a magnet.
	 * @param magnetprovides the information of the  file to download, must be
	 *  valid
	 * @param overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @return
	 * @throws SaveLocationException
	 * @throws IllegalArgumentException if the magnet is not 
	 * {@link MagnetOptions#isDownloadable() valid}.
	 */
	public static Downloader download(MagnetOptions magnet, boolean overwrite) 
		throws SaveLocationException {
		if (!magnet.isDownloadable()) {
			throw new IllegalArgumentException("invalid magnet: not have enough information for downloading");
		}
		return ProviderHacks.getDownloadManager().download(magnet, overwrite, null, magnet.getDisplayName());
	}

	/**
	 * Creates a downloader for a magnet using the given additional options.
	 *
	 * @param magnet provides the information of the  file to download, must be
	 *  valid
	 * @param overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @param saveDir can be null, then the save directory from the settings
	 * is used
	 * @param fileName the final filename of the download, can be
	 * <code>null</code>
	 * @return
	 * @throws SaveLocationException
	 * @throws IllegalArgumentException if the magnet is not
	 * {@link MagnetOptions#isDownloadable() downloadable}.
	 */
	public static Downloader download(MagnetOptions magnet, boolean overwrite,
			File saveDir, String fileName) throws SaveLocationException {
		return ProviderHacks.getDownloadManager().download(magnet, overwrite, saveDir, fileName);
	}

   /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SaveLocationException 
     */ 
    public static Downloader download(File incompleteFile)
            throws CantResumeException, SaveLocationException {
        return ProviderHacks.getDownloadManager().download(incompleteFile);
    }

    
    /**
	 * Starts a torrent download for a given Inputstream to the .torrent file
	 * 
	 * @param is
	 *            the InputStream belonging to the .torrent file
	 * @throws IOException
	 *             in case there was a problem reading the file 
	 */
	public static Downloader downloadTorrent(BTMetaInfo info, boolean overwrite)
			throws SaveLocationException {
		return ProviderHacks.getDownloadManager().downloadTorrent(info, overwrite);
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
    
    /**
	 * Browses the passed host
     * @param host The host to browse
     * @param port The port at which to browse
     * @param guid The guid to be used for the query replies received 
     * while browsing host
     * @param serventID The guid of the client to browse from.  I need this in
     * case I need to push....
     * @param proxies the list of PushProxies we can use - may be null.
     * @param canDoFWTransfer true if the remote host supports fw transfer
	 */
	public static BrowseHostHandler doAsynchronousBrowseHost(
	  final Connectable host, GUID guid, GUID serventID, 
	  final Set<? extends IpPort> proxies, final boolean canDoFWTransfer) {
        final BrowseHostHandler handler = new BrowseHostHandler(ProviderHacks.getActivityCallback(), 
                                                          guid, serventID);
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                handler.browseHost(host, proxies, canDoFWTransfer);
            }
        }, "BrowseHoster" );
        
        return handler;
	}

    /**
     * Tells whether the node is a supernode or not.
     * NOTE: This will return true if this node is capable
     * of being a supernode but is not yet connected to 
     * the network as one (and is not a shielded leaf either).
     * 
     * @return true, if supernode, false otherwise
     */
    public static boolean isSupernode() {
        return ProviderHacks.getConnectionManager().isSupernode();
    }
    
    /**
     * Tells whether the node is currently connected to the network
     * as a supernode or not.
     * @return true, if active supernode, false otherwise
     */
    public static boolean isActiveSuperNode() {
        return ProviderHacks.getConnectionManager().isActiveSupernode();
    }
    
	/**
	 * Accessor for whether or not this node is a shielded leaf.
	 *
	 * @return <tt>true</tt> if this node is a shielded leaf, 
	 *  <tt>false</tt> otherwise
	 */
    public static boolean isShieldedLeaf() {
        return ProviderHacks.getConnectionManager().isShieldedLeaf();
    }
}
