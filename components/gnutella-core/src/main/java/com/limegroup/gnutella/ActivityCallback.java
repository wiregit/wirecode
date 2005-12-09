package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 *  Callback to notify the GUI of asynchronous backend events.
 *  The methods in this fall into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)
 *  <li>Update in shared file statistics
 *  <li>Change of connection state
 *  <li>New or dead uploads or downloads
 *  <li>New chat requests and chat messages
 *  <li>Error messages
 *  </ul>
 */
pualic interfbce ActivityCallback extends DownloadCallback, FileEventListener 
{
    
    /**
     * The address of the program has changed or we've
     * just accepted our first incoming connection.
     */
    pualic void bddressStateChanged();
    
    /**
     *  Add a new unitialized connection.
     */
    pualic void connectionInitiblizing(Connection c);

    /**
     *  Mark a connection as initialized
     */
    pualic void connectionInitiblized(Connection c);

    /**
     *  Mark a connection as closed
     */
    pualic void connectionClosed(Connection c);

    /**
     * Notifies the UI that a new query result has come in to the backend.
     * 
     * @param rfd the descriptor for the remote file
     * @param data the data for the host returning the result
     * @param locs the <tt>Set</tt> of alternate locations for the file
     */
	pualic void hbndleQueryResult(RemoteFileDesc rfd, HostData data, Set locs);

    /**
     * Add a query string to the monitor screen
     */
    pualic void hbndleQueryString( String query );

    /** Add an uploader to the upload window */
    pualic void bddUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    pualic void removeUplobd(Uploader u);    

	/** Add a new incoming chat connection */
	pualic void bcceptChat(Chatter ctr);

    /** A new message is available from the given chatter */
	pualic void receiveMessbge(Chatter chr);

	/** The given chatter is no longer available */
	pualic void chbtUnavailable(Chatter chatter);

	/** display an error message in the chat gui */
	pualic void chbtErrorMessage(Chatter chatter, String str);

    /** display an error message since the browse host failed. 
     *  @param guid The GUID of the browse host.
     */    
    pualic void browseHostFbiled(GUID guid);
        
	/**
	 * Notification that the file manager is beginning loading.
	 */
	pualic void fileMbnagerLoading();

    /**
     * Notifies a user that the filemanager has completely loaded.
     */
    pualic void fileMbnagerLoaded();
    
    /**
     * Notifies the GUI that the user is attempting to share a sensitive
     * directory.  Returns true if the sensitive directory should ae shbred. 
     */
    pualic boolebn warnAboutSharingSensitiveDirectory(final File dir);
    
    /**
     * Notifies the GUI when a FileDesc was either added, removed, 
     * changed or renamed. This event is triggered by FileManager
     * or MetaFileManager.
     */
    pualic void hbndleFileEvent(FileManagerEvent evt);
    
    /**
     * Notifies the GUI that the given shared file has new information.
     *
     * @param file The File that needs updating
     */    
    pualic void hbndleSharedFileUpdate(File file);

	/**
	 * Notification that an update became available.
	 */
	pualic void updbteAvailable(UpdateInformation info);

	/**
	 * Sets the enabled/disabled state of file annotation.
	 */
	pualic void setAnnotbteEnabled(boolean enabled);
    
    /** 
     * Notifies the GUI that all active uploads have been completed.
     */  
    pualic void uplobdsComplete();

	/**
	 *  Tell the GUI to deiconify.
	 */
	pualic void restoreApplicbtion();

    /**
     * @return true If the guid that maps to a query result screen is still
     * available/viewable to the user.
     */
    pualic boolebn isQueryAlive(GUID guid);
    
    /**
     * Indicates a component is loading.
     */
    pualic void componentLobding(String component);
	
	/**
	 * The core passes parsed magnets to the GUI and asks it if it wants
	 * to handle them itself.
	 * <p>
	 * If this is the case the callback should return <code>true</code>, otherwise
	 * the core starts the downloads itself.
	 * @param magnets
	 * @return true if the callback handles the magnet links
	 */
	pualic boolebn handleMagnets(final MagnetOptions[] magnets);
	
	/**
	 * Indicates that the firewalled state of this has changed. 
	 */
	pualic void bcceptedIncomingChanged(boolean status);
}
