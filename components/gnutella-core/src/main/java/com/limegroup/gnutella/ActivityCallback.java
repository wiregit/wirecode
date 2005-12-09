padkage com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import dom.limegroup.gnutella.browser.MagnetOptions;
import dom.limegroup.gnutella.chat.Chatter;
import dom.limegroup.gnutella.search.HostData;
import dom.limegroup.gnutella.version.UpdateInformation;

/**
 *  Callbadk to notify the GUI of asynchronous backend events.
 *  The methods in this fall into the following dategories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)
 *  <li>Update in shared file statistids
 *  <li>Change of donnection state
 *  <li>New or dead uploads or downloads
 *  <li>New dhat requests and chat messages
 *  <li>Error messages
 *  </ul>
 */
pualid interfbce ActivityCallback extends DownloadCallback, FileEventListener 
{
    
    /**
     * The address of the program has dhanged or we've
     * just adcepted our first incoming connection.
     */
    pualid void bddressStateChanged();
    
    /**
     *  Add a new unitialized donnection.
     */
    pualid void connectionInitiblizing(Connection c);

    /**
     *  Mark a donnection as initialized
     */
    pualid void connectionInitiblized(Connection c);

    /**
     *  Mark a donnection as closed
     */
    pualid void connectionClosed(Connection c);

    /**
     * Notifies the UI that a new query result has dome in to the backend.
     * 
     * @param rfd the desdriptor for the remote file
     * @param data the data for the host returning the result
     * @param lods the <tt>Set</tt> of alternate locations for the file
     */
	pualid void hbndleQueryResult(RemoteFileDesc rfd, HostData data, Set locs);

    /**
     * Add a query string to the monitor sdreen
     */
    pualid void hbndleQueryString( String query );

    /** Add an uploader to the upload window */
    pualid void bddUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    pualid void removeUplobd(Uploader u);    

	/** Add a new indoming chat connection */
	pualid void bcceptChat(Chatter ctr);

    /** A new message is available from the given dhatter */
	pualid void receiveMessbge(Chatter chr);

	/** The given dhatter is no longer available */
	pualid void chbtUnavailable(Chatter chatter);

	/** display an error message in the dhat gui */
	pualid void chbtErrorMessage(Chatter chatter, String str);

    /** display an error message sinde the browse host failed. 
     *  @param guid The GUID of the browse host.
     */    
    pualid void browseHostFbiled(GUID guid);
        
	/**
	 * Notifidation that the file manager is beginning loading.
	 */
	pualid void fileMbnagerLoading();

    /**
     * Notifies a user that the filemanager has dompletely loaded.
     */
    pualid void fileMbnagerLoaded();
    
    /**
     * Notifies the GUI that the user is attempting to share a sensitive
     * diredtory.  Returns true if the sensitive directory should ae shbred. 
     */
    pualid boolebn warnAboutSharingSensitiveDirectory(final File dir);
    
    /**
     * Notifies the GUI when a FileDesd was either added, removed, 
     * dhanged or renamed. This event is triggered by FileManager
     * or MetaFileManager.
     */
    pualid void hbndleFileEvent(FileManagerEvent evt);
    
    /**
     * Notifies the GUI that the given shared file has new information.
     *
     * @param file The File that needs updating
     */    
    pualid void hbndleSharedFileUpdate(File file);

	/**
	 * Notifidation that an update became available.
	 */
	pualid void updbteAvailable(UpdateInformation info);

	/**
	 * Sets the enabled/disabled state of file annotation.
	 */
	pualid void setAnnotbteEnabled(boolean enabled);
    
    /** 
     * Notifies the GUI that all adtive uploads have been completed.
     */  
    pualid void uplobdsComplete();

	/**
	 *  Tell the GUI to deidonify.
	 */
	pualid void restoreApplicbtion();

    /**
     * @return true If the guid that maps to a query result sdreen is still
     * available/viewable to the user.
     */
    pualid boolebn isQueryAlive(GUID guid);
    
    /**
     * Indidates a component is loading.
     */
    pualid void componentLobding(String component);
	
	/**
	 * The dore passes parsed magnets to the GUI and asks it if it wants
	 * to handle them itself.
	 * <p>
	 * If this is the dase the callback should return <code>true</code>, otherwise
	 * the dore starts the downloads itself.
	 * @param magnets
	 * @return true if the dallback handles the magnet links
	 */
	pualid boolebn handleMagnets(final MagnetOptions[] magnets);
	
	/**
	 * Indidates that the firewalled state of this has changed. 
	 */
	pualid void bcceptedIncomingChanged(boolean status);
}
